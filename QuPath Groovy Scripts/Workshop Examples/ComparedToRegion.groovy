

selectCells();
//run the intensity features plugin at 50 um.
//you will need to change this
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":0.325,"region":"Circular Tiles","tileSizeMicrons":50.0,"channel1":true,"channel2":true,"channel3":true,"doMean":true,"doStdDev":false,"doMinMax":false,"doMedian":false,"doHaralick":false,"haralickMin":NaN,"haralickMax":NaN,"haralickDistance":1,"haralickBins":32}')

//Put Channel Name Here
String name = 'WGA AF647'
def cells=getCellObjects()
cells.each{
    double subtract=it.measurements['Cell: ' +name + ' mean']-it.measurements['ROI: 0.33 µm per pixel: '+name+': Mean']
    double divide=it.measurements['Cell: ' +name + ' mean']/it.measurements['ROI: 0.33 µm per pixel: '+name+': Mean']
    it.measurements['Subtract Background']=subtract
    it.measurements['Divide Background']=divide
}
