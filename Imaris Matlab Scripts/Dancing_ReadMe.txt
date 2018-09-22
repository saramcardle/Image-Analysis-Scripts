Dancing.m calculates shape and movement statistics for surface objects in Imaris.
Designed for analysis of macrophage motion. 
Finds standard metrics (Volume, Extent, Solidity, Axes lengths, ellipticity)
Also calculates new metrics:
1) Various "Dancing on the Spot" metrics- How much a shape has changed between 2 time points. Based on which pixels are occupied.
Dancing = pixels occupied newly in timepoint 2 / total pixels occupied in timepoints 1 or 2
Change = pixels occupied only in timespoints 1 or 2 (not both) / total pixels occupied in timepoints 1 or 2
Each of them are calculated with raw data, and when the centroid of timepoint 2 has been moved to the centroid of timepoint 1 (called AdjustedDancing or AdjustChange)

2) Outside of sphere- how many protrusion does the cell have outside an imagined sphere of the same volume and centroid. This is a measure of how "dendritic" the shape looks. The angle, volume, and length of each protrusion is measured. It can differentiate 
a) very round cells (high sphicity, low protrusion number and volume) 
b) cells that are shaped more like endothelial cells (elongated, with 1 or 2 protrusions of high volume) 
c) cells shaped like dendritic cells (multiple protrusions of small volume). 
The protrusions are tracked overtime to monitor shape changes.  

The output is a structure with all of the calculated statisitics and pixel masks. It is automatically saved. This file has statistics for each cell and averages for each track in a structure. 

Written by Sara McArdle of the La Jolla Institute Microscopy Core, 2017. For questions, please contact smcardle@lji.org. 
