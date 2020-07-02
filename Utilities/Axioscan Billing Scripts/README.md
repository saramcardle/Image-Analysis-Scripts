Collection of files that the LJI Microscopy core uses to recharge for Axioscan time.


1) ReadCZIMetadata.bat looks inside the folder where the .bat file is located for .czi files. It searches the metadata for the acquisition date and duration. This field will only be populated in files from the AxioscanZ.1 (and maybe CellExplorer?), but not the Zeiss confocals. It writes this data into a text file called "AxioScanZ1_usage.txt". 
2) ConvertToCSV takes that text file and puts it into an easier to read and work with CSV. It assumes the text file is located at: D:\SCANS\AxioScanZ1_usage.txt. Based on the fill file path of the czi file, it attempts to guess the username (useful for billing). There are 2 versions of this:
	
	-The raw Matlab script, ConvertToCSV.m, can be edited to change the location of the file or change how the script parses the username. Requires Matlab.
	
	-The compiled executable, ConvertToCSV_windowsInstaller.exe, does NOT require Matlab (only the free Matlab runtime which you will be prompted to download if you don't have it). But, the path of the text file cannot be changed. Only works for windows. 
	
Please contact smcardle@lji.org with any questions. 
	
	
	
