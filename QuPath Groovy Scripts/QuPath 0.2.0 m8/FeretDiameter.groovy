import qupath.imagej.tools.IJTools
import qupath.lib.gui.scripting.QPEx

def imageData = QPEx.getCurrentImageData()
def cal=imageData.getServer().getPixelCalibration()
double pixelSize=cal.getAveragedPixelSize()

cells=QPEx.getDetectionObjects()
cells.each{
    def ml = it.getMeasurementList()
    def roi = it.getROI()
    def roiIJ = IJTools.convertToIJRoi(roi, 0, 0, 1)
    def max = roiIJ.getFeretValues()[0] //max feret diameter
    def min = roiIJ.getFeretValues()[2] //min feret diameter
    ml.putMeasurement('Max Feret', max*pixelSize)
    ml.putMeasurement('Min Feret', min*pixelSize)
}