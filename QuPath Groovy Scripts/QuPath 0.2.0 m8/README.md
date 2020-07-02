import qupath.lib.gui.ml.PixelClassifierTools
import qupath.lib.gui.scripting.QPEx

runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 170,  "requestedPixelSizeMicrons": 10.0,  "minAreaMicrons": 100000.0,  "maxHoleAreaMicrons": 1000000.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": true,  "singleAnnotation": true}');

def imageData = getCurrentImageData()
def cal=imageData.getServer().getPixelCalibration()
double pixelSize=cal.getAveragedPixelSize()


def highClassifier = project.getPixelClassifiers().get('6382 GCGhigh1')
PixelClassifierTools.createDetectionsFromPixelClassifier(imageData, highClassifier, getAnnotationObjects(), 20, 20, true, false)

print(QPEx.getProjectEntry().getImageName())
if (QPEx.getProjectEntry().getImageName().contains("_ProINS")){
        getDetectionObjects().each{
            it.setPathClass(getPathClass("ProINS"))
        }
}

if (QPEx.getProjectEntry().getImageName().contains("_INS")){
        getDetectionObjects().each{
            it.setPathClass(getPathClass("Insulin"))
        }
}

if (QPEx.getProjectEntry().getImageName().contains("_CD45")){
        getDetectionObjects().each{
            it.setPathClass(getPathClass("CD45"))
        }
}

if (QPEx.getProjectEntry().getImageName().contains("_IAPP")){
        getDetectionObjects().each{
            it.setPathClass(getPathClass("IAPP"))
        }
}
if (QPEx.getProjectEntry().getImageName().contains("_GCG")){
        getDetectionObjects().each{
            it.setPathClass(getPathClass("Glucagon"))
        }
}
