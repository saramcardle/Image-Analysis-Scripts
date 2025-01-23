/*Finds small tiles with the average H score in the larger image
To objectively determine a "representative image" for publication
Written by Sara McArdle of the LJI Microscopy Core Facility, 2019.
 */
import qupath.lib.gui.panels.PathAnnotationPanel
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.gui.models.ObservableMeasurementTableData

//get basic image information
def imageData = getCurrentImageData()

//find annotations and measurements you just made
def detections = getDetectionObjects()
def tissue = getAnnotationObjects()

//get total tissue H score out of the observable data table
def totalob = new ObservableMeasurementTableData();
totalob.setImageData(imageData, tissue);
double tissueH = totalob.getNumericValue(tissue[0], "H-score")

//divide image into tiles
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons": 300.0,  "trimToROI": true,  "makeAnnotations": true,  "removeParentAnnotation": true}');

//the Tiler plugin deletes detections. Replace them (faster than recalculating)
addObjects(detections)

//get H score and number of detections of each tile individually
def tiles = getAnnotationObjects()
def ob = new ObservableMeasurementTableData();
ob.setImageData(imageData, tiles);

tileH=[] //list of H scores per tile
numDet=[] //list of cell numbers in each tile
tiles.each{
    tileH << ob.getNumericValue(it, "H-score")
    numDet << ob.getNumericValue(it, "Num Detections")
}

//remove ""atypical" tiles based on the number of cells
//"typical" tiles should be within 2 standard deviations of the mean
double meanDet=numDet.sum()/numDet.size()
double stdevDet=Math.sqrt(numDet.collect{ Math.pow(it-meanDet,2)}.sum()/(numDet.size()-1))
//calculate thresholds
double numLow=meanDet-(stdevDet*2)
double numHigh=meanDet+(stdevDet*2)
//make sure there's always at least some cells in a region, even if mean-2*std<0
if (numLow<10){
    numLow=10
}

//find tiles with extreme cell numbers and remove them
def toRemove=tiles.findAll{ob.getNumericValue(it,"Num Detections")<numLow||ob.getNumericValue(it,"Num Detections")>numHigh}
removeObjects(toRemove,true)

//find remaining tiles
tilesRem=getAnnotationObjects()
def obRem = new ObservableMeasurementTableData();
obRem.setImageData(imageData, tilesRem);

//get a list of the H scores of the remaining tiles
tileH=[]
tilesRem.each{
    tileH << obRem.getNumericValue(it, "H-score")
}

//find the lowest 3 and highest 3 H-scores
sortedH=tileH.toSorted()
minH=sortedH[0..2]
maxH=sortedH[-3..-1]

//find the H scores closest to average
//these have the smallest absolutele difference from average
def diff=tileH.collect{(it-tissueH).abs()}
sortedDiff=diff.toSorted()
diffMin=sortedDiff[0..2]

//label tiles as either low, average, or high H score with an increasing index number
//Remove all other tiles
remove=[]
mini=1
maxi=1
avgi=1
tilesRem.eachWithIndex{tile,idx ->
    if (minH.contains(tileH[idx])){
        minName=String.format('Low %d',mini)
        tile.setName(minName)
        mini=mini+1
    } else if (maxH.contains(tileH[idx])){
        maxName=String.format('High %d',maxi)
        tile.setName(maxName)
        maxi=maxi+1
    } else if (diffMin.contains(diff[idx])) {
        avgName=String.format('Average %d',avgi)
        tile.setName(avgName)
        avgi=avgi+1
    } else {
        remove << tile
    }
}

//remove the tiles
removeObjects(remove,true)

//lock the remaining tiles to prevent accidentally moving them while scrolling
tileRem2=getAnnotationObjects()
tileRem2.each{
    it.setLocked(true)
}

//replace the whole tissue outline
addObjects(tissue)

print('Done!')






