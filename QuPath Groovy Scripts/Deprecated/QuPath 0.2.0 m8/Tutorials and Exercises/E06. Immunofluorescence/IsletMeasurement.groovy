import qupath.imagej.plugins.ImageJMacroRunner

/* Macro to measure the overlap between a chemokine and insulin or glucagon in a pancreatic islet
Uses annotations of islets (made through pixel classifier + IsletProcessing.groovy)
Sends each region to ImageJ and then runs the macro "IsletIntersection".
ImageJ sends back ROIs correspond to insulin+, glucagon+, insulin+chemokine+, and glucagon+chemokine+ regions.
Final Result- each annotation will have 2 new measurements:
1) the fraction of the Insulin+ area that is also chemokine+,
2) the fraction of the Glucagon+ area that is also chemokine+
The thresholds for insulin, glucagon, and chemokine should be set in the ImageJ macro
 */

//get basic image info
def imageData = getCurrentImageData()

// Create a macro runner so we can check what the parameter list contains
def params = new ImageJMacroRunner(getQuPath()).getParameterList()
params.getParameters().get('downsampleFactor').setValue(1.0 as double)

//find macro file (.ijm) and format it properly
macrolocation=buildFilePath(PROJECT_BASE_DIR,'IsletIntersection.ijm');
def macro = new File(macrolocation).text

//process each islet annotation
annotations=getAnnotationObjects();
annotations.eachWithIndex {islet, idx ->
    //send the small ROI to ImageJ and run the macro
    ImageJMacroRunner.runMacro(params, imageData, null, islet, macro)

    //find the regions that ImageJ returns by name
    regions=getDetectionObjects()
    Ins=regions.find{it.getName().equals("Insulin")} //total insulin area
    Gluc=regions.find{it.getDisplayedName().equals("Glucagon")} //total glucagon area
    InsIl6=regions.find{it.getDisplayedName().equals("Ins_IL6")} //Insulin+Chemokine+ double positive region
    GlucIl6=regions.find{it.getDisplayedName().equals("Gluc_IL6")} //Glucagon+Chemokine+ double positive region
    
    //calculate area fraction
    if (Ins && InsIl6){
        InsIl6Frac=InsIl6.getROI().getArea()/Ins.getROI().getArea()
    } else {
        InsIl6Frac=0 //if either Ins or InsIl6 is missing, set overlap to 0
    }
        
    //calculate the area fraction
    if (Gluc && GlucIl6){
        GlucIl6Frac=GlucIl6.getROI().getArea()/Gluc.getROI().getArea()
    } else {
        GlucIl6Frac=0 //if either Gluc or GlucIl6 is missing, set overlap to 0
    }    

    //put the calculated area fractions in the measurement list
    islet.getMeasurementList().putMeasurement('Insulin Il6 Overlap',InsIl6Frac*100)
    islet.getMeasurementList().putMeasurement('Glucagon Il6 Overlap',GlucIl6Frac*100)

    //remove detections so that next round there is only 1 "Insulin", etc
    removeObjects(regions,false)

    //print counter just so the user knows it's still processing
    print(idx)
}

//update gui with new information and report that script is finished
fireHierarchyUpdate()
print("Done!")