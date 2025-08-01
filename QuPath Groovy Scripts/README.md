# QuPath Groovy Scripts

A collection of hopefully useful groovy scripts to be run in QuPath. 

  - Pixelwise Hscore - Implementation of Ram, et al., 2021, "Pixelwise H-score: A novel digital image analysis-based metric to quantify membrane biomarker expression from immunohistochemistry images" (https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0245638). Decently fast, because no objects are created.  
  - FluorescentChannelNames - Tool for visualizing the channel names on fluorescence images. Updates as you change the visible channels, colors, and names. Written by Pete Bankhead.  
  - RareCellFetcher - Function to help you annotate single, rare cells to improve a classifier. Please see [the forum:] (https://forum.image.sc/t/rarecellfetcher-a-tool-for-annotating-rare-cells-in-qupath/33654) UPDATED to deal with ignored classes! [QP 0.5.1]
  - FeretDiameter - Uses IJTools to calculate the max and mean [Feret Diameter](https://en.wikipedia.org/wiki/Feret_diameter) of cells
  - CellClassPct - Calculates the total percentage of cells inside each annotation that are positive for any relevant marker, regardless of other class names. For example, it can show you the total CD3%, whether the cells are positive or negative for CD4, CD8, etc. Uncomment some lines to change to area instead of cell frequency. 
  - DivideByAngle - finds the region of a cell closest to a user-defined point and splits the membrane compartment into regions that are potentially in contact with a second object vs far from it. "Contact" is defined as the 90 degrees closest to the vector between the cells. 
 - ExportForPaper - Makes exporting images for a figure easier. Select a region. For that ROI, this takes your current display settings, exports each channel individually as grayscale, and then exports the overlay as RGB. Also removes the annoying yellow boundary that shows the selected rectangle.  

## Workshop Examples
These are from the 2023 or 2025 QuPath Training Course at LJI. They are designed to be examples of what you can do with groovy scripting and some common QuPath scripting methods more than stand-alone functions. 	

  - Compared to Region - An example script showing how to create bespoke measurements to feed into a classifier. Measures the background intensity in a circle around each cell and then calculates the difference (subtraction) and ratio (division) of the cell intensity and background 
  - B_Helper_Cyto - An example script for applying object classifiers only to certain objects to reduce the total number of potential classes
  - RegionNameAndExport - Simultaneously export multiple ROIs with unique names