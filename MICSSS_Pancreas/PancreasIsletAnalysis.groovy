/*********
FULL ANALYSIS. To be run in 0.5.0 or better with stardist extension.
 Referred to as the "One Script To Rule Them All" throughout the project.

Run script from one base image, CD45 currently. DO NOT RUN FOR PROJECT.
The script will:
  1. Generate a  tissue detection
  2. Distribute that across all other images in the project
  3. Run a pixel classifier determined by the image's name (or not for some markers) to generate detections
  4. Merge detections into islets
  5. Distribute Islets to all images and take measurements of their intensities
  6. Merge all the measurements into a single object set
  7. Run an object classifier to remove incorrect islets
  8. Distribute the islets, as named annotations, to the other images
 9. Run a pixel classifier to find positive staining in the endocrine channels. Organize everything by name.
 10. Expand the islets to find the "peri islet" region
 11. In the CD45 channel, use StarDist to detect cells in the expanded region. Run an object classifier to mark CD45+ cells.
 12. Put the CD45 statistics into the islet annotation for export and into the islet/periislet detection for measurement maps
 13. Gather the positive stains from the other images.
 14. Colocalize stains of interest (currently every doublet) and find union of all.
 15. Write the areas and colocalized areas to the islet annotations for export and detections for measurement maps.

 EXPECTATIONS:
 Project contains 9 images from sequential scans of a pancreas, each stained with a different antibody, using the MICSSS Protocol [https://pubmed.ncbi.nlm.nih.gov/31502167/]
   - 8 endocrine stains followed by CD45. The CD45 is used as the Base Image throughout the alignment and analysis
   - Each image is labeled with the same Patient ID num and the antibody name
 In the Pixel Classifiers folder:
   - Low-res pixel classifiers to find basic islet outlines
   - High-res pixel classifiers to find precise positive regions
   - Pixel Classifier to detect pancreas tissue overall
 In the Object Classifiers folder
   - Trained ML Object classifier to remove false islets.
   - Trained ML Object classifier to remove false cells
   - Trained ML object classifier to find CD45+
 A subfolder called Affine with the pre-calculated affine transforms between images (from MICSSSAlignment.groovy)
********/
org.locationtech.jts.geom.GeometryOverlay.isOverlayNG = true //prevents JTS errors downstream

/**Things to define**/

isletClassifier = "ThresholderClassifier062220v3"
Distance=10 //Annotation erosion distance to avoid edge artifacts
boolean fullTissue=true //true to create original full pancreas annotation

def pathModel =  "C:\\Users\\smcardle\\Downloads\\dsb2018_heavy_augment.pb" //Stardist model
double expansionDistMicrons=20  //peri islet distance

/**Preparation Code**/
def baseImageName = getProjectEntry().getImageName()
def imageData = getCurrentImageData()
def cal=imageData.getServer().getPixelCalibration()
def pixelSize=cal.getAveragedPixelSize()
def project=getProject()
String affinepath = buildFilePath(PROJECT_BASE_DIR, 'Affine '+baseImageName)

/**Tissue Detection**/
if (fullTissue) {
    clearAllObjects()  //Start fresh
    createAnnotationsFromPixelClassifier("TissueDetection", 100000.0, 5000.0, "DELETE_EXISTING", "SELECT_NEW")

    //erode by Distance to get rid of islets touching the edge (likely damaged)
    runPlugin('qupath.lib.plugins.objects.DilateAnnotationPlugin', '{"radiusMicrons": -'+Distance.toString()+',  "lineCap": "Round",  "removeInterior": false,  "constrainToParent": true}')
    def large = getAnnotationObjects().find{it.getLevel()==1}
    removeObject(large,true)
    selectAnnotations()
    runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

    //remove small objects that aren't truly pancreas
    def small=getAnnotationObjects().findAll{it.getROI().getArea()<900000}
    if (small.size()>0){
        removeObjects(small,true)
    }

    //merge remaining
    selectAnnotations()
    mergeSelectedAnnotations()
    getAnnotationObjects().each{it.setLocked(true)}
} else {
    getAnnotationObjects().each{it.setLocked(true)}
}

//save
getProject().getEntry(getCurrentImageData()).saveImageData(getCurrentImageData())
 
/**Distribute Tissue Detection to each image**/
deleteExisting = true // Delete existing objects
createInverse = false //Change this if things end up in the wrong place
pathObjects = getAnnotationObjects() //What do you want to transfer? getDetectionObjects() ? getCellObjects()?

new File(affinepath).eachFile{ f->
    DistributeObjects(deleteExisting, createInverse,pathObjects, f)
}
print 'Done distributing tissue detection!'

/**Set Stains. **/
setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.67271 0.63325 0.38269 ", "Stain 2" : "DAB", "Values 2" : "0.27625 0.65227 0.70586 ", "Background" : " 218 214 214 "}')
def stains = getCurrentImageData().getColorDeconvolutionStains()
imageData.hierarchyChanged()

/**In each endocrine stain image, Run classifier or thresholders for many stains to find pieces of islets **/
project.getImageList().each{ //loops through project
    if(it.getImageName() != baseImageName){
        String imgName = it.getImageName()
        imageData = it.readImageData()
        def hierarchy = imageData.getHierarchy()
        def otherAnno = hierarchy.getAnnotationObjects()
        hierarchy.getSelectionModel().setSelectedObjects(otherAnno, null)
        imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
        println("Working on: "+it)

        imageData.setColorDeconvolutionStains(stains)

        if (imgName.contains("_INS")) {
            def classifier = project.getPixelClassifiers().get('DAB3')
        //    classifier=project.getPixelClassifiers().get('DAB_lowres')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        } else if (imgName.contains("_Pro")){
            def classifier = project.getPixelClassifiers().get('DAB3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        } else if (imgName.contains("_SST")){
            def classifier = project.getPixelClassifiers().get('DAB_closing')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        } else if (imgName.contains("_CHGA")){
            def classifier = project.getPixelClassifiers().get('DAB3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        } else if (imgName.contains("_GCG -")){
            def classifier = project.getPixelClassifiers().get('DAB3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        }else if (imgName.contains("_PPY")){
            def classifier = project.getPixelClassifiers().get('DAB_closing')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 50.0, 1000.0,PixelClassifierTools.CreateObjectOptions.SPLIT)
        } else{
            print "Skipping"
            return
        }
        it.saveImageData(imageData)
    }
}

print "Done writing individual stain detections to project!"

/**Gather the created objects to the CD45 channel**/
deleteExisting = false // Delete existing objects
createInverse = true // Change this if things end up in the wrong place

new File(affinepath).eachFile{ f->
   GatherObjects(deleteExisting, createInverse, f)
}

//save
getProject().getEntry(getCurrentImageData()).saveImageData(getCurrentImageData())
print "Gathered Initial Islet Objects"

/** Create an Outside of tissue object **/
//to be used later
def tissue=getAnnotationObjects()[0]
makeInverseAnnotation(tissue)
def outline=getCurrentHierarchy().getSelectionModel().getSelectedObject()
outline.setPathClass(getPathClass("Outside"))
outline.setLocked(true)
def outsidegeo=outline.getROI().getGeometry()


/**Merge overlapping detections from each stain into islets objects*/
def islets= getDetectionObjects()
def imagePlane=islets[0].getROI().getImagePlane()
    PrecisionModel PM = new PrecisionModel(PrecisionModel.FIXED)
int stepsize=2000;

Geometry merged

//need to do this in steps for memory conservation
if (islets.size()<stepsize*1.5){
    //if there are few enough islets that they can all be handled at once
    print("small islet num")
    def geos=islets.collect{GeometryPrecisionReducer.reduce(it.getROI().getGeometry(), PM)} //collect geometries
    def combined=GeometryCombiner.combine(geos) //combine into a single geometry object with many lines
    print ("geometry combined")
    merged= UnaryUnionOp.union(combined) //perform union to combine all detections into a single object
    print"Union complete"
} else {
    //if there are many, go from left to right
    def sortedX=islets.toSorted {it.getROI().getCentroidX()}
    int steps = Math.ceil(islets.size()/stepsize)

    def mergedParts=[]
    for (int r=0; r<steps; r++){
        print r + " out of " + steps-1
        def sublist = sortedX[r*stepsize.. Math.min((r + 1) * stepsize - 1,islets.size()-1)]
        def geos=sublist.collect{GeometryPrecisionReducer.reduce(it.getROI().getGeometry(), PM)}
        def combined=GeometryCombiner.combine(geos)
        mergedParts[r]= UnaryUnionOp.union(combined) //combine all detections into a single object
        }
    merged = UnaryUnionOp.union(GeometryCombiner.combine(mergedParts))
    print "full union complete"
}

//take the single geometry and split into component parts
def mergedRois=GeometryTools.geometryToROI(merged, imagePlane)
def splitRois=RoiTools.splitROI(mergedRois) //split them into non-touching objects
print("split touching objects")
def filledRois=splitRois.collect{RoiTools.fillHoles(it)} //fill holes
def largeRois=filledRois.findAll{it.getArea()>1000/pixelSize/pixelSize} //remove small objects //1000
def largegeos=largeRois.collect{it.getGeometry()}

//remove objects touching outer boundary
//outside geo defined above
def notintersects=[]
largegeos.eachWithIndex{entry,idx->
    if (!entry.intersects(outsidegeo)){
        notintersects<<idx
    }
}
print("cleaned and filtered")

//create new detection objects
def newObjs=[]
def isletClass=getPathClass("Islet")

largegeos[notintersects].each{
    def roi=GeometryTools.geometryToROI(it,imagePlane)
    newObjs << PathObjects.createDetectionObject(roi,isletClass)
}

//add merged islets
addObjects(newObjs)

//remove islet parts
removeObjects(islets,true) //turn on clean up once you are confident in classifiers
print('Done Merging detections into Islets')

//save
getProject().getEntry(getCurrentImageData()).saveImageData(getCurrentImageData())

/** Measure CD45 intensities per islet**/
setColorDeconvolutionStains('{"Name" : "CD45", "Stain 1" : "Hematoxylin", "Values 1" : "0.71669 0.59394 0.3655 ", "Stain 2" : "DAB", "Values 2" : "0.37271 0.68418 0.62689 ", "Background" : " 205 200 203 "}')
stains = getCurrentImageData().getColorDeconvolutionStains()

//measure intensity in CD45 image
def origDetections=getDetectionObjects()
origDetections.each{
    it.setName('CD45')
}
selectDetections()
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.5,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": false,  "colorStain1": false,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": true,  "doMinMax": true,  "doMedian": false,  "doHaralick": true,  "haralickDistance": 1,  "haralickBins": 32}')

/**Distrubute the islets, set their names and measurements, then Gather them back **/
deleteExisting = true //Delete existing objects
createInverse = false //Change this if things end up in the wrong place
pathObjects = getDetectionObjects()

new File(affinepath).eachFile{ f->
    DistributeObjects(deleteExisting, createInverse, pathObjects, f)

    //measure intensity in each image
    def entry = project.getImageList().find {it.getImageName()+".aff" == f.getName()}
    imageData = entry.readImageData()
    def otherHierarchy = imageData.getHierarchy()

    imageData.setColorDeconvolutionStains(stains)

    def otherDetections = otherHierarchy.getDetectionObjects()
    otherHierarchy.getSelectionModel().setSelectedObjects(otherDetections, null)

    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin',  imageData,'{"pixelSizeMicrons": 0.5,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": false,  "colorStain1": false,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": true,  "doMinMax": true,  "doMedian": false,  "doHaralick": true,  "haralickDistance": 1,  "haralickBins": 32}')

    String stain=f.getName().split('_')[1].split('-')[0]
    print(stain)
    otherDetections.each{
        it.setName(stain)
    }

    entry.saveImageData(imageData)
    GatherObjects(false,true,f)
}
getProject().getEntry(getCurrentImageData()).saveImageData(getCurrentImageData())
print('Done calculating intensities')

/**Find matching detection objects by centroid location. Combine all of their measurements into one.**/
def allDetections=getDetectionObjects()

def centroidX=allDetections.collect{Math.round(it.getROI().getCentroidX())}
def centroidY=allDetections.collect{Math.round(it.getROI().getCentroidY())}

def toRemove=[]

def measurements = origDetections[0].getMeasurementList().measurementNames as Set
measurements.removeAll {it.contains("Area")}

origDetections.each {det->
    //def measurements = det.getMeasurementList() as Set
    //measurements.removeAll {it.contains("Area")}
    MeasurementList origML =det.getMeasurementList()

    def matches = centroidX.findIndexValues { it == Math.round(det.getROI().getCentroidX()) }.intersect(centroidY.findIndexValues { it == Math.round(det.getROI().getCentroidY()) })
    def matched=allDetections[matches]

    matched.each { m ->
        MeasurementList matchedml = m.getMeasurementList()

        measurements.each {
            def chInt=matchedml.getMeasurementValue(it)
            if (chInt) {
                origML.putMeasurement(m.getName() + it, chInt)

            } else {
                origML.putMeasurement(m.getName() + it, Double.NaN)
                print(m.getName() + it + " does not exist!")
            }
            //origML.removeMeasurements(it)
        }
    }
    origML.removeMeasurements(measurements)

    matched.findAll{it.getName()!="CD45"}.each{
        toRemove<<it
    }
}

//Remove Duplicate Objects
removeObjects(toRemove,true)

print "Done gathering intensity measurements"
getProject().getEntry(getCurrentImageData()).saveImageData(getCurrentImageData())

selectDetections()
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")

/**Classify as Islet/Other and remove false islets**/
runObjectClassifier(isletClassifier) //trained previously

removeObjects(getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Outside')},true)

def otherobjs=getDetectionObjects().findAll{it.getPathClass()==getPathClass('Other')}
removeObjects(otherobjs,true)

//name each islet, create a matching annotation
def sorted=getDetectionObjects().findAll{it.getPathClass()==isletClass}.toSorted{it.getROI().getCentroidY()}
sorted.eachWithIndex {det, i -> det.setName("Islet " + (i+1))}
def finalIsletrois=sorted.collect{it.getROI()}

def isletAnnos=[]
finalIsletrois.eachWithIndex{ roi, i ->
    def newAnnot = PathObjects.createAnnotationObject(roi,isletClass)
    newAnnot.setName("Islet " + (i+1))
    isletAnnos << newAnnot
}

addObjects(isletAnnos)

/**Distribute cleaned islet annotations to each other image**/
deleteExisting = true // Delete existing objects
createInverse = false //Change this if things end up in the wrong place
pathObjects = isletAnnos //What do you want to transfer? getDetectionObjects() ? getCellObjects()?

new File(affinepath).eachFile{ f->
    DistributeObjects(deleteExisting, createInverse,pathObjects, f)
}

/**Measure true stain area in each islet with better resolution**/
//name the resulting detection by stain name
project.getImageList().each {
    if (it.getImageName() != baseImageName) {
        String imgName = it.getImageName()
        imageData = it.readImageData()
        def hierarchy = imageData.getHierarchy()
        def otherAnno = hierarchy.getAnnotationObjects()
        hierarchy.getSelectionModel().setSelectedObjects(otherAnno, null)

        String stain=imgName.split('_')[1].split('-')[0]
        stain=stain.substring(0,stain.size()-1)

        if (imgName.contains("_INS")) {
            classifier = project.getPixelClassifiers().get('POS2')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 5, 5)
        } else if (imgName.contains("_Pro")){
            //will change this one to be special
            def classifier1 = project.getPixelClassifiers().get('ProINSlight2')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier1, 1, 1)
            def classifier2 = project.getPixelClassifiers().get('ProINSdark')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier2, 1, 1)
/*
            def proins=hierarchy.getDetectionObjects()
            otherAnno.each{islet->
                def isletproins=proins.findAll{it.getParent()==islet}
                def dark=isletproins.find{it.getPathClass()==getPathClass('PROINS D')}
                def light=isletproins.find{it.getPathClass()==getPathClass('PROINS L')}
                if (dark? & light?){
                    def lgeo=light.getROI().getGeometry()
                    def dgeo=dark.getROI().getGeometry()

                    def newgeo=lgeo.difference(dgeo)
                    def newlight=PathObjects.createDetectionObject( GeometryTools.geometryToROI(newgeo, islet.getROI().getImagePlane()))

                }*/

        } else if (imgName.contains("_SST")){
            classifier = project.getPixelClassifiers().get('POS3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 1, 1)
        } else if (imgName.contains("_CHGA")){
            classifier = project.getPixelClassifiers().get('POS2')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 5, 5)
        } else if (imgName.contains("_GCG -")){
            classifier = project.getPixelClassifiers().get('ProINSlight2')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 1, 1)

            hierarchy.getDetectionObjects().findAll{it.getPathClass()==getPathClass('PROINS L')}.each{it.setPathClass(getPathClass('Chromogranin'))}
            hierarchy.getDetectionObjects().each{it.setName(it.getParent().getName())}
        }else if (imgName.contains("_PPY")){
            classifier = project.getPixelClassifiers().get('POS3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 1, 1)
        }else if (imgName.contains("IAPP")){
            classifier = project.getPixelClassifiers().get('POS3')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 1, 1)
        }else if (imgName.contains("GCG Rb")){
            classifier = project.getPixelClassifiers().get('POS2')
            PixelClassifierTools.createDetectionsFromPixelClassifier(imageData,classifier, 5, 5)
        }

        if (imgName.contains("_Pro")){
            hierarchy.getDetectionObjects().findAll{it.getPathClass()==getPathClass('PROINS D')}.each{it.setPathClass(getPathClass('ProINS Dark'))}
            hierarchy.getDetectionObjects().findAll{it.getPathClass()==getPathClass('PROINS L')}.each{it.setPathClass(getPathClass('ProINS Total'))}
            hierarchy.getDetectionObjects().each{it.setName(it.getParent().getName())}
        } else{
            hierarchy.getDetectionObjects().each{it.setPathClass(getPathClass(stain))}
            hierarchy.getDetectionObjects().each{it.setName(it.getParent().getName())}
        }
        it.saveImageData(imageData)
    }
}

/**Islet Expansion to find peri islet region**/
//this will expand the islet boundaries by Expansion Distance, making sure to NOT overlap with the neighbors
//taken from Watershed Cell Detection code
//https://github.com/qupath/qupath/blob/master/qupath-core-processing/src/main/java/qupath/imagej/detect/cells/WatershedCellDetection.java#L844

//I didn't change all of the variable names from Pete's original code. Nuclei = islet. Cytoplasm = periislet region.
imageData=getCurrentImageData()
ImageServer<BufferedImage> server = imageData.getServer()
double downsample = 16
double downsampleSqrt = Math.sqrt(downsample)
ColorDeconvolutionStains stains2 = imageData.getColorDeconvolutionStains()
makeMeasurements=false
smoothBoundaries=true

//Expand Islets
RegionRequest.createInstance(server, downsample)
PathImage pathImage = IJTools.convertToImagePlus(server, RegionRequest.createInstance(server, downsample))
ip = pathImage.getImage().getProcessor()
//IJTools.quickShowImage('test',ip)
cal = pathImage.getImage().getCalibration()
ImagePlane plane = ImagePlane.getPlane(getAnnotationObjects()[0].getROI())

//black image to put nuclei on top of
ByteProcessor bp = new ByteProcessor(ip.getWidth(), ip.getHeight())
bp.setValue(255)

//initializing the color channels
Map<String, FloatProcessor> channels = new LinkedHashMap<>() // Map of channels to measure for nuclei only, and their names
Map<String, FloatProcessor> channelsCell = new LinkedHashMap<>() // Map of channels to measure for cell/cytoplasm, and their names

//get the deconvolved stains
if (ip instanceof ColorProcessor && stains2 != null) {
    FloatProcessor[] fps = IJTools.colorDeconvolve((ColorProcessor) ip, stains2)
    for (int i = 0; i < 3; i++) {
        StainVector stain = stains2.getStain(i + 1)
        if (!stain.isResidual()) {
            channels.put(stain.getName() + " OD", fps[i])
            channelsCell.put(stain.getName() + " OD", fps[i])
        }
    }

    fpDetection = (FloatProcessor) fps[0].duplicate()
    //switch to this to use Optical Density Sum
    //fpDetection = IJTools.convertToOpticalDensitySum((ColorProcessor)ip, stains2.getMaxRed(), stains2.getMaxGreen(), stains2.getMaxBlue())
}

//turn nuclei objects into a black and white image
def isletObjs = getDetectionObjects().findAll{it.getPathClass()==getPathClass("Islet")}
def isletROIs = isletObjs.collect { it.getROI() }
List<PolygonRoi> roisIslet = new ArrayList<>()
isletROIs.each {
    Roi roiIJ = IJTools.convertToIJRoi(it, cal, downsample)
    roisIslet.add(roiIJ)
    bp.fill(roiIJ)
}
//bp is now a black and white image with nuclei.

expPixels=expansionDistMicrons/pixelSize/downsample //defined above

//create label image of nuclei (1-# nuclei, 0 = background).
ShortProcessor ipLabels = new ShortProcessor(ip.getWidth(), ip.getHeight())
RoiLabeling.labelROIs(ipLabels, roisIslet)
//ipLabels is now a label image of the nuclei .

//calculate intensity statistics for nuclei
Map<String, List<RunningStatistics>> statsMap = new LinkedHashMap<>()
if (makeMeasurements) {
    SimpleImage imgLabels = new PixelImageIJ(ipLabels)
    for (String key : channels.keySet()) {
        List<RunningStatistics> statsList = StatisticsHelper.createRunningStatisticsList(roisNuclei.size())
        StatisticsHelper.computeRunningStatistics(new PixelImageIJ(channels.get(key)), imgLabels, statsList)
        statsMap.put(key, statsList)
    }
}

//Now we are ready to find the cell boundaries
//Euclidean Distance Map of distance to nuclei.
FloatProcessor fpEDM = new EDM().makeFloatEDM(bp, (byte) 255, false)
fpEDM.multiply(-1)
//fpEDM = 0 at all nuclei pixels, increasingly negative as you get further away

//watershed segmentation on binary nuclei image, up to the distance given by cellExpansion
ImageProcessor ipLabelsCells = ipLabels.duplicate()
double cellExpansionThreshold = -expPixels
Watershed.doWatershed(fpEDM, ipLabelsCells, cellExpansionThreshold, false)
//ipLabelCells now is a label image, like ipLabels, but larger for the cells. Already watershed segmented.

//turn label image into rois (imageJ rois, not yet qupath rois)
PolygonRoi[] roisRegions = RoiLabeling.labelsToFilledROIs(ipLabelsCells, roisIslet.size())

//calculate intensity measurements on cells
Map<String, List<RunningStatistics>> statsMapCell = new LinkedHashMap<>()

// Create labelled image for cytoplasm, i.e. remove all nucleus pixels
for (int i = 0; i < ipLabels.getWidth() * ipLabels.getHeight(); i++) {
    if (ipLabels.getf(i) != 0)
        ipLabelsCells.setf(i, 0f)
}

//calculate intensity measurements on cytoplasm
Map<String, List<RunningStatistics>> statsMapCytoplasm = new LinkedHashMap<>()

//turns the ImageJ rois into QuPath ROIs
List<PathObject> expandedObjects = new ArrayList<>() //initialize
for (int i = 0; i < roisRegions.length; i++) {
    //actually add the measurements to the objects
    PolygonRoi rN = roisIslet.get(i)
    PolygonRoi r = roisRegions[i]
    def isletObj = isletObjs[i]

    if (r == null)
        continue

    //smooth boundaries of cell polygons
    if (smoothBoundaries) {
        r = new PolygonRoi(r.getInterpolatedPolygon(1, false), Roi.POLYGON)
        r = smoothPolygonRoi(r)
        r = new PolygonRoi(r.getInterpolatedPolygon(Math.min(2, r.getNCoordinates().toDouble() * 0.1), false), Roi.POLYGON)
    }

    PolygonROI pathROI = IJTools.convertToPolygonROI(r, cal, downsample, plane)
    if (smoothBoundaries)
        pathROI = ShapeSimplifier.simplifyPolygon(pathROI, downsampleSqrt / 2.0)

    // Create & store the cell object
    PathObject pathObject= PathObjects.createAnnotationObject(pathROI)
    pathObject.setName(isletObj.getName().replace('Islet','Expanded'))

    pathObject.setPathClass(getPathClass('PeriIslet'))
    expandedObjects.add(pathObject)
}

for (PathObject pathObject : expandedObjects)
    pathObject.getMeasurementList().close()

addObjects(expandedObjects)

resolveHierarchy()
print('Islet Expansion Complete')
//now you have a new annotation that is larger than each islet, wih the same ID num

/**Stardist to find CD45+ Leukocytes**/

isletDetections=getDetectionObjects().findAll{it.getPathClass()==getPathClass('Islet')}
isletAnnotations=getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Islet')}

removeObjects(isletDetections,true)

print('Detecting Nuclei via Stardist')
def stardist = StarDist2D.builder(pathModel)
        .preprocess(
                ImageOps.Channels.deconvolve(stains),
                ImageOps.Channels.extract(0),
                ImageOps.Filters.gaussianBlur(1),
        )
        .threshold(0.6)              // Prediction threshold
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(pixelSize)              // Resolution for detection
        .includeProbability(true)    // Add probability as a measurement (enables later filtering)
        .tileSize(1024)              // Specify width &d height of the tile used for prediction
        .constrainToParent(false)
        .cellExpansion(1)
        .measureShape()
        .measureIntensity()
        .nThreads(8)
        .build()

stardist.detectObjects(imageData,expandedObjects)
println 'StarDist Done!'

/**Make measurements of all cells to classify CD45+**/
getCurrentHierarchy().getSelectionModel().setSelectedObjects(expandedObjects,null);

//measure local avg
runPlugin('qupath.lib.plugins.objects.SmoothFeaturesPlugin', '{"fwhmMicrons": 100.0,  "smoothWithinClasses": false}')

//calculate Normalized, which is cell value minus local avg for every measurement
getCellObjects().each { cell ->
    def ml = cell.getMeasurementList()
    def names = ml.measurementNames
    names.findAll { it.contains('Smoothed: ') }.each {
        String basename = it.substring(18)
        ml.putMeasurement('Normalized: ' + basename, ml.getMeasurementValue(it) - ml.getMeasurementValue(basename))
    }
}

//Circular tiles of 10 ums
selectCells();
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.22,  "region": "Circular tiles",  "tileSizeMicrons": 10.0,  "colorOD": false,  "colorStain1": false,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": true,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');

//Haralick features
selectCells();
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.22,  "region": "ROI",  "tileSizeMicrons": 10.0,  "colorOD": false,  "colorStain1": true,  "colorStain2": true,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": false,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": true,  "haralickDistance": 1,  "haralickBins": 32}');


//calculate overall max DAB above total background
getCellObjects().each { cell ->
    double maxOf = [measurement(cell, "DAB: Membrane: Mean"), measurement(cell, "DAB: Nucleus: Mean"), measurement(cell, "DAB: Cytoplasm: Mean")].max()
    double overBackground = maxOf/(0.2+measurement(cell,"Circle: Diameter 10.0 µm: 0.22 µm per pixel: DAB: Mean"))
    cell.getMeasurementList().putMeasurement("DAB over background", overBackground)
}

//remove objects that aren't cells
runObjectClassifier("NotACell2");
removeObjects(getCellObjects().findAll{it.getPathClass().toString()=='Not'},true)
print("Removed Non cells")

//classify into CD45+ and Negative
print("CD45 classification")
runObjectClassifier("CD45_selected_v3")
//runPlugin('qupath.opencv.features.DelaunayClusteringPlugin', '{"distanceThresholdMicrons": 50,  "limitByClass": true,  "addClusterMeasurements": true}')

addObjects(isletDetections)
addObjects(isletAnnotations)
resolveHierarchy() //important for determining intraislet vs periislet

/**Summarize into Islet and Peri-islet measurements**/
def cells=getCellObjects()

//def tissue=getAnnotationObjects().find{it.getPathClass()==getPathClass('Stroma')}
int tissuePos=0
int tissueNeg=0
print("Assigning Measurements")
clusterSizes=[]
dispDets=[]

expandedObjects.each{annot->
    def innerIslet=getAnnotationObjects().find{it.getName().equals(annot.getName().replace('Expanded', 'Islet'))}
    def ringChildren=cells.findAll{it.getParent()==annot}
    def isletChildren=cells.findAll{it.getParent()==innerIslet}

    def isletRoi=innerIslet.getROI()
    def geo=annot.getROI().getGeometry()
    def ring=geo.difference(isletRoi.getGeometry()) //get just the  peri-islet "ring"
    def ringRoi = GeometryTools.geometryToROI(ring, annot.getROI().getImagePlane())
    def ringObj = PathObjects.createDetectionObject(ringRoi)
    ringObj.setPathClass(getPathClass('PeriIslet'))
    ringObj.setName(annot.getName().replace('Expanded','Ring'))
    def isletObj = getDetectionObjects().find{it.getName().equals(annot.getName().replace('Expanded', 'Islet'))}
    //def isletObj = PathObjects.createDetectionObject(isletRoi)
    //  def detection = new PathDetectionObject(roi, getPathClass("Islet"))

    def isletAnnotml=innerIslet.getMeasurementList()
    def isletml=isletObj.getMeasurementList()
    //number and percentage of CD45+ in intra, peri, and total islet regions
    if (isletChildren){
        isletml.putMeasurement('CD45 neg',isletChildren.findAll{it.getPathClass()==null}.size())
        isletml.putMeasurement('CD45 pos',isletChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size())
        isletml.putMeasurement('%CD45 pos',isletChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()/isletChildren.size()*100)

        isletAnnotml.putMeasurement('CD45 neg',isletChildren.findAll{it.getPathClass()==null}.size())
        isletAnnotml.putMeasurement('CD45 pos',isletChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size())
        isletAnnotml.putMeasurement('%CD45 pos',isletChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()/isletChildren.size()*100)
        tissuePos+=isletChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()
        tissueNeg+=isletChildren.findAll{it.getPathClass()==null}.size()
    } else {

        isletml.putMeasurement('CD45 neg',0)
        isletml.putMeasurement('CD45 pos',0)
        isletml.putMeasurement('%CD45 pos',Double.NaN)

        isletAnnotml.putMeasurement('CD45 neg',0)
        isletAnnotml.putMeasurement('CD45 pos',0)
        isletAnnotml.putMeasurement('%CD45 pos',Double.NaN)
    }

    def ringAnnotml=annot.getMeasurementList()
    def ringml=ringObj.getMeasurementList()
    if (ringChildren){
        ringml.putMeasurement('CD45 neg',ringChildren.findAll{it.getPathClass()==null}.size())
        ringml.putMeasurement('CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size())
        ringml.putMeasurement('%CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()/ringChildren.size()*100)
        ringAnnotml.putMeasurement('CD45 neg',ringChildren.findAll{it.getPathClass()==null}.size())
        ringAnnotml.putMeasurement('CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size())
        ringAnnotml.putMeasurement('%CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()/ringChildren.size()*100)

        isletAnnotml.putMeasurement('Peri CD45 neg',ringChildren.findAll{it.getPathClass()==null}.size())
        isletAnnotml.putMeasurement('Per CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size())
        isletAnnotml.putMeasurement('Peri %CD45 pos',ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()/ringChildren.size()*100)


        tissuePos+=ringChildren.findAll{it.getPathClass()==getPathClass("CD45")}.size()
        tissueNeg+=ringChildren.findAll{it.getPathClass()==null}.size()
    } else {
        ringml.putMeasurement('CD45 neg',0)
        ringml.putMeasurement('CD45 pos',0)
        ringml.putMeasurement('%CD45 pos',Double.NaN)
        ringAnnotml.putMeasurement('CD45 neg',0)
        ringAnnotml.putMeasurement('CD45 pos',0)
        ringAnnotml.putMeasurement('%CD45 pos',Double.NaN)

        isletAnnotml.putMeasurement('Peri CD45 neg',0)
        isletAnnotml.putMeasurement('Peri CD45 pos',0)
        isletAnnotml.putMeasurement('Peri %CD45 pos',Double.NaN)
    }

    //isletAnnotml=innerIslet.getMeasurementList()
    dispDets<<ringObj
    dispDets<<isletObj
}

//total tissue measurements
//IMPORTANT: only islet-adjacent cells were detected
//Exocrine pancreas never measured
tissue.getMeasurementList().putMeasurement('CD45 pos',tissuePos)
tissue.getMeasurementList().putMeasurement('CD45 neg',tissueNeg)
tissue.getMeasurementList().putMeasurement('%CD45 pos',tissuePos/(tissuePos+tissueNeg))
fireHierarchyUpdate()


addObjects(dispDets)
resolveHierarchy()


/**Measuring Colocalization and Union**/
print('Gathering Positive Regions and colocalizing')
deleteExisting = false // Delete existing objects
createInverse = true //Change this if things end up in the wrong place

//gather positive stained areas from each slide
new File(affinepath).eachFile{ f->
    GatherObjects(deleteExisting, createInverse, f)
}

//not sure where the polylines come from, but they break things and need to be removed
removeObjects(getDetectionObjects().findAll{it.getROI() instanceof PolylineROI},true)

//for every pair of endocrine stains, find colocalized area and create a new detection
classListBase=['INS','GCG','IAPP','ProINS Dark','ProINS Total','PPY','SST','Chromogranin','GCG Rb']
for (int i=0; i<classListBase.size(); i++){
    for (int j=i+1; j<classListBase.size(); j++){
        addObjects(colocalize([classListBase[i],classListBase[j]]))
        //see below for colocalize function
    }
}

//total endocrine area
addObjects(unionAll(classListBase))

//find all existing classes of endocrine stains
Set classList = []
for (object in getAllObjects().findAll{it.isDetection()}) {
    classList << object.getPathClass()
}
classList.removeAll{['Islet','PeriIslet','CD45'].contains(it.toString())} //these don't count

//write the colocalized area and % area to each islet
print('Summary Measurements')
//getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Islet')}.each{
isletAnnotations.each{
    String isletName=it.getName()
    def det=isletDetections.find{it.getName()==isletName}
    def annotationArea = it.getROI().getArea()
    def children=getDetectionObjects().findAll{it.getName()==isletName}
    for (aClass in classList){
        if (aClass){
            def tiles = children.findAll{it.getPathClass() == aClass}
            double totalArea = 0

            for (def tile in tiles){
                totalArea += tile.getROI().getArea()
            }
            it.getMeasurementList().putMeasurement(aClass.toString()+" area um^2", totalArea*pixelSize*pixelSize)
            it.getMeasurementList().putMeasurement(aClass.toString()+" area %", totalArea/annotationArea*100)
            det.getMeasurementList().putMeasurement(aClass.toString()+" area um^2", totalArea*pixelSize*pixelSize)
            det.getMeasurementList().putMeasurement(aClass.toString()+" area %", totalArea/annotationArea*100)
        }
    }
}

//write the sums to the tissue region
double isletArea=isletDetections.collect{it.getROI().getArea()*pixelSize*pixelSize}.sum()
classList.each{aClass->
    if (aClass){
        double classTotal = isletAnnotations.collect{it.getMeasurementList().getMeasurementValue(aClass.toString()+" area um^2")}.sum()
        tissue.getMeasurementList().putMeasurement(aClass.toString()+" islet area um^2",classTotal)
        tissue.getMeasurementList().putMeasurement(aClass.toString()+" islet area %",classTotal/isletArea)
    }
}

print('Unionizing and IOU')

//create the Union (not intesersection) for each pair of endocrine stains
for (int i=0; i<classListBase.size(); i++){
    for (int j=i+1; j<classListBase.size(); j++){
        addObjects(unionAll([classListBase[i],classListBase[j]]))
    }
}

//get union class names
for (object in getAllObjects().findAll{it.isDetection()}) {
    classList << object.getPathClass()
}
def unionClasses=classList.findAll{it.toString().contains('Union')}

//write the IOU per islet
isletAnnotations.each{
    String isletName=it.getName()
    def det=isletDetections.find{it.getName()==isletName}
    //def annotationArea = it.getROI().getArea()
    def children=getDetectionObjects().findAll{it.getName()==isletName}
    for (aClass in unionClasses){
        if (aClass){
            def tiles = children.findAll{it.getPathClass() == aClass}
            double totalArea = 0

            for (def tile in tiles){
                totalArea += tile.getROI().getArea()
            }


            //Jaccard Index = Intersection / union
            String intName=aClass.toString().substring(10)
            def intersection=children.find{it.getPathClass().toString()==intName}
            if (intersection){
                it.getMeasurementList().putMeasurement(intName + " IOU %", intersection.getROI().getArea()/totalArea*100)
                det.getMeasurementList().putMeasurement(intName + " IOU %", intersection.getROI().getArea()/totalArea*100)
            } else {
                it.getMeasurementList().putMeasurement(intName + " IOU %", 0)
                det.getMeasurementList().putMeasurement(intName + " IOU %", 0)
            }
        }
    }
}

tissue.getMeasurementList().putMeasurement("Total islet area um^2",isletArea)
tissue.getMeasurementList().putMeasurement("Total islet area %",isletArea/(tissue.getROI().getArea()*pixelSize*pixelSize))

/**Islet Shapes**/
selectAnnotations()
addShapeMeasurements("AREA", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")

/**Write Islet, PeriIslet, and Tissue Measurements to file**/

annotPath = buildFilePath(PROJECT_BASE_DIR, 'Annotation Results')
mkdirs(annotPath)
annotOut=buildFilePath(annotPath,'AllOverlaps '+baseImageName+'.txt')
saveAnnotationMeasurements(annotOut)

getProjectEntry().saveImageData(getCurrentImageData())

print('Completely Finished')

/***********************
 Subfunctions:
 * transformObject
 * transformROI
 * DistributeObjects
 * GatherObjects
 * colocalize
 * unionAll
 * smoothPolygonRoi
 ***********************/

//transformObject, transformROI, DistributeObjects, GatherObjects taken from here: https://forum.image.sc/t/interactive-image-alignment/23745/9

PathObject transformObject(PathObject pathObject, AffineTransform transform) {
    //apply an affine matrix to an object
    // Create a new object with the converted ROI
    def roi = pathObject.getROI()
    def roi2 = transformROI(roi, transform)
    def newObject = null
    if (pathObject instanceof PathCellObject) {
        def nucleusROI = pathObject.getNucleusROI()
        if (nucleusROI == null)
            newObject = PathObjects.createCellObject(roi2, pathObject.getPathClass(), pathObject.getMeasurementList())
        else
            newObject = PathObjects.createCellObject(roi2, transformROI(nucleusROI, transform), pathObject.getPathClass(), pathObject.getMeasurementList())
    } else if (pathObject instanceof PathTileObject) {
        newObject = PathObjects.createTileObject(roi2, pathObject.getPathClass(), pathObject.getMeasurementList())
    } else if (pathObject instanceof PathDetectionObject) {
        newObject = PathObjects.createDetectionObject(roi2, pathObject.getPathClass(), pathObject.getMeasurementList())
        newObject.setName(pathObject.getName())
    } else {
        newObject = PathObjects.createAnnotationObject(roi2, pathObject.getPathClass(), pathObject.getMeasurementList())
        newObject.setName(pathObject.getName())
    }
    // Handle child objects
    if (pathObject.hasChildren()) {
        newObject.addPathObjects(pathObject.getChildObjects().collect({transformObject(it, transform)}))
    }
    return newObject
}

ROI transformROI(ROI roi, AffineTransform transform) {
    //apply and affine matrix to a ROI
    def shape = RoiTools.getShape(roi) // Should be able to use roi.getShape() - but there's currently a bug in it for rectangles/ellipses!
    shape2 = transform.createTransformedShape(shape)
    return RoiTools.getShapeROI(shape2, roi.getImagePlane(), 0.5)
}

def DistributeObjects(boolean deleteExisting, boolean createInverse, pathObjects, File f){
    //take the objects in the current image and move them (with affine transform) to the image with name f

    f.withObjectInputStream {
        def matrix = it.readObject()

        // Get the project & the requested image name
//        def project = getProject()
        def entry = project.getImageList().find {it.getImageName()+".aff" == f.getName()}
        if (entry == null) {
            print 'Could not find image with name ' + f.getName()
            return
        }
        def imageData = entry.readImageData()
        def otherHierarchy = imageData.getHierarchy()

        // Define the transformation matrix
        def transform = new AffineTransform(
                matrix[0], matrix[3], matrix[1],
                matrix[4], matrix[2], matrix[5]
        )
        if (createInverse)
            transform = transform.createInverse()

        if (deleteExisting)
            otherHierarchy.clearAll()

        def newObjects = []
        for (pathObject in pathObjects) {
            newObjects << transformObject(pathObject, transform)
        }
        otherHierarchy.addPathObjects(newObjects)
        otherHierarchy.getAnnotationObjects().each{it.setLocked(true)}
        entry.saveImageData(imageData)

        entry.saveImageData(imageData)
    }
}

def GatherObjects(boolean deleteExisting, boolean createInverse, File f){
    //grab the objects from image f and put them into the current image
    f.withObjectInputStream {
        matrix = it.readObject()

        // Get the project & the requested image name
        def project = getProject()
        def entry = project.getImageList().find {it.getImageName()+".aff" == f.getName()}
        if (entry == null) {
            print 'Could not find image with name ' + f.getName()
            return
        }

        def otherHierarchy = entry.readHierarchy()
        def pathObjects = otherHierarchy.getDetectionObjects()

        // Define the transformation matrix
        def transform = new AffineTransform(
                matrix[0], matrix[3], matrix[1],
                matrix[4], matrix[2], matrix[5]
        )
        if (createInverse)
            transform = transform.createInverse()

        if (deleteExisting)
            clearAllObjects()

        def newObjects = []
        for (pathObject in pathObjects) {
            newObjects << transformObject(pathObject, transform)
        }
        addObjects(newObjects)
    }
}

def colocalize(List<String> stains){
    //intersect two sets of detections based on their names
    //i.e. finds Ins_1 and GCG_1 and creates a detection that is their intersection; repeat for INS_2 and GCG_2, etc
    PrecisionModel PM = new PrecisionModel(PrecisionModel.FIXED)
    String newStainName=stains.join(': ')
    def islets = getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Islet')}
    def dets = getDetectionObjects()
    def newObjs=[]
    islets.each{ islet ->
        def matches = dets.findAll{it.getName().equals(islet.getName())}.findAll{stains.contains(it.getPathClass().toString())}
        if (matches.size()==stains.size()) {
            def geos = matches.collect { GeometryPrecisionReducer.reduce(it.getROI().getGeometry(), PM) }
            geos.eachWithIndex{ entry, int i ->
                if (i==0){
                    merged=entry
                } else {
                    merged=merged.intersection(entry)
                }
            }

            def mergedRoi=GeometryTools.geometryToROI(GeometryTools.homogenizeGeometryCollection(merged), islets[0].getROI().getImagePlane())
            if (mergedRoi.getArea()>0){
                def mergeddet = PathObjects.createDetectionObject(mergedRoi,getPathClass(newStainName))
                mergeddet.setName(islet.getName())
                newObjs << mergeddet
            }
        } else if (matches.size()>stains.size()) {
            print 'how are there too many???'
        }
    }
    return newObjs
}

def unionAll(List<String> stains){
    //as above, but create the union, not the intersection
    //i.e. finds Ins_1 and GCG_1 and creates a detection that is their union; repeat for INS_2 and GCG_2, etc
    PrecisionModel PM = new PrecisionModel(PrecisionModel.FIXED)
    String newStainName='Union of: '+stains.join(': ')
    def islets = getAnnotationObjects().findAll{it.getPathClass()==getPathClass('Islet')}
    def dets = getDetectionObjects()
    def newObjs=[]
    islets.each{ islet ->
        def matches = dets.findAll{it.getName().equals(islet.getName())}.findAll{stains.contains(it.getPathClass().toString())}
        if (matches.size()>1) {
            def geos = matches.collect { GeometryPrecisionReducer.reduce(it.getROI().getGeometry(), PM) }
            geos.eachWithIndex{ entry, int i ->
                if (i==0){
                    merged=entry
                } else {
                    merged=merged.union(entry)
                }
            }
            def mergedRoi=GeometryTools.geometryToROI(merged, islets[0].getROI().getImagePlane())
            def mergeddet = PathObjects.createDetectionObject(mergedRoi,getPathClass(newStainName))
            mergeddet.setName(islet.getName())
            newObjs << mergeddet
        }
    }
    return newObjs
}

private static PolygonRoi smoothPolygonRoi(PolygonRoi r) {
    //copied from here: https://github.com/qupath/qupath/blob/main/qupath-core-processing/src/main/java/qupath/imagej/detect/cells/WatershedCellDetection.java#L1141
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

/**Imports**/

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.CoordinateSequenceComparator
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFilter

import static qupath.lib.gui.scripting.QPEx.*
import qupath.opencv.ops.ImageOps
import qupath.ext.stardist.StarDist2D

import qupath.lib.classifiers.pixel.PixelClassifier
import qupath.lib.images.servers.ImageServer
import qupath.lib.images.servers.PixelCalibration
import qupath.lib.images.PathImage
import qupath.lib.images.ImageData
import qupath.lib.measurements.MeasurementList
import qupath.lib.regions.RegionRequest
import qupath.lib.regions.ImagePlane
import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathTileObject
import qupath.lib.objects.PathCellObject
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.RoiTools
import qupath.lib.roi.interfaces.ROI
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.PolygonROI
import qupath.lib.roi.PolylineROI
import qupath.lib.roi.ShapeSimplifier
import qupath.lib.analysis.images.SimpleImage
import qupath.lib.analysis.stats.RunningStatistics
import qupath.lib.analysis.stats.StatisticsHelper
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.color.StainVector

import qupath.opencv.tools.OpenCVTools
import qupath.opencv.ml.pixel.PixelClassifierTools
import qupath.opencv.ops.ImageOps
import qupath.opencv.features.DelaunayTriangulation

import qupath.imagej.detect.cells.ObjectMeasurements
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.processing.Watershed
import qupath.imagej.tools.IJTools
import qupath.imagej.tools.PixelImageIJ

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.util.GeometryCombiner
import org.locationtech.jts.operation.union.UnaryUnionOp
import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.locationtech.jts.geom.PrecisionModel

import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.TermCriteria
import org.bytedeco.opencv.global.opencv_video
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.indexer.Indexer

import ij.gui.PolygonRoi
import ij.gui.Roi
import ij.measure.Calibration
import ij.plugin.filter.EDM
import ij.process.ByteProcessor
import ij.process.ColorProcessor
import ij.process.FloatPolygon
import ij.process.FloatProcessor
import ij.process.ImageProcessor
import ij.process.ShortProcessor

import java.awt.Graphics2D
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.geom.AffineTransform

import javafx.scene.transform.Affine
import java.nio.charset.StandardCharsets