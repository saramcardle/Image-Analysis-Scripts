# Microglia segmentation and analysis
Associated with 'Unmasking Early Microglial Remodeling in an Alzheimer’s Disease Mouse Model', [PMID TBD]

## Description of images
The project has 21 whole slide images of mouse hippocampus. 3 channels- Hoechst, CD68, and Iba1. The images were acquired as Z-stacks on a widefield slidescanner, and then deconvolved, Z-projected, and stitched. The pixel size is 0.1722 um. (Hopefully the raw image files will be placed on Zenodo or a similar database. If/when that happens, it will be linked here.)

## Software Requirements
1. QuPath (https://qupath.github.io/)
2. Cellpose and the QuPath Cellpose Extension (https://github.com/BIOP/qupath-extension-cellpose)
   - GPU acceleration is helpful but not essential. You can also replace the Cellpose step with Stardist for a much easier install and similar results. 
3. Fiji (https://fiji.sc/) with the Simple Neurite Tracer plugin (https://imagej.net/plugins/snt/)
4. The StatProcessing.m script requires Matlab, but this is not essential to the project and can be easily reproduced in many other languages

## Steps for reproducing the analysis
1. Start a QuPath project with the processed images
2. In each image, manually annotate the hippocampus, skipping the granule cell nuclei layer in the dentate gyrus. Assign this to the `Region*` class
3. Add the classifiers folder into the QuPath project. These classifiers are only applicable to these images and cannot be extrapolated onto other projects. They are here only as a reference.
4. Run `SegmentMicroglia.groovy` for project
   - This can take hours for a large file!
   - The steps are described inside the script, but briefly:
      1. Cellpose to detect nuclei
      2. Trained pixel classifier to detect Iba1
      3. Using parallel processing for speed, use binary morphological processing to merge disconnected regions and associate nuclei with cells
      4. Clean up cell overlaps that arose from step 3
      5. Give each cell-nucleus pair the same name for alter reference
      6. Remove artifacts
   - The result will be microglia outlines (class `MicrogliaShape`) and nuclear outlines (class `Soma`)
5. Run `BasicMeasurements.groovy`  for project to measure basic shape metrics, intensities, and spatial relationships
6. Run `Sholl_exportBinary.groovy` to export binary images of each microglia with its nucleus overlaid
   - This will create a new folder 'MicroShapes' inside your QuPath project. Inside that will be 1 subfolder per image. Inside that will be individual tiffs for each cell.
7. In Fiji that already has the SNT plugin, run `Sholl_ImageJFunctions.py` to measure the Sholl and skeleton metrics
   - In the file chooser, pick the created folder 'MicroShapes'
   - This will create a .csv file inside each folder named for the image
8. Grab all of the .csv files and put them into one folder!
9. In QuPath, run `Sholl_importStats.groovy`
   - Save your work *before* running this, because it interacts directly with the .qpdata file!
   - Just once, *not* Run For Project
   - Select the folder that contains all the resultant .csv files
   - This will assign Sholl and skeletonization metrics to each microglia
   - Reload the project data on the open image to view the results!
10. To identify the reactive microglia, run `ReactiveCells.groovy`. This requires the basic measurements and the Sholl measurements to have already been calculated.
11. In QuPath, draw a line separating the CA1 region from the rest of the hippocampus. Use the `Split annotations by line` to separate them. Give each half a relevant name. Then, resolve the hierarchy. 
12. (Optional) Further statistical analysis in Matlab. 
    1. Export the detection measurements
    2. In Excel or similar, delete all of the Soma objects. Also delete any columns that are now empty because they only applied to Soma objects or that have redundant/meaningless information. (First 4 columns should be: Image, Name, Classification, Parent, all the others are cell metrics)       
    3. Load this file into Matlab as a table. Check the column variable types, sometimes is defaults to Categorical instead of number for measurements that have highly repetitive values. 
    4. Run `StatProcessing.m`
      - This will produce a variable `medians` that has the median, per slide, of every metric
      - It will also separate the cells into deciles based on Branching Index (or any other variable you chose) and produce 2 tables with the CD68 and Iba values per decile that are formatted to be be easy to copy-paste into GraphPad Prism
