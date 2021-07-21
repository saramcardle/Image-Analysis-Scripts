Program that the LJI Microscopy core uses to recharge for Axioscan usage time.

SlideScannerDurationCSV reads the metadata of .czi files looking for the acquisition date and duration. This field will only be populated in files from the AxioscanZ.1 (and maybe CellExplorer?), but not the Zeiss confocals. It writes the information into a readable .csv that is useful for billing. You will be prompted to select the input files and choose an output file. 
Version 2.1 has a GUI that allows you to select mutliple groups of files with multiple outputs simultaneously so that you can process files from multiple users without additional input. It also contains SOME error checking and more efficient memory usage to attempt to avoid the Java Heap Space error.  It will remove all commas from a .czi file name. 

The raw matlab script (Duration.mlapp) can be edited. Requires Matlab, at least 2019b with AppDesigner. 

The compiled executable, SlideScannerDurationCSV2.1.exe, does NOT require Matlab (only the free Matlab runtime which you will be prompted to download if necessary). *Only works for Windows.* 
	
Please contact smcardle@lji.org with any questions. Please see here for the origin of the X all the Y meme: http://hyperboleandahalf.blogspot.com/2010/06/this-is-why-ill-never-be-adult.html
	
	

