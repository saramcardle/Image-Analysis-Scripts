/* Script for running Quality Control on histology sections.
Outputs regions that are good quality tissue, whitespace, or contains artifacts (folds, dust, out-of-focus) and a text file containing percentages of each class.
Run this for the entire project. It generates superpixels, calculates features, classifies, exports data as text.
Make sure to change the location of the classifier (line 41)!

Afterwards, run "Merge exported annotation results" by Pete Bankhead to collect all results into a single file.

Written by Sara McArdle of the LJI Microscopy Core, 2019. 
*/

//Classifier only tested on H&E stains, but should be broadly generalizable with retraining
setImageType('BRIGHTFIELD_H_E');

//Change deconvolution stain values for each batch of staining
setColorDeconvolutionStains('{"Name" : "H&E default", "Stain 1" : "Hematoxylin", "Values 1" : "0.49432 0.84595 0.20005 ", "Stain 2" : "Eosin", "Values 2" : "0.15067 0.97431 0.16742 ", "Background" : " 201 202 203 "}');

//Detect whole tissue and divide into superpixels
runPlugin('qupath.imagej.detect.tissue.SimpleTissueDetection2', '{"threshold": 203,  "requestedPixelSizeMicrons": 20.0,  "minAreaMicrons": 10000.0,  "maxHoleAreaMicrons": 1000000.0,  "darkBackground": false,  "smoothImage": true,  "medianCleanup": true,  "dilateBoundaries": false,  "smoothCoordinates": true,  "excludeOnBoundary": false,  "singleAnnotation": true}');
selectAnnotations();
runPlugin('qupath.imagej.superpixels.DoGSuperpixelsPlugin', '{"downsampleFactor": 8.0,  "sigmaMicrons": 10.0,  "minThreshold": 10.0,  "maxThreshold": 230.0,  "noiseThreshold": 1.0}');

//Measure shape, intensity, and texture features of each superpixel, and calculate smoothed features
selectDetections()
runPlugin('qupath.lib.plugins.objects.ShapeFeaturesPlugin', '{"area": true,  "perimeter": true,  "circularity": true,  "useMicrons": true}');
selectDetections();
runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons": 0.2,  "region": "ROI",  "tileSizeMicrons": 25.0,  "colorOD": true,  "colorStain1": true,  "colorStain2": truue,  "colorStain3": false,  "colorRed": false,  "colorGreen": false,  "colorBlue": false,  "colorHue": false,  "colorSaturation": false,  "colorBrightness": false,  "doMean": true,  "doStdDev": true,  "doMinMax": true,  "doMedian": true,  "doHaralick": true,  "haralickDistance": 1,  "haralickBins": 32}');
selectDetections();
runPlugin('qupath.lib.algorithms.CoherenceFeaturePlugin', '{"magnification": 5.0,  "stainChoice": "Optical density",  "tileSizeMicrons": 25.0,  "includeStats": true,  "doCircular": false}');
selectDetections();
runPlugin('qupath.lib.algorithms.LocalBinaryPatternsPlugin', '{"magnification": 5.0,  "stainChoice": "Optical density",  "tileSizeMicrons": 25.0,  "includeStats": true,  "doCircular": false}');
selectAnnotations();
runPlugin('qupath.lib.plugins.objects.SmoothFeaturesPlugin', '{"fwhmMicrons": 50.0,  "smoothWithinClasses": false,  "useLegacyNames": false}');

//Run Classifier
//Check Path
def classFolder=buildFilePath(PROJECT_BASE_DIR,'classifiers')
runClassifier(buildFilePath(classFolder, "ScanningQC.qpclassifier"))

//Summarize results for each detection
allSuperPixs = getDetectionObjects()
classList=[getPathClass("Good"),getPathClass("Whitespace"),getPathClass("Fold"),getPathClass("Out Of Focus"),getPathClass("Dust")] //adjust if you add more classes

Areas = new double[classList.size()]
def summary = getAnnotationObjects().findAll {it.getDisplayedName().toString().contains('PathAnnotationObject') == true} //detection results will be summarized within the tissue annotation

//Sum the areas of each detection per class
for (c=0; c<classList.size(); c++){
    classDet=allSuperPixs.findAll{it.getPathClass()==classList[c]}
    double classArea=0
    classDet.each{
        classArea+=it.getMeasurementList().getMeasurementValue("ROI Shape: Area µm^2")
    }
    Areas[c]=classArea
}
summary[0].getMeasurementList().putMeasurement("Total Area", Areas.sum())

//Calculate percentages
for (c=0; c<classList.size(); c++){
    measurementName = classList[c].toString() + " Area Percent"
    summary[0].getMeasurementList().putMeasurement(measurementName, Areas[c] / Areas.sum() * 100)
}
summary[0].getMeasurementList().putMeasurement('Total Artifact Area Percent', Areas[2..4].sum() / Areas.sum() * 100)

fireHierarchyUpdate() //Update display

//export results as a txt file
def Imgname = getProjectEntry().getImageName() + '.txt'
def path = buildFilePath(PROJECT_BASE_DIR, 'QC results')
mkdirs(path)
path = buildFilePath(path, Imgname)
saveAnnotationMeasurements(path, 'Good Area Percent', 'Total Artifact Area Percent', 'Whitespace Area Percent', 'Fold Area Percent', 'Out Of Focus Area Percent','Dust Area Percent')
print 'Results exported to ' + path


