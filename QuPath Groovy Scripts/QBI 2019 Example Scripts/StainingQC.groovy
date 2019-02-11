/*
Script to detect changes in staining quality in histology stains over time
Stain one slide from a control sample with each batch of staining
Add each new slide to the project at then run this script
Output the results to a text file. Results from multiple files can be combined with "Merge exported annotation results" by Pete Bankhead

Written by Sara McArdle of the LJI Microscopy Core, 2019
*/

//Set Staining Type (H&E, DAB, or Other)
setImageType('BRIGHTFIELD_H_E');

//Find color vector values  by copying the results of running "Estimate Stain Vectors" on a good quality sample
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.61921 0.76391 0.18172 ", "Stain 2" : "Eosin", "Values 2" : "0.07601 0.99272 0.09341 ", "Background" : " 216 216 216 "}');

//Outline tissue and measure intensity
//Maximum allowed hole area (maxHoleAreaMicrons) decreased from the default to remove more whitespace so that it does not affect the tissue average intensity
runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 190,  "requestedPixelSizeMicrons": 20.0,  "minAreaMicrons": 10000.0,  "maxHoleAreaMicrons": 10000.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": true}');
selectAnnotations()
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 2.0,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": true,  "colorStain2": true,  "colorStain3": true,  "colorRed": true,  "colorGreen": true,  "colorBlue": true,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');

//Renaming the file to something more convenient. This was specific for our file name system, and should be altered to best suit you
def name = getProjectEntry().getImageName()+'.txt'
def folder = buildFilePath(PROJECT_BASE_DIR, 'intensity QC')
mkdirs(folder)
def path = buildFilePath(folder, name)
saveAnnotationMeasurements(path)
print 'Results exported to ' +  path


