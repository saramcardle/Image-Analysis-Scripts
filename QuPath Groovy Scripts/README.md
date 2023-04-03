# A collection of hopefully useful groovy scripts to be run in QuPath. Files are organized by version they were designed for.

## QuPath 0.1.2
	* Cropper - exports tifs of annotated regions. Works with RGB or Fluorescences, 2D or 3D. (Out-of-date, there are functions to do this in QuPath 0.2.3)
	* DetectionAreaSummary - For each annotation, finds the child cells and sums the area of all cells in each class. Adds this as annotation measurements.  
	* Tutorials and Exercises - Data, instructions, and example scripts from the LJI QuPath 2019 workshop
	* QBI 2019 Example Scripts - Scripts used to QC a histology workflow, as described at QBI 2019.
	
## QuPath 0.2.0 m8
	* FeretDiameter - Uses IJTools to calculate the max and mean [Feret Diameter](https://en.wikipedia.org/wiki/Feret_diameter) of cells
	* RareCellFetcher - Function to help you annotate single, rare cells to improve a classifier. Please see [the forum:] (https://forum.image.sc/t/rarecellfetcher-a-tool-for-annotating-rare-cells-in-qupath/33654)
	* RepresentativeImageFinder - Finds small tiles with the average H score in the larger image to objectively determine a "representative image" for publication.
	
## QuPath 0.2.3
	* Alignment_diffPixel- Aligns two images (from repeated scanning of a slide or sequential sections) and transfers objects from one to another. Can be used for either intensity- or area-based alignment, the images can have different pixel sizes, and can transfer annotations or detections. 
	* AlignmentWithbUnWarpJ - an ImageJ script (not groovy) to do elastic alignment of two images. This one is not fully automated, please see [the forum:] (https://forum.image.sc/t/automatically-align-and-rotate-images/30596/21?u=smcardle) for instructions


## QuPath 0.3.0-rc1
	* RareCellFetcher - Function to help you annotate single, rare cells to improve a classifier. Please see [the forum:] (https://forum.image.sc/t/rarecellfetcher-a-tool-for-annotating-rare-cells-in-qupath/33654)


## QuPath 0.4.x
	* Pixelwise H-score - Implementation of Ram, et al., 2021, "Pixelwise H-score: A novel digital image analysis-based metric to quantify membrane biomarker expression from immunohistochemistry images" (https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0245638). Decently fast, because no objects are created. 

	
