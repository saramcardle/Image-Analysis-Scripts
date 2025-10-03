import qupath.imagej.gui.ImageJMacroRunner
import qupath.lib.objects.PathObjects

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.roi.ROIs

//This file exports every cell as a binary image with the nucleus set as an overlay
//Related files perform the Sholl and Skeleton analysis in ImageJ and then import the results back to QuPath

//This whole process is because we need the SNT plugin. At the time of writing, QuPath was packaged with ImageJ1
// and the SNT plugin requires IJ2. 
//There has been progress in this area, so maybe in the future, this step can be simplified. 

//keep true for first run. Turn to false if the boxes already exist from a previous, failed run
boolean makerects=true

//ensure the hippocampus annotation is the Region* class. It should be, but this is just in case it's not.


//send to imagej
def micros=getDetectionObjects().findAll{it.getPathClass()==getPathClass('MicrogliaShape')}
def somas=getDetectionObjects().findAll{it.getPathClass().toString().contains('Soma')}

//put the word Soma in the nuclei object names so that we can differentiate them later
somas.each{
    it.setName(it.getName()+' Soma')
}

//set export location
def imgname=getProjectEntry().getImageName()
def folder = imgname //[0..(imgname.indexOf('_')-1)]
def outfolder=buildFilePath(PROJECT_BASE_DIR,"MicroShapes",folder)
mkdirs(outfolder)
forMacro=outfolder.replace('\\','/')

//make bounding boxes
if (makerects) {
   getAnnotationObjects().each {
       it.setPathClass(getPathClass('Region*')) 
    } 
    def rects =[]
    micros.each {micro->
        double x1=micro.getROI().getBoundsX()
        double x2=micro.getROI().getBoundsWidth()
        double y1=micro.getROI().getBoundsY()
        double y2=micro.getROI().getBoundsHeight()

        def boundary = ROIs.createRectangleROI(x1, y1, x2, y2, micro.getROI().getImagePlane()) //make a bounding box rectangle

        def rect=PathObjects.createAnnotationObject(boundary) //turn it into an annotation object
        rect.setName(micro.getName()+ ' Rect')
        rects << rect
    }

addObjects(rects) //add all rectangles to the project
}

//we're going to use ImageJ to create a binary image with an overlay
params = new ImageJMacroRunner(getQuPath()).getParameterList()

// Change the value of a parameter, using the JSON to identify the key
params.getParameters().get('downsampleFactor').setValue(1) //full resolution
params.getParameters().get('sendROI').setValue(false) //don't set box
params.getParameters().get('sendOverlay').setValue(true) //all internal detections are 'overlays'
params.getParameters().get('getOverlay').setValue(false) //don't return anything
params.getParameters().get('getROI').setValue(false)
params.getParameters().get('clearObjects').setValue(false)

//macro written in .ijm language
macro="""
title=getTitle();
name = substring(title,0,title.length-5); //will be the name of the microglia
print(name); 
run("To ROI Manager");

//a bounding box may contain outlines of multiple cells
//searching by name lets us focus on the correct one

for (i =roiManager("count")-1; i>-1; i=i-1){
	if (endsWith(RoiManager.getName(i) ,name)){ //find the overlay that is the correct microglia
		roiManager("Select",i);
		run("Create Mask"); //turn it into a binary image
		roiManager("Select",i);		
		roiManager("Delete"); //delete the ROI
		
	} else if (!endsWith(RoiManager.getName(i),name+' Soma')){ //OTHER THAN the matching nucleus
		roiManager("Select",i);
		roiManager("Delete"); //delete all other ROIs
	}
    //this leaves only the nucleus roi
}

selectWindow("Mask");
roiManager("Deselect");
roiManager("Show None");
roiManager("Select",0); //select the remaining ROI

saveAs("Tiff", "${forMacro}/"+name+".tif"); //it will save the nuc shape as an overlay inside the Tiff
run("Close All");

"""

//run the macro for every bounding box
rects = getAnnotationObjects().findAll{it.getPathClass()==null}
rects.each{
   ImageJMacroRunner.runMacro(params, getCurrentImageData(), null, it, macro)
}

print('Binary Images Created at: '+outfolder.toString())
removeObjects(rects,true)