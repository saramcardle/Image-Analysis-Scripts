#@ File (label="Select Folder of Images", style="directory") outerDir
#@ String (choices={"2x","4x","10x", "20x","40x","60x","100x"}, style="radioButtonHorizontal") ObjectiveMagnification

setBatchMode(true);
folders=getFileList(outerDir);

if (ObjectiveMagnification=="2x"){
	pixelSize=3.77442;
} else if (ObjectiveMagnification=="4x"){
	pixelSize=1.88721;
} else if (ObjectiveMagnification=="10x"){
	pixelSize=0.75488;
} else if (ObjectiveMagnification=="20x"){
	pixelSize=0.37744;
} else if (ObjectiveMagnification=="40x"){
	pixelSize=0.18872;
} else if (ObjectiveMagnification=="60x"){
	pixelSize=0.12581;
} else if (ObjectiveMagnification=="100x"){
	pixelSize=0.07549;
}

checkStructure(outerDir);

function checkStructure(dir){
	folders = getFileList(dir);
	
	hasSub=false;
	for (i = 0; i < lengthOf(folders); i++) {
		if (File.isDirectory(dir+File.separator+folders[i])){
			hasSub=true;
		}
	}
	
	if (hasSub) { //assume outerDir is folder of folders (most common)
		for (i = 0; i < lengthOf(folders); i++) {
			if (File.isDirectory(dir+File.separator+folders[i])){
				checkStructure(dir+File.separator+folders[i]);
			}
		}
	} else {
		print(dir);
		imgfolder(dir,pixelSize);
		run("Close All");
	}
}

//for (i = 0; i < lengthOf(folders); i++) {
	//if (File.isDirectory(outerDir + File.separator+folders[i])){
		//imgfolder(outerDir + File.separator+folders[i],pixelSize);
		//run("Close All");
	//} else {
		//REORGANIZE CODE TO ALLOW FOR 1 IMAGE
//	}
//}

function imgfolder(filedir,pixelSize){
	//check for more recursion!!!
	files = getFileList(filedir);
	Array.print(files);
	imgs=newArray;
	imgnum=0;
	for (k=0; k<lengthOf(files); k++){
		if (files[k].matches(".*_CH[0-9].tif")){
			//open(pdir+File.separator+files[k]);
			if (endsWith(filedir,File.separator)){
				open(filedir+files[k]);
			} else {
				open(filedir+File.separator+files[k]);
			}
		//	open(filedir+files[k]);
			title=getTitle();
			getDimensions(width, height, channels, slices, frames);
			
			if (channels>1){
				run("Split Channels");
			
				for (c=1; c<4; c++){		
					selectWindow('C'+c+'-'+title);
					run("Measure");
					intVal=getResult("Max",nResults-1);
					if (intVal==0){
						run("Close");
					} else {
						imgs[imgnum]='C'+c+'-'+title;
						imgnum=imgnum+1;
						break //this might be wrong
						//I need to check how keyence exports cyan
					}
				}
			} else {
								bd = bitDepth();	
				if (bd==24){
					run("Split Channels");
					colors=newArray(" (red)"," (green)"," (blue)");
					
					for (c=0; c<3; c++){		
						selectWindow(title + colors[c]);
						run("Measure");
						intVal=getResult("Max",nResults-1);
						if (intVal==0){
							run("Close");
						} else {
							imgs[imgnum]=title + colors[c];
							imgnum=imgnum+1;
							break 
						}
					}
				} else { 
				imgs[imgnum]=getTitle();
				imgnum++;
				}
			}
		}	
	}
	//merge
	merging="";
	Array.print(imgs);

	for (img=0; img<imgnum; img++){
		merging=merging+"c"+(img+1)+"=["+imgs[img]+"] ";
	}
	merging=merging+"create";
	run("Merge Channels...", merging);
		
	newTitle=getTitle();
	
	if (height==1920){
		pixelSize = pixelSize*1;
	} else if (height==960){
		pixelSize=pixelSize*2;
	} else if (height == 480){
		pixelSize=pixelSize*4;
	} else {
		pixelSize=NaN;
	}
						
	run("Set Scale...", "distance=1 known="+pixelSize+" unit=um");
			
	//RENAME THIS!!!!
	folderTitle=File.getName(filedir);
	print(folderTitle);
	saveAs(".tif",outerDir+File.separator+folderTitle+'_'+imgnum+"chs.tif");
	
if (isOpen("Results")){
	selectWindow("Results");
	run("Close");
}
}

