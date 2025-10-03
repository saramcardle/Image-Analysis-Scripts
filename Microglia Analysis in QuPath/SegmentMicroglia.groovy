//imports
import static qupath.lib.gui.scripting.QPEx.*
import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.measure.Calibration
import ij.process.ByteProcessor
import ij.process.FloatPolygon
import ij.process.ImageProcessor
import ij.process.ShortProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.processing.SimpleThresholding
import qupath.imagej.processing.Watershed
import qupath.imagej.tools.IJTools
import qupath.lib.images.PathImage
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import qupath.lib.regions.RegionRequest
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.PolygonROI
import qupath.lib.roi.ROIs
import qupath.lib.roi.RoiTools
import qupath.lib.roi.ShapeSimplifier
import qupath.lib.roi.interfaces.ROI
import qupath.opencv.ops.ImageOps
import qupath.lib.objects.PathDetectionObject
import qupath.lib.regions.ImageRegion
import org.locationtech.jts.geom.util.GeometryCombiner
import org.locationtech.jts.operation.union.UnaryUnionOp
import qupath.lib.roi.GeometryTools
import java.awt.image.BufferedImage
import java.util.stream.Collectors
import qupath.ext.biop.cellpose.Cellpose2D

//You must already have the hippocampus segmented with the class Region*

//***********
//INITIAL SEGMENTATION
//***************

//Iba pixel classifier + cellpose nuclei
//you could replace Cellpose with Stardist and have similar results

removeDetections();

//initial segmentation of Iba1 spots using a trained pixel classifier
selectAnnotations()
createDetectionsFromPixelClassifier("Iba1_v2", 0.2, 0.2, "SPLIT")
def microdets=getDetectionObjects().findAll{it.getPathClass()==getPathClass('Microglia')}

//clear the Iba detections. Will put them back later
removeObjects(microdets,true)


//Initial Segmentation of nuclei using Cellpose
//Requires Cellpose and Cellpose Extension from EPFL BIOP: https://github.com/BIOP/qupath-extension-cellpose
//Code is based on the extension
selectAnnotations()

def imageData = getCurrentImageData()
double pixelSize=imageData.getServer().getPixelCalibration().getAveragedPixelSize()
// Specify the model name (cyto, nuc, cyto2, omni_bact or a path to your custom model)
def pathModel = 'nuc'

def cellpose = Cellpose2D.builder( pathModel )
        .pixelSize( pixelSize )              // Resolution for detection
        .preprocess(
            ImageOps.Channels.extract(0), //Hoechst channel
            ImageOps.Filters.gaussianBlur(2)
        )
        .normalizePercentiles(1,99)
        .flowThreshold(1)            // Threshold for the flows, defaults to 0.4
        .cellprobThreshold(0)
        .diameter(50)                   // Median object diameter. Set to 0.0 for the `bact_omni` model or for automatic computation
        .setOverlap(100)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
        .measureShape()                // Add shape measurements
        .measureIntensity()            // Add cell measurements (in all compartments)
        .build()

// Run detection for the selected objects

def pathObjects = getAnnotationObjects()

cellpose.detectObjects(imageData, pathObjects)
println 'Cellpose Done!'

//Filter nuclei for those that have enough Iba1 to qualify as a microglia nucleus

selectDetections()
addPixelClassifierMeasurements("Iba1_v2", "Iba1_v2") //same classifier as above

//turn area into %
getDetectionObjects().each {
    it.measurements['Iba %']=it.measurements['Iba1_v2: Microglia area µm^2']/it.measurements['Area µm^2']*100
}

runObjectClassifier("MicroNucsv3") //30% Iba1+ Area threshold

//remove all Iba1- nuclei
removeObjects(getDetectionObjects().findAll{it.getPathClass()==null},true)

//I called the nuclei objects "Stroma" because it was one of the default classes and it sounds like "Soma"
//I absolutely regret not calling them Soma because then I had to keep track of Soma vs Stroma for the full project
//Future readers: make better choices

def somas=getDetectionObjects().findAll{it.getPathClass()==getPathClass('Stroma')} //somas = filtered nuclei

//give them a unique class. This will be necessary later for distance measurements against microglia
somas.eachWithIndex {c, idx->
    c.setPathClass(getPathClass('Stroma'+idx.toString()))
    c.setName('Stroma'+idx.toString())
}

//replace the Iba1 objects we deleted above
addObjects(microdets)

/*
def imageData = getCurrentImageData()
def microdets=getDetectionObjects().findAll{it.getPathClass()==getPathClass('Microglia')}
double pixelSize=imageData.getServer().getPixelCalibration().getAveragedPixelSize()
*/

//***********
//Morphological Processing for Microglia
//***************

//measure distance from each Iba blob to EVERY nucleus. This is why we needed individual classes
detectionCentroidDistances(true)

//expansion and joining

//set up some variables
ImageServer<BufferedImage> server = imageData.getServer()
int halfsize = 200 / pixelSize / 2 as int //from every nucleus, we will expand halfsize in every direction to capture potential cell branches. Pick a value slightly larger than the diameter of the largest potential cell
double downsample =1
def hippo=getAnnotationObjects()[0]
def hier = getCurrentHierarchy()

def roi = hippo.getROI()
ImagePlane plane = ImagePlane.getPlane(roi)
def region = ImageRegion.createInstance(roi)

//get all tiles that intersect the hippocampus annotation
//this is important for parallelization below
def tiles = server.getTileRequestManager().getTileRequestsForLevel(1)
        .findAll(tile -> tile.getRegionRequest().intersects(region))


def cleanedMicro=[] //will hold the cleaned up cell objects
tiles.parallelStream().forEach { tile ->
    print(tile) //print just to show it's working

    //dimensions of this tile
    int tx = tile.getRegionRequest().getX()
    int ty = tile.getRegionRequest().getY()
    int tw = tile.getRegionRequest().getWidth()
    int th = tile.getRegionRequest().getHeight()

    //all nuclei inside the box defined by the tile
    def tileroi = ROIs.createRectangleROI(tx, ty, tw, th, plane)
    def tilesoma = hier.getObjectsForROI(PathDetectionObject, tileroi).findAll { it.getPathClass().toString().contains('Stroma') }

    //then loop over each nucleus
    //We have to parallelize over the tiles to ensure that we never try to call the same nucleus in multiple parallel threads, which makes qupath angry
    //then we normal (sequential) loop over the cells in that tile

    tilesoma.each { soma ->
        ROI nuc = soma.getROI()
        int xcen = nuc.getCentroidX()
        int ycen = nuc.getCentroidY()

        //grab the region that is within halfsize of the nucleus centroid
        //I called this circleroi because I thought of it as grabbing all the objects in a circle of radius halfsize
        //but the RegionRequest below is going to use a rectangle, so that's what I did here
        //It doesn't matter, because I picked a halfsize slightly bigger than the largest microglia
        def circleroi = ROIs.createRectangleROI(xcen - halfsize, ycen - halfsize, halfsize * 2, halfsize * 2, plane)

        def circlerequest = RegionRequest.createInstance(server.getPath(), downsample, circleroi)

        //convert the QuPath image into an ImageJ image for morphological processing
        PathImage pathImage = IJTools.convertToImagePlus(server, circlerequest)
        ip = pathImage.getImage().getProcessor()

        //get the pixel calibration (includes XY position)
        Calibration cal = pathImage.getImage().getCalibration()

        //initialize an 8 bit image with the same dimensions
        ByteProcessor bp = new ByteProcessor(ip.getWidth(), ip.getHeight())
        bp.setValue(255)
        //bp will hold the microglia shapes. 0= background, 255 = cell

        //find all Iba blobs that are within 80 um of this nucleus
        def ibaNeighbors = microdets.findAll { it.measurements['Distance to detection with ' + soma.getPathClass().toString() + ' µm'] < 80 }

        //turn these objects into a binary image (bp)
        def ibarois = ibaNeighbors.collect { it.getROI() } //get their ROIS

        //convert QuPath ROIs to ImageJ rois and then fill in
        ibarois.each {
            Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
            bp.fill(roiIJ)
        }

        //this is the morpholigcal closing
        def open = bp.duplicate()
        open.setValue(255)
        open.erode()
        open.erode()
        open.erode()
        open.dilate()
        open.dilate()
        //I know this seems backwards, but in this setup, erode makes it bigger and dilate makes it smaller? I don't know what setting I have backwards, but this worked for me
        //1 fewer "dilation" than "erosion" step to make the microglia capture the edge of the signal that I purposely did not grab in the Iba1 pixel classifier

        //also fill in the full nucleus to ensure it is covered by the cell boundary
        open.fill(IJTools.convertToIJRoi(RoiTools.fillHoles(nuc), cal, downsample))

        //ipLabels is for finding the microglia object and removing all iba blobs that are discontinuous with it
        ShortProcessor ipLabels = new ShortProcessor(ip.getWidth(), ip.getHeight())
        ipLabels.setValue(255)

        //the "primary" iba blobs are the ones that are touching the nucleus. These will definitely be part of the microglia
        def primary = ibaNeighbors.findAll { it.getROI().getGeometry().isWithinDistance(soma.getROI().getGeometry(), 0) }.collect { it.getROI() }

        //fill in ipLabels with the primary iba blobs and the nucleus
        primary.each {
            Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
            ipLabels.fill(roiIJ)
        }
        ipLabels.fill(IJTools.convertToIJRoi(soma.getROI(), cal, downsample))

        //use the watershed function to find connected components
        //open is the "intensity" image, ipLabels is the starting locations. This will only watershed blobs connected to the nucleus.
        // The result will be written to ipLabels
        // https://qupath.github.io/javadoc/docs/qupath/imagej/processing/Watershed.html#doWatershed(ij.process.ImageProcessor,ij.process.ImageProcessor,double,boolean)
        Watershed.doWatershed(open, ipLabels, 2, true)

        //convert 8-bit to true binary
        ipLabels.subtract(254)

        //We're about to convert the binary image to ImageJ ROIs. These cannot have interior holes. holes will hold the holes until we can add them into the QuPath ROIs.
        def holes = ipLabels.duplicate()

        //turns label image into ImageJ rois
        PolygonRoi[] microRegs = RoiLabeling.labelsToFilledROIs(ipLabels, 1)
        //There can only be 1 (roi associated with this nucleus)
        PolygonRoi r = microRegs[0] //r is the cell outer outline

        if (r) { //check that r actually exists
            RoiLabeling.fillOutside(holes, r, 1)
            //now the cell AND the background has a value of 1. Interior holes have a value of 0.
            def invert = SimpleThresholding.thresholdBelow(holes, 1) //reverse that

            def holesLabel = RoiLabeling.labelImage(invert, 0, true) //labels EACH hole individually
            def holesRois = RoiLabeling.labelsToFilledRoiList(holesLabel, true) //turns them into ROIs

            //smooth the cell outline before returning it to QuPath (reduces pixelation effect)
            r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON)
            r = smoothPolygonRoi(r)
            r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON)

            //converts the ImageJ ROI to a QuPath ROI
            PolygonROI pathROI = IJTools.convertToPolygonROI(r, cal, downsample, plane)

            //if there were holes in the cell shape
            if (holesRois) {
                //first, smooth them as above
                holesRois = holesRois.collect { h ->
                    h = new PolygonRoi(h.getInterpolatedPolygon(1, false), Roi.POLYGON)
                    h = smoothPolygonRoi(h)
                    new PolygonRoi(h.getInterpolatedPolygon(Math.min(2, h.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON)
                }


                def holesGeos = holesRois.collect { h ->
                    if (h.size() > 1) {
                        def hroi = IJTools.convertToPolygonROI(h, cal, downsample, plane)
                        hroi = ShapeSimplifier.simplifyPolygon(hroi, 1 / 2.0)
                        return hroi.getGeometry()
                    } else {
                        return null
                    }
                }
                //holesGeos is the JTS geometries of the holes

                //combine the hole geometries into a 1 single geometry
                def geoCombo = GeometryCombiner.combine(holesGeos)
                def geoMerged = UnaryUnionOp.union(geoCombo)

                //subtract the holes from the cell outline
                def microHoles = pathROI.getGeometry().difference(geoMerged)

                //convert the geometry back to the QuPath ROI
                def shapeROI = GeometryTools.geometryToROI(microHoles, plane)

                //convert the QuPath ROI to a detection object
                PathObject shapeObj = PathObjects.createDetectionObject(shapeROI, getPathClass('MicrogliaShape'))
                //new object will be of class MicrogliaShape
                shapeObj.setName(soma.getPathClass().toString()) //named the same at the original nucleus
                cleanedMicro.add(shapeObj) //add it to the running list of new microglia objects

            } else {
                //if there are no hole objects, we can skip many steps and go straight to converting the ROI to a QuPath detection object
                PathObject shapeObj = PathObjects.createDetectionObject(pathROI, getPathClass('MicrogliaShape'))
                shapeObj.setName(soma.getPathClass().toString())
                cleanedMicro.add(shapeObj)
            }
        } //from if(r)
    } //nucleus loop
} //parallel tile loop

//add the microglia shapes into the hierarachy
addObjects(cleanedMicro)
//remove all disconnected iba blobs
removeObjects(microdets,true)


//***********
//Removing Overlaps
//***************
//some Iba blobs will be within 80 um of 2 nuclei. The preceding code will have those microglia overlapping at those areas.
//the next step identifies overlapping cells and watershed separates them

//We want two independent lists to check against
Collection<PathObject> micros=getDetectionObjects().findAll{it.getPathClass()==getPathClass('MicrogliaShape')}
Collection<PathObject> potential=getDetectionObjects().findAll{it.getPathClass()==getPathClass('MicrogliaShape')}

//The original code I used in 2023 had a mistake that allowed some clusters of overlapping cells to remain. I did not catch this until much later.
// Here, I've fixed that issue. Therefore, this isn't *exactly* the code that was used in the paper, but is actually more accurate.

ArrayList<HashSet<PathObject>> duplicates =  new ArrayList<HashSet<PathObject>>() //duplicates will hold a list of sets of overlapping cells, but without overlap
micros.each{micro-> //for each cell
    def neighbors= getNeighborObjects(0,micro,potential) //find other cells that intersect it. This will INCLUDE the cell itself
    if (neighbors.size()>1) { //if any neighbors were found (other than the cell itself)
        def list = duplicates.findIndexOf { it.contains(micro) }
        if (list >= 0) {
            duplicates[list].addAll(neighbors) //add the whole set to duplicates

        } else {
            duplicates.add(neighbors.toSet())
        }
    }
    potential.remove(micro) //remove this cell from the list that is being searched so we don't find the same overlaps twice
}

def pairs = duplicates;
def allSoma =getDetectionObjects().findAll{it.getPathClass().toString().contains('Stroma')}

print('Overlapping Cells')
pairs.each{println(it)}

def demerged=[] //will hold the corrected objects
//to fix the overlap, we're going to turn the objects into an ImageJ binary image, just like we did before
//the code is repetitive with the first step
pairs.each { pair ->

    //find the total region covering all the relevant cells
    def pairCombo = GeometryCombiner.combine(pair.collect{it.getROI().getGeometry()})
    def pairMerged = UnaryUnionOp.union(pairCombo)
    def obj = GeometryTools.geometryToROI(pairMerged, plane)

    def tileroi = ROIs.createRectangleROI(obj.getBoundsX() - 20, obj.getBoundsY() - 20, obj.getBoundsWidth() + 20, obj.getBoundsHeight() + 20, plane) //larger tile because now there could be multiple cells

    def request = RegionRequest.createInstance(server.getPath(), 1, tileroi)
    PathImage pathImage = IJTools.convertToImagePlus(server, request)
    def ip = pathImage.getImage().getProcessor()
    Calibration cal = pathImage.getImage().getCalibration()

    ByteProcessor bp = new ByteProcessor(ip.getWidth(), ip.getHeight())
    bp.setValue(255)

    def pairrois = pair.collect { it.getROI() }
    pairrois.each {
        Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
        bp.fill(roiIJ)
    }

    //find the nuclei that correspond to the overlapping cells
    def pairsoma = pair.collect { p ->
        allSoma.find { it.getPathClass().toString() == p.getName() }
    }
    //collect QuPath ROIS
    def cellRois = pairsoma.collect { it.getROI() }

    //turn them into ImageJ ROIs
    List<PolygonRoi> roisCells = new ArrayList<>()
    cellRois.each {
        Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
        roisCells.add(roiIJ)
        //  bp.fill(roiIJ)
    }

    ShortProcessor ipLabels = new ShortProcessor(ip.getWidth(), ip.getHeight()) //will hold the final result
    RoiLabeling.labelROIs(ipLabels, roisCells) //seed ipLabels with multiple nuclei
    Watershed.doWatershed(bp, ipLabels, 1, true) //watershed as before
    IJTools.quickShowImage('Watershed', ipLabels)
    //turn the watershed labels into ROIs
    PolygonRoi[] newOutlines = RoiLabeling.labelsToFilledROIs(ipLabels, roisCells.size())

    //find the holes
    def holes = bp.duplicate()
    def invert = SimpleThresholding.thresholdBelow(holes, 254)
    def holesLabel = RoiLabeling.labelImage(invert, 0, true)
    def holesRois = RoiLabeling.labelsToFilledRoiList(holesLabel, true)

    newOutlines.each { r ->
        if (r) {
            r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON)
            r = smoothPolygonRoi(r)
            r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON)
            PolygonROI pathROI = IJTools.convertToPolygonROI(r, cal, downsample, plane)

            def psoma = getCurrentHierarchy().getObjectsForROI(PathDetectionObject, pathROI).find { it.getPathClass().toString().contains('Stroma') }
            if (holesRois) {
                holesRois = holesRois.collect { h ->
                    h = new PolygonRoi(h.getInterpolatedPolygon(1, false), Roi.POLYGON)
                    h = smoothPolygonRoi(h)
                    new PolygonRoi(h.getInterpolatedPolygon(Math.min(2, h.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON)
                    
                }

                def holesGeos = holesRois.collect { h ->
                    if (h.size()>1){
                    def hroi = IJTools.convertToPolygonROI(h, cal, downsample, plane)
                    hroi = ShapeSimplifier.simplifyPolygon(hroi, 1 / 2.0)
                    return hroi.getGeometry()
                    } else {
                        return null
                    }
                }

                def geoCombo = GeometryCombiner.combine(holesGeos)
                def geoMerged = UnaryUnionOp.union(geoCombo)

                def microHoles = pathROI.getGeometry().difference(geoMerged)
                def shapeROI = GeometryTools.geometryToROI(microHoles, plane)

                PathObject shapeObj = PathObjects.createDetectionObject(shapeROI, getPathClass('MicrogliaShape'))
                shapeObj.setName(psoma.getPathClass().toString())
                demerged.add(shapeObj)
            } else {
                PathObject shapeObj = PathObjects.createDetectionObject(pathROI, getPathClass('MicrogliaShape'))

                shapeObj.setName(psoma.getPathClass().toString())
                demerged.add(shapeObj)
            }
        } //r if statement
    } //cell loop
} //pairs loop (cluster of overlapping cells)

//add the demerged objects
addObjects(demerged)
//remove the overlapping cell outlines
removeObjects(duplicates.flatten(),true)

print('Overlapping cells separated!')

//***********
//Cleaning Up Artifacts
//***************
/*
Removing any cells that:
    -Are smaller than 50 um2 area
    -Have a nucleus that touches the hippocampus boundary
    -Have a nucleus smaller than 5 um2 area
Also clean up the nucleus classes
 */

pixelSize=imageData.getServer().getPixelCalibration().getAveragedPixelSize()

//remove individual class names for each soma
soma = getDetectionObjects().findAll{it.getPathClass().toString().contains('Stroma')}
soma.each{it.setPathClass(getPathClass('Soma'))}

//make an annotation for "outside the hippocampus"
selectObjectsByClassification("Region*");
makeInverseAnnotation()
getSelectedObject().setPathClass(getPathClass('Other'))

micros=getDetectionObjects().findAll{it.getPathClass().toString().contains('MicrogliaShape')}

//using a distance of 1 um instead of 0 because sometimes the smoothing applied during segmentation means a border cell is not quite touching the outside object
def edge =getNeighborObjects(1,getAnnotationObjects().find{it.getPathClass()==getPathClass('Other')},soma)
def names =edge.collect{it.getName()}

def toRemove = edge //toRemove is all cells and nuclei that will get deleted
toRemove.addAll(micros.findAll{names.contains(it.getName())}) //also add all associated microglia

def smallSoma = soma.findAll{it.getROI().getArea()*pixelSize*pixelSize<5.0} //find all very small nuclei
toRemove.addAll(smallSoma) //add them to the toRemove list
toRemove.addAll(micros.findAll{m->smallSoma.collect{s->s.getName()}.contains(m.getName())}) //also add the associated cell outside

def smallMicros = micros.findAll{it.getROI().getArea()*pixelSize*pixelSize<50.0} //find all very small
toRemove.addAll(smallMicros)
toRemove.addAll(soma.findAll{s->smallMicros.collect{m->m.getName()}.contains(s.getName())}) //remove all matching nuclei

//delete the artifacts and the Other object
removeObjects(toRemove as Set,true )
removeObjects(getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Other')},true)

resetSelection()
print('SEGMENTATION FINISHED!')

//subfunctions
private static PolygonRoi smoothPolygonRoi(PolygonRoi r) {
    //part of the QuPath source code (watershed cell detection)
    FloatPolygon poly = r.getFloatPolygon()
    FloatPolygon poly2 = new FloatPolygon()
    int nPoints = poly.npoints
    for (int i = 0; i < nPoints; i += 2) {
        int iMinus = (i + nPoints - 1) % nPoints
        int iPlus = (i + 1) % nPoints
        poly2.addPoint((poly.xpoints[iMinus] + poly.xpoints[iPlus] + poly.xpoints[i])/3,
                (poly.ypoints[iMinus] + poly.ypoints[iPlus] + poly.ypoints[i])/3)
    }
//			return new PolygonRoi(poly2, r.getType())
    return new PolygonRoi(poly2, Roi.POLYGON)
}

static Collection<PathObject> getNeighborObjects(double distancePixels, PathObject obj, Collection<PathObject> regionObjs) {
    //Adapted from https://gist.github.com/petebankhead/aac937b112724ab1626b020b6cca87b4

    // def detections = getDetectionObjects().findAll{!it.isCell()}
    def roi = obj.getROI()
    if (roi == null)
        return null // Shouldn't actually happen...

    def geometry = roi.getGeometry()
    def neighbors=regionObjs.findAll{it.getROI().getGeometry().isWithinDistance(geometry,distancePixels)}

    return neighbors
}