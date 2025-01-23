//Macro for find intersections between insulin+ and IL6+ regions (and glucagon+ IL6+)
//Written by Zbigniew Mikulski and Sara McArdle, LJI Microscopy Core. 

//set thresholds for 3 channels
InsThresh=4500;
GlucThresh=5000;
IL6Thresh=5000;

//find image title
title=getTitle();

//separate channels
run("Split Channels");

//define channel names
//check that these match your image
InsImage="C2-"+title;
GlucImage="C3-"+title;
IL6Image="C4-"+title;

//determine which parameters to measure for each image
run("Set Measurements...", "area min redirect=None decimal=1");

//find insulin positive regions
selectImage(InsImage);  
setThreshold(InsThresh, 65535); 
run("Convert to Mask"); //convert to black and white based on threshold
//measure maximum value
run("Measure"); 
white=getResult("Max",0);

//if there is at least 1 white pixel (above threshold), turn it into an ROI
if (white>0){
	run("Create Selection");
	roiManager("add");
	roiManager("select",0);
	roiManager("rename","Insulin"); //name that ROI "Insulin"
}
run("Clear Results");

nTotal=roiManager("count"); //nTotal will be 1 if there was an insulin region, 0 if not

//repeat same process for glucagon
selectImage(GlucImage);
setThreshold(GlucThresh, 65535);  
run("Convert to Mask");
run("Measure");
white=getResult("Max",0);
if (white>0){
	run("Create Selection");
	roiManager("add");
	roiManager("select",nTotal); //use nTotal because we do not know if there was an Insulin region
	roiManager("rename","Glucagon"); //name that 1 ROI "Glucagon"
}
run("Clear Results");

nTotal=roiManager("count");

//Repeat for IL6
selectImage(IL6Image);
setThreshold(IL6Thresh, 65535);  
run("Convert to Mask");
run("Measure");
white=getResult("Max",0);
if (white>0){
	run("Create Selection");
	roiManager("add");
	roiManager("select",nTotal);
	roiManager("rename","IL6"); //name that 1 ROI "IL6"
}
run("Clear Results");
nTotal=roiManager("count");

//Find intersecting regions
//Insulin and IL6

//find pixels that exist in both the insulin image and the IL6 image
imageCalculator("AND create", InsImage,IL6Image);
selectWindow("Result of C2-"+title);
rename("InsIl6");

run("Measure");
white=getResult("Max",0);
if (white>0){
	run("Create Selection");
	roiManager("add");
	roiManager("select",nTotal);
	roiManager("rename","Ins_IL6"); 
}

run("Clear Results");
nTotal=roiManager("count");

//Repeat for Glucagon and IL6
imageCalculator("AND create", GlucImage,IL6Image);
selectWindow("Result of C3-"+title);
rename("GlucIl6");

run("Measure");
white=getResult("Max",0);
if (white>0){
	run("Create Selection");
	roiManager("add");
	roiManager("select",nTotal);
	roiManager("rename","Gluc_IL6");
}
run("Clear Results");

if (roiManager("count")>0){
//send ImageJ ROIs to QuPath as detection objects
run("From ROI Manager");
roiManager("show all without labels");
run("Send Overlay to QuPath", "choose_object_type=Detection include_measurements");
}

//clear ROIs and Results table to be ready for next image
roiManager("select",Array.getSequence(roiManager("count")));
roiManager("delete");
run("Clear Results");
