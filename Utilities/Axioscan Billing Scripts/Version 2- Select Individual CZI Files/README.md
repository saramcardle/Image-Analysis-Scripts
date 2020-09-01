Program that the LJI Microscopy core uses to recharge for Axioscan time.

SlideScannerDurationCSV reads the metadata of .czi files looking for the acquisition date and duration. This field will only be populated in files from the AxioscanZ.1 (and maybe CellExplorer?), but not the Zeiss confocals. It writes the information into a readable .csv that is useful for billing. You will be prompted to select the input files and choose an output file. 

	-The raw matlab script (SlideScannerDurationCSV.m) can be edited. Requires Matlab. 

	-The compiled executable, SlideScannerDurationCSV.exe, does NOT require Matlab (only the free Matlab runtime which you will be prompted to download if you don't have it). But, the path of the text file cannot be changed. *Only works for Windows.* 
	
Please contact smcardle@lji.org with any questions. 
	
	
	
