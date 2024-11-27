Series of files for analysis of MICSSS pancreas images, in conjunction with [PMID TBD].

Brief File Descriptions:
- MICSSSAlignment.groovy is used in a QuPath project to calculate affine transforms of all images in a project against a base image. 
- PancreasIsletAnalysis.groovy is the meat of the MICSSS Pancreas project. It segments the islets based on 9 images, calculates many features across images, and combines all data into a single .txt file
- 'Classifiers' contains all of the classifiers used in the QuPath analysis. These are here for reference and are unlikely to be applicable for other projects with new data. 
- Pseudofluor.groovy generates a fluorescence-like image from a series of H-DAB MICSSS images for visualization
- DataCombinerMICSSSS.m takes the series of .txt files output from QuPath and generates a single combined data file for import into CytoMap (requires Matlab)
- RipleyTissueBoundary.m performs Ripley H-score analysis of islets, accounting for the pancreas tissue boundary (requires Matlab)
- MICSSStoFCS.m converts the full data table into a .fcs file for analysis in FlowJo, including renaming the variables in a more consistent way. 

All groovy scripts were a collaboration between Sara McArdle, LJI, and Mike Nelson, UW-Madison. We relied heavily on the QuPath source code (referenced inside each file where relevant). 

If you find bugs, please contact Sara at smcardle@lji.org
