//After runnning SegmentMicroglia.groovy
//This calculates a variety of statistics (excluding sholl and skeleton metrics, that's in a separate file)

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.objects.PathDetectionObject

//grab detection objects
def micros = getDetectionObjects().findAll{it.getPathClass().toString().contains('MicrogliaShape')}
def somas = getDetectionObjects().findAll{it.getPathClass().toString().contains('Soma')}
double pixelSize=getCurrentImageData().getServer().getPixelCalibration().getAveragedPixelSize()

//clean up unnecessary measurments
def toRemove = []
somas.each{
    toRemove.addAll(it.getMeasurementList().getMeasurementNames().findAll{it.contains('Distance to detection with Stroma')})
}
removeMeasurements(PathDetectionObject, toRemove.toSet() as String[])

//Basic QuPath Shape Measurements
selectDetections();
addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER")

//Metrics involving the convex hull
getDetectionObjects().each{
    def hull=it.getROI().getGeometry().convexHull()
    double hullArea=hull.getArea()*pixelSize*pixelSize
    it.measurements['Hull Area']=hullArea
    double hullPeri=hull.getLength()*pixelSize
    it.measurements['Hull length']=hullPeri
    it.measurements['Convexity']=hullPeri/it.getROI().getLength()*pixelSize

}

//Basic Intensity Measurements for 3 channels
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":0.325,"region":"ROI","tileSizeMicrons":25.0,"channel1":false,"channel2":true,"channel3":true,"doMean":true,"doStdDev":false,"doMinMax":false,"doHaralick":false,"haralickMin":NaN,"haralickMax":NaN,"haralickDistance":1,"haralickBins":32}')

//CD68 Area Pct
selectDetections();
addPixelClassifierMeasurements("CD68_1000", "CD68_1000") //find area per cell that has CD68 (>1000 pixel value)
getDetectionObjects().each{
 it.measurements['CD68%']=it.measurements['CD68_1000: CD68 area µm^2'] /it.measurements['Area µm^2']*100 //turn Area into %
}

//Nucleus-Cell Centroid Offset
micros.each{micro->
    def nuc = somas.find{it.getName()==micro.getName()}
    def microROI = micro.getROI()
    def nucROI = nuc.getROI()

    double offset=Math.sqrt(((microROI.getCentroidX()-nucROI.getCentroidX())**2)+((microROI.getCentroidY()-nucROI.getCentroidY())**2))*pixelSize
    micro.measurements['Centroid Offset']=offset
}

//Delaunay Measurements

//We're going to delaunay triangulation twice- once with cell centroids and a large threshold,
// once with nuclear centroids and a small distance threshold.
// To separate these, we'll use the Hierarachy

resolveHierarchy() //puts all detections as children of the hippocampus

//remove the micros from the hierarchical relationship, leaving only nuclei
removeObjects(micros,true)
addObjects(micros)

//Delaunay metrics on nuclei only
selectObjectsByClassification("Region*");
runPlugin('qupath.opencv.features.DelaunayClusteringPlugin', '{"distanceThresholdMicrons":25.0,"limitByClass":true,"addClusterMeasurements":false}')

//repeat the process, but excluding the nuclei
resolveHierarchy()
removeObjects(somas,true)
addObjects(somas)

selectObjectsByClassification("Region*");
runPlugin('qupath.opencv.features.DelaunayClusteringPlugin', '{"distanceThresholdMicrons":250.0,"limitByClass":true,"addClusterMeasurements":false}')

resolveHierarchy() //put it all back

//Write all Nucleus measurements to it's associated microglia. This makes the final csv export easier to read
micros.each {m-> //loop over microglia
    def soma = somas.findAll{it.getName()==m.getName()} //find its nucleus
    if (soma.size()==0) { //if it didn't find one, tell me
        print(m.getName())
    } else if (soma.size()>1) { //if it found more than one, tell me
        print(m.getName())
    } else { //if we found exactly 1 nucleus with the same name, as expected
        soma = soma[0]
        soma.getMeasurementList().getMeasurementNames().each{name->
            m.measurements['Soma '+name]=soma.measurements[name] //write every measurement with Soma appended
        }
    }
}

