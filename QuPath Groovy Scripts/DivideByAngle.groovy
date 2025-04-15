import qupath.lib.objects.PathObjectTools
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs
import static qupath.lib.gui.scripting.QPEx.*
// divide cell by angle

//run any cell detection. Watershed exmaple here.
//runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImage":"ChS1-T3","requestedPixelSizeMicrons":0.24,"backgroundRadiusMicrons":8.0,"backgroundByReconstruction":true,"medianRadiusMicrons":0.0,"sigmaMicrons":0.5,"minAreaMicrons":20.0,"maxAreaMicrons":100.0,"threshold":2000.0,"watershedPostProcess":true,"cellExpansionMicrons":0.0,"includeNuclei":true,"smoothBoundaries":true,"makeMeasurements":false}')

//how thick is the membrane (pixels)
int boundary = 6

//user defines a single point that is the center of 1 cell
def pts = getAnnotationObjects().find{it.getROI().isPoint()}
def pt1 = pts.getROI().getAllPoints()[0]
def toRemove =[]
def toAdd=[]

//for every cell
def cells = getDetectionObjects()
cells.eachWithIndex{cell, idx->
    def cellroi = cell.getROI()
    //find the vector between the cell's centroid and the point
    double difX = pt1.getX() - cellroi.getCentroidX()
    double difY = pt1.getY() - cellroi.getCentroidY()
    double rotAng =Math.atan2(difY,difX);
    double hyp=Math.sqrt((difX**2)+(difY**2))

    //to create a 90 degree angle, subtract 45 and add 45
    def lines=[]

    //subtract 45 degrees, double the length of the hypotenuse just to ensure it reaches out of the cell
    double y2=Math.sin(rotAng-Math.toRadians(45))*hyp*2+cellroi.getCentroidY()
    double x2=Math.cos(rotAng-Math.toRadians(45))*hyp*2+cellroi.getCentroidX()
    def downroi = ROIs.createLineROI(cellroi.getCentroidX(),cellroi.getCentroidY(),x2, y2, ImagePlane.getDefaultPlane())
    lines << PathObjects.createAnnotationObject(downroi)

    //add 45 degrees, double the length of the hypotenuse just to ensure it reaches out of the cell
    double y3=Math.sin(rotAng+Math.toRadians(45))*hyp*2+cellroi.getCentroidY()
    double x3=Math.cos(rotAng+Math.toRadians(45))*hyp*2+cellroi.getCentroidX()
    def uproi = ROIs.createLineROI(cellroi.getCentroidX(),cellroi.getCentroidY(),x3, y3, ImagePlane.getDefaultPlane())
    lines << PathObjects.createAnnotationObject(uproi)

    // create a ring around the cell, thickness defined aboce
    def cellgeo =  cellroi.getGeometry()
    def ring = cellgeo.difference(cellgeo.buffer(-1*boundary))
    def ringroi = GeometryTools.geometryToROI(ring, ImagePlane.getDefaultPlane())
    def ringdet = PathObjects.createDetectionObject(ringroi,getPathClass('Membrane'))

    //split by lines
    def newObjs = PathObjectTools.splitAreasByLines([ringdet],lines)
    def parts = newObjs[ringdet]
    def areas = parts.collect{it.getROI().getArea()}
    def away = parts.find{it.getROI().getArea()==areas.max()}
    away.setName('Non Contact')
    def close = parts.find{it.getROI().getArea()!=areas.max()}
    close.setName('Contact')
    
    toAdd << away
    toAdd << close

    //convert cell to annotation
    def newCell = PathObjects.createAnnotationObject(cellroi,getPathClass('Tcell'))
    newCell.setName('Cell '+idx.toString())
    toAdd << newCell
    toRemove << cell

}

removeObjects(toRemove,true)
addObjects(toAdd)

resolveHierarchy()
//selectDetections()
//runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":0.12,"region":"ROI","tileSizeMicrons":25.0,"channel1":false,"channel2":false,"channel3":true,"channel4":false,"channel5":false,"doMean":true,"doStdDev":true,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickMin":NaN,"haralickMax":NaN,"haralickDistance":1,"haralickBins":32}')
