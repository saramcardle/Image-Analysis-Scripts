transformFolder1="D:\\Dirk\\METAL SCANS\\6209_02_metals2\\'


if (roiManager("count")>0){
	roiManager("deselect");
	roiManager("delete");
}

selectWindow("Image1");
run("Create Mask");
selectWindow("Mask");
rename("Image1 Mask");

selectWindow("Image2");
run("Create Mask");
selectWindow("Mask");
rename("Image2 Mask");

selectWindow("Objects");
run("Create Mask");
selectWindow("Mask");
rename("Objects Mask");

selectWindow("Image2 Mask");
getDimensions(width, height, channels, slices, frames);

selectWindow("Image1 Mask");
run("Canvas Size...", "width="+width+" height="+height+" position=Top-Left zero");
run("bUnwarpJ", "source_image=[Image1 Mask] target_image=[Image2 Mask] registration=Accurate image_subsample_factor=0 initial_deformation=[Very Coarse] final_deformation=[Very Fine] divergence_weight=0 curl_weight=0 landmark_weight=0 image_weight=1 consistency_weight=10 stop_threshold=0.01 save_transformations save_direct_transformation=["+transformFolder1+"Image1 Mask_direct_transf.txt] save_inverse_transformation=["+transformFolder1+"Image2 Mask_inverse_transf.txt]");
selectWindow("Objects Mask");
run("Canvas Size...", "width="+width+" height="+height+" position=Top-Left zero");
call("bunwarpj.bUnwarpJ_.loadElasticTransform", transformFolder1+"Image1 Mask_direct_transf.txt", "Image2 Mask", "Objects Mask");
print('Transformed!');

setThreshold(15.9000, 1000000000000000000000000000000.0000);
 //set this threshold
setOption("BlackBackground", true);
run("Convert to Mask");
run("Analyze Particles...", "add");

roiManager("Deselect");
roiManager("Combine");
run("Send ROI to QuPath");
