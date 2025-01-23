#@ File (label="Select Folder of Images", style="directory") outerDir
#@ String (choices={"2x","4x","10x", "20x","40x","60x","100x"}, style="radioButtonHorizontal") ObjectiveMagnification

setBatchMode(true);
folders=getFileList(outerDir);

run("Set Measurements...", "area mean min limit display redirect=None decimal=3");

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

for (i = 0; i < lengthOf(folders); i++) {
	if (folders[i].matches(".*W[0-9]{3}/")){
		//w folder
		wfolder(outerDir + File.separator+folders[i],pixelSize);
	}
}

function wfolder(dir,pixelSize){
	subfolders = getFileList(dir);
	wellNum="";
	for (j=0; j<lengthOf(subfolders); j++){
		if (subfolders[j].matches(".*_[A-Z][0-9]{2}")){
			wellNum=subfolders[j];
			wellNum=substring(wellNum,1);
			print(wellNum);
		}
	}
	
	if (lengthOf(wellNum)<2){
		wparts=split(dir,File.separator);
		wname=wparts[lengthOf(wparts)-1];
		wellNum=substring(wname,0,lengthOf(wname)-1);
	}
	

	for (j=0; j<lengthOf(subfolders); j++){
		if (subfolders[j].matches(".*P[0-9]{5}/")){
			merged=pfolder(dir+ subfolders[j]);

			selectWindow(merged);
			getDimensions(width, height, channels, slices, frames);
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

			saveAs(".tif",dir+wellNum+"_"+substring(subfolders[j],0,lengthOf(subfolders[j])-1)+".tif");
		} 
		
		run("Close All");
	}
}

function pfolder(pdir){
	files = getFileList(pdir);
	imgs=newArray;
	imgnum=0;
	for (k=0; k<lengthOf(files); k++){
		if (files[k].matches(".*W[0-9]{3}_P[0-9]{5}_CH[0-9].tif")){
			//open(pdir+File.separator+files[k]);
			open(pdir+files[k]);
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
	return newTitle;
}
if (isOpen("Results")){
	selectWindow("Results");
	run("Close");
}
