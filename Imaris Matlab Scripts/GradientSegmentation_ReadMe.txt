Segmentation algorithm for microscopy data. 
Segments cells based on 2 channels simulataneously, based on the gradients in both channels. Good for images where there are adjacent double-positive cells that are difficult to separate with a single-color intensity thresholds. Does not assume that cells are round, so is especially well adapted for oddly shaped cells, such as macrophages.

Largely based on: "Unsupervised color image segmentation using a dynamic color gradient thresholding algorithm", Balasubramanian, et al., Human Vision and Electronic Imaging, 2008. Please see the pdf file for a general overview of how the algorithm works. 

Algorithm was adapted to fluorescent imaging data. Major changes:
1) Performs 3D segmentation
2) Uses 2 channels only (written for GFP/YFP, though any colors will work)
3) Assumes you have set all background pixels to 0. Fluorescent images typically have a high (>50%) number of background pixels and it is inefficient to process them. The script will still work if you have not done this, but it will be slower. 
4) Additional region merging based on some reasonable assumptions about cells. No final region is allowed to be smaller than a user-defined size, and no cell can be "landlocked" (fully contained within other cells). 
5) Reads data from an .ims file, and outputs calculated cell surfaces to Imaris for further processing/tracking.
6) Multiple parameters are tweakable

Dependencies:
1) Imaris Reader- Library by Pete Beemiller than can be found on the Imaris XTensions page
2) Imaris 7 (newer versions do not work with Imaris Reader)
3) Matlab Parallel Processing Toolbox
4) adjacentRegionsGraph - function by Steve Eddins that can be found on the Matlab File Exchange

This script is still a work in progress. Version 2 will be more efficient and better commented. It is currently very slow, even with parallel computing. It takes ~12-24 hours to process a large data set (512x512x50x30 XYZT). It is highly recommended that you first adjust parameters based on a small cropped region. Running it block by block and then looking at each stage of the output with imshow() is helpful for tuning the parameters. 

Written by Sara McArdle of the La Jolla Institute Microscopy Core, 2018.