import ij.ImageStack
import qupath.lib.geom.ImmutableDimension
import qupath.lib.gui.TaskRunnerFX
import qupath.lib.gui.tools.GuiTools
import qupath.lib.images.ImageData
import qupath.lib.images.servers.PixelCalibration
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathCellObject

import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathObject
import qupath.lib.objects.TMACoreObject
import qupath.lib.objects.hierarchy.PathObjectHierarchy
import qupath.lib.plugins.AbstractInteractivePlugin
import qupath.lib.plugins.AbstractTileableDetectionPlugin;
import qupath.lib.plugins.AbstractPlugin
import qupath.lib.plugins.CommandLineTaskRunner
import qupath.lib.plugins.DetectionPluginTools
import qupath.lib.plugins.ObjectDetector
import qupath.lib.plugins.ParallelTileObject
import qupath.lib.plugins.PathPlugin
import qupath.lib.plugins.TaskRunner;
import qupath.lib.plugins.parameters.ParameterList;

import ij.ImagePlus
import jj2000.j2k.roi.encoder.ROIScaler
import org.bytedeco.javacpp.indexer.Bfloat16BufferIndexer
import qupath.lib.analysis.stats.RunningStatistics
import qupath.lib.images.PathImage
import qupath.lib.images.servers.ImageChannel
import qupath.lib.measurements.MeasurementList
import qupath.lib.measurements.MeasurementListFactory
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.interfaces.ROI

import static qupath.lib.gui.scripting.QPEx.*
import qupath.imagej.processing.RoiLabeling
import ij.plugin.RoiEnlarger
import ij.process.ShortProcessor
import ij.process.FloatProcessor
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.measure.Calibration
import qupath.lib.regions.RegionRequest
import qupath.imagej.tools.IJTools
import qupath.imagej.tools.PixelImageIJ
import qupath.lib.analysis.stats.RunningStatistics
import qupath.lib.analysis.stats.StatisticsHelper
import qupath.lib.analysis.images.SimpleImage
import ij.gui.PointRoi
import org.locationtech.jts.geom.util.LinearComponentExtracter

import qupath.lib.gui.tools.MeasurementExporter

import java.lang.reflect.Field;


//largely copied from here:
//https://github.com/qupath/qupath/blob/master/qupath-core-processing/src/main/java/qupath/imagej/detect/cells/WatershedCellDetection.java
//https://github.com/qupath/qupath/blob/main/qupath-core-processing/src/main/java/qupath/lib/algorithms/IntensityFeaturesPlugin.java

public class BorderCompensation extends AbstractTileableDetectionPlugin<BufferedImage> {

	//int PREFERRED_TILE_SIZE = 128;

	@Override
	String getName() {
		return "Cell Border Compensation";
	}

	@Override
	String getDescription() {
		return "attempt to reuse QuPath's built in tiling to do cell-cell boundary compensation without reinventing the parallelization wheel";
	}

	@Override
	String getLastResultsDescription() {
		return null
	}

	static class BorderInt implements ObjectDetector<BufferedImage> {
		//prep stuff here??
		//I don't understand this, but it's in both Watershed and SLIC so I'm copying it

		private PathImage<ImagePlus> pathImage = null;
		private ROI pathROI = null;

		private String lastResultSummary = null;
		String getLastResultsDescription() {
			return null
		}


		@Override
		public Collection<PathObject> runDetection(final ImageData<BufferedImage> imageData, ParameterList params, ROI pathROI) throws IOException {
			//largely copied from here:
			//https://github.com/qupath/qupath/blob/master/qupath-core-processing/src/main/java/qupath/imagej/detect/cells/WatershedCellDetection.java#L844

			int borderPix = 4
			//imageData = getCurrentImageData()
			ImageServer<BufferedImage> server = getCurrentServer()
			double downsample = 1

//			def parent = getAnnotationObjects()[0].getROI()
			def parent = pathROI

//create region request
			PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(imageData.getServerPath(), downsample, parent))
			def ip = pathImage.getImage().getProcessor()
			//		IJTools.quickShowImage('test',ip)

			Calibration cal = pathImage.getImage().getCalibration()

//turn cell objects into a black and white image
			Collection<PathObject> cells = getCurrentHierarchy().getObjectsForROI(PathDetectionObject.class, parent).findAll { it.getPathClass() != getPathClass('LocalPt') }
			cells.removeAll({ it.getROI() instanceof qupath.lib.roi.RectangleROI })
			cells.removeAll { it.getROI().getBoundsWidth() < 1.5 || it.getROI().getBoundsHeight() < 1.5 }

			def cellROIs = cells.collect { it.getROI() }

			def cutoffs = []
			for (def line in LinearComponentExtracter.getLines(parent.getGeometry())) {
				cutoffs.addAll(
						cells.findAll { c -> line.isWithinDistance(c.getROI().getGeometry(), 0) }
				)
			}

			def cellUUIDs = cells.collect { it.getID().toString() }
			def cutoffUUIDs = cutoffs.collect { it.getID().toString() }

			List<PolygonRoi> roisCell = new ArrayList<>()
			List<PolygonRoi> roisShrunk = new ArrayList<>()
			cellROIs.each {
				Roi roiJ = IJTools.convertToIJRoi(it, cal, downsample)
				roisCell.add(roiJ)
				Roi shrunk = RoiEnlarger.enlarge(roiJ, -1 * borderPix)
				if (shrunk != roiJ)
					roisShrunk.add(shrunk)
			}

//create label image of nuclei (1-# nuclei, 0 = background).
			ShortProcessor ipLabels = new ShortProcessor(ip.getWidth(), ip.getHeight())
			ipLabels.set(0.0)
			ShortProcessor ipLabels2 = new ShortProcessor(ip.getWidth(), ip.getHeight())
			ipLabels2.set(0.0)
			RoiLabeling.labelROIs(ipLabels, roisCell)
			RoiLabeling.labelROIs(ipLabels2, roisShrunk)
//ipLabels is now a label image of the cell .
//IJTools.quickShowImage('ipLabels',ipLabels)
			//IJTools.quickShowImage('ipLabels2',ipLabels2)

// Create labelled image for cytoplasm, i.e. remove all nucleus pixels
			for (int i = 0; i < ipLabels.getWidth() * ipLabels.getHeight(); i++) {
				if (ipLabels2.getf(i) != 0)
					ipLabels.setf(i, 0f)
			}
//ipLabels is now missing the center of every cell
			//	IJTools.quickShowImage('borders',ipLabels)

//measure border area
			Map<Integer, Integer> borderAreaMap = new LinkedHashMap<>()

			for (int i = 0; i < ipLabels.getWidth() * ipLabels.getHeight(); i++) {
				borderAreaMap[ipLabels.get(i)] = (borderAreaMap[ipLabels.get(i)] ?: 0) + 1
			}

			List<Double> borderAreas = []
			for (int i = 0; i < roisCell.size(); i++) {
				borderAreas[i] = borderAreaMap[i + 1]
			}

//Measuring border intensity

//initializing the color channels
			Map<String, FloatProcessor> channels = new LinkedHashMap<>()
			// Map of channels to measure for borders only, and their names
//calculate intensity statistics for borders
			Map<String, List<RunningStatistics>> statsBorder = new LinkedHashMap<>()

//channel names
			List<ImageChannel> imageChannels = getCurrentServer().getMetadata().getChannels()

//set up the map that links channel name to a FloatProcessor
			ImagePlus imp = pathImage.getImage();
			for (int c = 1; c <= imp.getNChannels(); c++) {
				String name = imageChannels.get(c - 1).getName();
				if (channels.containsKey(name))
					logger.warn("Channel with duplicate name '{}' - will be skipped", name);
				else
					channels.put(name, imp.getStack().getProcessor(imp.getStackIndex(c, 0, 0)).convertToFloatProcessor());
			}

//calculate statistics, assign to statsBorder
			SimpleImage imgLabels = new PixelImageIJ(ipLabels)
			for (String key : channels.keySet()) {
				List<RunningStatistics> statsList = StatisticsHelper.createRunningStatisticsList(roisCell.size())
				StatisticsHelper.computeRunningStatistics(new PixelImageIJ(channels.get(key)), imgLabels, statsList)
				statsBorder.put(key, statsList)
			}

			def pointCells = []
			for (int i = 0; i < cells.size(); i++) {
				PathObject cell = cells[i]

				PointRoi ptRoiIJ = new PointRoi(1, 1)
				def ptRoi = IJTools.convertToROI(ptRoiIJ, cal, downsample, parent.getImagePlane())
				MeasurementList measurementList = MeasurementListFactory.createMeasurementList(channels.size() + 2, MeasurementList.MeasurementListType.FLOAT);

				for (String key : channels.keySet()) {
					List<RunningStatistics> statsList = statsBorder.get(key);
					RunningStatistics stats = statsList.get(i)
					measurementList.put("Border Mean: " + key, stats.getMean())
					measurementList.put("Border Min: " + key, stats.getMin())
				}

				if (cutoffs.contains(cell)){
					measurementList.put('Cutoff', 1)
				} else {
					measurementList.put('Cutoff', 0)
				}

				measurementList.put('Border Area Pix',borderAreas[i])

				def ptCellObj = PathObjects.createDetectionObject(ptRoi, getPathClass('CellPt'), measurementList);
				ptCellObj.setName(cell.getID().toString())
				pointCells << ptCellObj
			}

//now we need to calculate how much of that border is potentially bleedthrough from a neighboring cell
			ShortProcessor neighborLabels = new ShortProcessor(ip.getWidth(), ip.getHeight())

			//	def neighborAreaMap = new HashMap<String, Integer>() //String = key, integer = pixel Areas

			//	def cellList=makeCellList(imageData)
			List<String> keyList = []
			def pointRois = new HashMap<String, ROI>()

			//make convolve stack
			ImageStack stack = new ImageStack()
			stack.addSlice(ipLabels)

			//	IJTools.quickShowImage('convolved',ipLabels)

			borderPix = borderPix+1;
			float[] blackKernel = new float[((borderPix * 2) + 1)**2]

			for (int x = 0; x <= borderPix * 2; x++) {
				for (int y = 0; y <= borderPix * 2; y++) {
					if (((x - borderPix)**2 + (y - borderPix)**2) <= borderPix**2) {
						if ((x != borderPix) || (y != borderPix)) {
							def k = blackKernel.clone()
							int idx = (x * (borderPix * 2 + 1)) + y
							k[idx] = 1.0f
							def conv = ipLabels.duplicate()
							conv.convolve(k, borderPix * 2 + 1, borderPix * 2 + 1)
							//	IJTools.quickShowImage('convolved',conv)
							stack.addSlice(conv)
						}
					}
				}
			}

			int convNum = stack.size()
			float[] empty = new float[convNum]

			for (int i = 0; i < ipLabels.getWidth(); i++) {
				for (int j = 0; j < ipLabels.getHeight(); j++) {

					def vals = stack.getVoxels(i, j, 0, 1, 1, convNum, empty)

					def baseVal = vals[0]
					if ((baseVal != 0)) {
						String cellIDstr = cellUUIDs[baseVal - 1]

						def valList = vals.toSet()
						valList.removeAll { it == 0 }

						String name
						if (valList.size() == 1) { //no neighbor pixel!
							name = cellIDstr.toString()
						} else {
							valList.removeAll { it == baseVal }
							name = cellIDstr.toString() + '_' + cellUUIDs[valList.collect { it - 1 }].sort().join('_')
						}
							if (keyList.contains(name)) {
								neighborLabels.putPixelValue(i, j, keyList.findIndexOf { it == name } + 1)
							} else {
								keyList << name;
								neighborLabels.putPixelValue(i, j, keyList.findIndexOf { it == name } + 1)

								PointRoi ptRoiIJ = new PointRoi(i, j)

								def ptRoi = IJTools.convertToROI(ptRoiIJ, cal, downsample, parent.getImagePlane())
								//def ptRoi = ROIs.createPointsROI(i as double, j as double, parent.getImagePlane())
								pointRois.put(name, ptRoi)
							}
							//neighborAreaMap[name] = (neighborAreaMap[name] ?: 0) + 1

					}
				}
			}

					IJTools.quickShowImage('neighbors',neighborLabels)

			Map<String, List<RunningStatistics>> localStats = new LinkedHashMap<>()
//calculate statistics, assign to statsBorder
			SimpleImage floatLabels = new PixelImageIJ(neighborLabels)
			for (String key : channels.keySet()) {
				List<RunningStatistics> statsList = StatisticsHelper.createRunningStatisticsList(keyList.size())
				StatisticsHelper.computeRunningStatistics(new PixelImageIJ(channels.get(key)), floatLabels, statsList)
				localStats.put(key, statsList)
			}

			def label1 = []
			//	def localAreas = []

			keyList.eachWithIndex { k, idx ->
				label1 << k
				//	localAreas << neighborAreaMap[k]
			}


			def ptObjs = []
			for (int i = 0; i < keyList.size(); i++) {
				MeasurementList measurementList = MeasurementListFactory.createMeasurementList(channels.size() + 1, MeasurementList.MeasurementListType.FLOAT);
				String blobName = keyList[i]

				String primary = blobName.split('_')[0]


				def ptRoi = pointRois[blobName]
				int localStatPix
				for (String key : channels.keySet()) {
					List<RunningStatistics> statsList = localStats.get(key);
					RunningStatistics stats = statsList.get(i)
					measurementList.put("Local: " + key + " mean", stats.getMean())
					if (key == channels.keySet()[0]) {
						localStatPix = stats.size()
					}
				}

				//	measurementList.put('Blob Area Pix', localAreas[i] as double)
				measurementList.put('Blob Area Pix', localStatPix as double)

				if (cutoffUUIDs.contains(primary)) {
					measurementList.put('Cutoff', 1)
				} else {
					measurementList.put('Cutoff', 0)
				}

				PathObject pathObject = PathObjects.createDetectionObject(ptRoi, getPathClass('LocalPt'), measurementList);
				pathObject.setName(keyList[i])
				ptObjs.add(pathObject)
			}

			def toReturn = pointCells + ptObjs
			return toReturn
		}

	}

	@Override
	protected ObjectDetector<BufferedImage> createDetector(final ImageData<BufferedImage> imageData, final ParameterList params) {
		return new BorderInt()
	}


	//FIX THIS
	@Override
	protected boolean parseArgument(ImageData<BufferedImage> imageData, String arg) {
		if (imageData == null)
			return false;
		if (arg != null && arg.length() > 0)
			macroText = arg;
		return true;
	}

	//Once I understand this, delete parent object from borderInt
	@Override
	protected Collection<? extends PathObject> getParentObjects(final ImageData<BufferedImage> imageData) {
		// Try to get currently-selected objects
		PathObjectHierarchy hierarchy = imageData.getHierarchy();
		List<PathObject> pathObjects = hierarchy.getSelectionModel().getSelectedObjects().stream()
				.filter(p -> p.isAnnotation() || p.isTMACore()).toList();
		if (pathObjects.isEmpty()) {
			if (GuiTools.promptForParentObjects(this.getName(), imageData, false, getSupportedParentObjectClasses()))
				pathObjects = new ArrayList<>(hierarchy.getSelectionModel().getSelectedObjects());

		}

		def newParents = pathObjects.collect{PathObjects.createAnnotationObject(it.getROI(),getPathClass('toDelete'))}
		addObjects(newParents)
		return newParents;
	}

	@Override
	ParameterList getDefaultParameterList(ImageData<BufferedImage> imageData) {
		ParameterList params = new ParameterList()
				.addIntParameter('overlap', 'Tile Overlap Size', 50)
				.addIntParameter('radius', 'Boundary Radius', 5)

		return params
	}

	@Override
	protected double getPreferredPixelSizeMicrons(final ImageData<BufferedImage> imageData, final ParameterList params) {
		PixelCalibration cal = imageData.getServer().getPixelCalibration();
		if (cal.hasPixelSizeMicrons())
			return cal.getAveragedPixelSizeMicrons() * getPreferredDownsample(imageData, params);
		return getPreferredDownsample(imageData, params);
	}

	private static double getPreferredDownsample(final ImageData<BufferedImage> imageData, final ParameterList params) {
		return 1.0
	}

	@Override
	protected int getTileOverlap(ImageData<BufferedImage> imageData, ParameterList params) {
		return 15/getPreferredPixelSizeMicrons(imageData,params);
		//return 0
	}

	//I don't know what this does
	@Override
	public Collection<Class<? extends PathObject>> getSupportedParentObjectClasses() {
		List<Class<? extends PathObject>> parents = new ArrayList<>();
		parents.add(PathAnnotationObject.class);
		parents.add(TMACoreObject.class);
		return parents;
	}

}



def imageData = getCurrentImageData()

//Collection<PathObject> cells = getCurrentHierarchy().getDetectionObjects().findAll{it.getPathClass()!=getPathClass('LocalPt')}
Collection<PathObject> cells = getCellObjects()
removeObjects(getCurrentHierarchy().getDetectionObjects().findAll{it.getPathClass()==getPathClass('LocalPt')},true)
removeObjects(getCurrentHierarchy().getDetectionObjects().findAll{it.getPathClass()==getPathClass('CellPt')},true)

def measNames = cells.collect{cell->cell.getMeasurementList().getMeasurementNames().findAll{it.startsWith('Border')}}.flatten().toSet()
measNames.add('Measured')
//def measNames=cells[0].getMeasurementList().getMeasurementNames().findAll{it.startsWith('Border')}
removeMeasurements(PathCellObject, measNames as String[])


def bc = new BorderCompensation()

//I'm pretty sure this does nothing
Field pref = bc.getClass().getSuperclass().getDeclaredField("PREFERRED_TILE_SIZE")
pref.setAccessible(true)
pref.set(bc,512)

Field pref2 = bc.getClass().getSuperclass().getDeclaredField("MAX_TILE_SIZE")
pref2.setAccessible(true)
pref2.set(bc,1024)

//Constructor<?> cons = BorderCompensation.getConstructor();
//final PathPlugin plugin = (PathPlugin)cons.newInstance()

//String pluginName = plugin.getName();
final PathPlugin plugin = bc
TaskRunner runner;

String args=""

var qupath = getQuPath();
def completed
if (isBatchMode() || imageData != qupath.getImageData()) {
	runner = new CommandLineTaskRunner();
	completed = plugin.runPlugin(runner, imageData, args);
	cancelled = runner.isCancelled();
}
else {
	completed = qupath.runPlugin(plugin, args, false);
	cancelled = !completed;
	print(completed)
//	runner = new PluginRunnerFX(qupath);
}

//significant cleanup required
if (completed){
	//remove duplicate localpts
	def localPts = getDetectionObjects().findAll{it.getPathClass()==getPathClass('LocalPt')}
	def named=localPts.groupBy{it.getName()}

	def toRemove=[]
	named.each { k, v ->
		if (v.size() > 1) {
			def cutoffpts = v.findAll { it.measurements['Cutoff'] == 1 }
			if (v.size() > cutoffpts.size()) {
				toRemove.addAll(cutoffpts)
				v.removeAll(cutoffpts)
				if (v.size()>1){
					def ascending = v.toSorted { it.measurements['Blob Area Pix'] }
					toRemove.addAll(ascending[1..-1])
				}
			} else {
				def ascending = v.toSorted { it.measurements['Blob Area Pix'] }
				toRemove.addAll(ascending[0..-2])
			}
		}
	}

	removeObjects(toRemove,true)

	//remove duplicate cell measurements and assign them
	def cellPts = getDetectionObjects().findAll{it.getPathClass()==getPathClass('CellPt')}
	def namedCells=cellPts.groupBy{it.getName()}

	def toRemoveCells=[]
	namedCells.each { k, v ->
		if (v.size() > 1) {
			def cutoffpts = v.findAll { it.measurements['Cutoff'] == 1 }
			if (v.size() > cutoffpts.size()) {
				toRemoveCells.addAll(cutoffpts)
				v.removeAll(cutoffpts)
				if (v.size()>1){
					def ascending = v.toSorted { it.measurements['Blob Area Pix'] }
					toRemove.addAll(ascending[1..-1])
				}
			} else {
				def ascending = v.toSorted { it.measurements['Blob Area Pix'] }
				toRemoveCells.addAll(ascending[0..-2])
			}
		}
	}

	removeObjects(toRemoveCells,true)

	cellPts = getDetectionObjects().findAll{it.getPathClass()==getPathClass('CellPt')}
	def startingCells = getCellObjects()
	cellPts.each{pt->
		def match = startingCells.find{it.getID().toString()==pt.getName()}
		pt.getMeasurementList().getMeasurementNames().each{ml->
			match.measurements[ml]=pt.measurements[ml]
		}
	}

	removeObjects(cellPts,true)

	imageData = getCurrentImageData()
	def entry = getProjectEntry()
	entry.saveImageData(imageData)


	//def project = getProject()
	def imagesToExport = [entry]

	def separator = ","
	def columns = ["Object ID","Border Area Pix"]

	def channels =getCurrentServer().getMetadata().getChannels()
	channels.each{
		columns.add("Border Mean: "+it.getName())
		columns.add("Border Min: "+it.getName())
		//       columns.add("ROI: 0.33 µm per pixel: "+it.getName()+ ": Min")
//	columnsToInclude << "Local: " + it.getName + " mean"
	}


	def columnsToInclude = columns as String[]
	def exportType = PathCellObject.class

	def outputPath = buildFilePath(PROJECT_BASE_DIR,getProjectEntry().getImageName()+'_borderIntCells.csv')
	def outputFile = new File(outputPath)

	def exporter  = new MeasurementExporter()
			.imageList(imagesToExport)            // Images from which measurements will be exported
			.separator(separator)                 // Character that separates values
			.includeOnlyColumns(columnsToInclude) // Columns are case-sensitive
			.exportType(exportType)               // Type of objects to export
			.filter(obj -> obj.getMeasurementList().getMeasurementNames().contains('Border Area Pix'))    // Keep only objects with class 'Tumor'
			.exportMeasurements(outputFile)        // Start the export process

	def columns2 = ["Object ID", "Name","Blob Area Pix"]
	channels.each{
//	columnsToInclude << "Border: "+it.getName()+ " mean"
		columns2.add("Local: " + it.getName() + " mean")
	}

	def columnsToInclude2 = columns2 as String[]

	def outputPath2 = buildFilePath(PROJECT_BASE_DIR, getProjectEntry().getImageName()+'_borderIntBlobs.csv')
	def outputFile2 = new File(outputPath2)


	def exporter2  = new MeasurementExporter()
			.imageList(imagesToExport)            // Images from which measurements will be exported
			.separator(separator)                 // Character that separates values
			.includeOnlyColumns(columnsToInclude2) // Columns are case-sensitive
			.exportType(PathDetectionObject.class)               // Type of objects to export
			.filter(obj -> obj.getPathClass() == getPathClass("LocalPt"))    // Keep only objects with class 'Tumor'
			.exportMeasurements(outputFile2)        // Start the export process


//removeObjects(getDetectionObjects().findAll{it.getPathClass()==getPathClass('LocalPt')},true)

	removeObjects(getAnnotationObjects().findAll{it.getPathClass()==getPathClass('toDelete')},true)

	print('DONE!')
}
