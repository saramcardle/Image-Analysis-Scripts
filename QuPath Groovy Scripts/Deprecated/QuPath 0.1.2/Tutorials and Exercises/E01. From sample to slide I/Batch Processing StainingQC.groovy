/*
Set correct stain vectors values based on your own samples.

Script to detect changes in staining quality in histology stains over time
Stain one slide from a control sample with each batch of staining
Add each new slide to the project at then run this script.
Output the results to a text file. Results from multiple files can be combined with "Merge exported annotation results" by Pete Bankhead

See full-sentence description at https://drive.google.com/open?id=1HWUCzr6E2jI100d0XUChNYtLvFUjjRvu

Written by Sara McArdle of the LJI Microscopy Core, 2019
*/

//1 Set Staining Type (H&E, DAB, or Other)
setImageType('BRIGHTFIELD_H_E');

//2 Find color vector values  by copying the results of running "Estimate Stain Vectors" on a good quality sample
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.606 0.679 0.415 ", "Stain 2" : "Eosin", "Values 2" : "0.252 0.947 0.197 ", "Background" : " 209 208 205 "}');

//3 Outline tissue 
//Maximum allowed hole area (maxHoleAreaMicrons) decreased from the default to remove more whitespace so that it does not affect the tissue average intensity
runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 190,  "requestedPixelSizeMicrons": 20.0,  "minAreaMicrons": 1000000.0,  "maxHoleAreaMicrons": 10000.0,  "darkBackground": false,  "smoothImage": false,  "medianCleanup": false,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": false}');

//4 Select the outline you just created
selectAnnotations()

//5 Measure the intensity
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 2.0,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": true,  "colorStain2": true,  "colorStain3": true,  "colorRed": true,  "colorGreen": true,  "colorBlue": true,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": false,  "doMinMax": false,  "doMedian": false,  "doHaralick": false,  "haralickDistance": 1,  "haralickBins": 32}');

//6 Renaming the file to something more convenient. This was specific for our file name system, and should be altered to best suit you
def name = getProjectEntry().getImageName()+'.txt'

//7 Defining where to keep the files in the project 
def folder = buildFilePath(PROJECT_BASE_DIR, 'intensity QC')

//8
mkdirs(folder)

//9
def path = buildFilePath(folder, name)

//10 
saveAnnotationMeasurements(path)

//11 And we are done!
print 'Results exported to ' +  path