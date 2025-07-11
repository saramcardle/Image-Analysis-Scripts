# Series of files for analysis of MICSSS pancreas images, in conjunction with [PMID TBD].

Brief File Descriptions:
- MICSSSAlignment.groovy is used in a QuPath project to calculate affine transforms of all images in a project against a base image. 
- PancreasIsletAnalysis.groovy is the meat of the MICSSS Pancreas project. It segments the islets based on 9 images, calculates many features across images, and combines all data into a single .txt file
- 'Classifiers' contains all of the classifiers used in the QuPath analysis. These are here for reference and are unlikely to be applicable for other projects with new data. 
- Pseudofluor.groovy generates a fluorescence-like image from a series of H-DAB MICSSS images for visualization
- DataCombinerMICSSSS.m takes the series of .txt files output from QuPath and generates a single combined data file for import into CytoMap (requires Matlab).
- RipleyTissueBoundary.m performs Ripley H-score analysis of islets, accounting for the pancreas tissue boundary (requires Matlab)
- Jaccard.m calculates the average jaccard indices across groups. 

All groovy scripts were a collaboration between Sara McArdle, LJI, and Mike Nelson, UW-Madison. We relied heavily on the QuPath source code (referenced inside each file where relevant). 

If you find bugs, please contact Sara at smcardle@lji.org

# Description of PancreasIsletAnalysis.groovy
We use the CD45 image as the "base" image because it was scanned last and displays all accumulated tissue damage. Run this script on the CD45 image. This script will perform these steps:

## Tissue detection
1. Generate a tissue annotation (using a pre-trained 'TissueDetection' classifier, in the pixel_classifiers subfolder)
2. Distribute that annotation to all other images (using the pre-calculated Affine matrices from MICSSSAlignment.groovy)

## Coarse islet detection
1. On 6 hormone images (CHGA, INS, ProINS, GCG, SST, PPY), segment objects based on a low-resolution classifier. These 6 were chosed because they completely define the islet outline. 
2. Gather all of those objects from the 6 images onto the base image and then merge overlapping objects
3. Distribute these merged objects onto the 8 hormone images (deleting the existing objects). Measure DAB and Hematoxylin features in each (including CD45).
4. Once again gather the potential islets on the Base image and colate all measured features. 
5. Run an object classifer to remove artifacts (Islet vs NotIslet in object_classifiers). Also, remove any islets that are very close (<10um) from the tissue boundary because these were so often incorrectly segmented. 
6. Give the islets names.
7. Convert the islets to QuPath annotations and distribute them to all images 

## Refine hormone detection
1. Within each islet in each image, run a high-resolution pixel classifier to find the precise hormone boundaries. For the ProINS image, run 2 classifiers- for dim DAB and dark DAB. 
2. Name each newly created hormone object by the islet name and the stain.

## CD45 measurements
1. On only the CD45 image, expand every islet by 10 um to mark the "peri-islet" region
2. Within the islet + peri-islet region, use Stardist to segment cells
3. Use an object classifier (NotACell2) to remove artifacts
4. Use another object classiifer (CD45_selected_v3) to mark CD45+ leukocytes. 
5. Add the number of islet and peri-islet CD45+ and CD45- cells to the islet annotation measurement table.

## Endocrine area calculations
1. Gather all of the hormone objects created above to the Base image. 
2. Calculate the positive staining area of each hormone in each islet. Write this to the islet annotation measurement table.
3. For each pair of hormones (56 pairs), use the Jaca JTS suite to calculate the intersection and union of the objects. Write these as new detections. Also calculate IOU. 
4. Calculate the total endocrine area by union-izing all endocrine stains. 
5. Write all of these to the islet annotation measurement tables. 
6. Measure shape features using built-in QuPath tools.
7. Export the islet measurements as a .txt file with the measurements of each islet taking 1 row. 
