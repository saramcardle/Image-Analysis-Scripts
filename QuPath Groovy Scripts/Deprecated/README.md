# A collection of hopefully useful groovy scripts to be run in QuPath. Files are organized by version they were designed for.

## QuPath 0.1.2
	* Cropper - exports tifs of annotated regions. Works with RGB or Fluorescences, 2D or 3D. (Out-of-date, there are functions to do this in QuPath 0.2.3)
	* DetectionAreaSummary - For each annotation, finds the child cells and sums the area of all cells in each class. Adds this as annotation measurements.  
	* Tutorials and Exercises - Data, instructions, and example scripts from the LJI QuPath 2019 workshop
	* QBI 2019 Example Scripts - Scripts used to QC a histology workflow, as described at QBI 2019.
	
## QuPath 0.2.0 m8
	* RepresentativeImageFinder - Finds small tiles with the average H score in the larger image to objectively determine a "representative image" for publication.
	
## QuPath 0.2.3
	* Alignment_diffPixel- Aligns two images (from repeated scanning of a slide or sequential sections) and transfers objects from one to another. Can be used for either intensity- or area-based alignment, the images can have different pixel sizes, and can transfer annotations or detections. 
	* AlignmentWithbUnWarpJ - an ImageJ script (not groovy) to do elastic alignment of two images. This one is not fully automated, please see [the forum:] (https://forum.image.sc/t/automatically-align-and-rotate-images/30596/21?u=smcardle) for instructions
