How to run a Zen OAD Macro:

1) Download the file.
2) Find your Zen macro folder. It will be approximately here, but it will depend on your exact computer's set up (user accounts, etc): Documents\Carl Zeiss\ZEN\Documents\Macros\
3) Open Zen Blue (any version EXCEPT lite)
4) If you found the correct folder, the macro will appear under Tools> Customize Application > Macros >User Macros
5) Drag the macro to the right toolbar window. Then, a little button will appear in one of your toolbars that calls the macro.


File Descriptions:

-RemoveScenes: 	Takes a multiscene image and copies the data into a single scene. This is designed for Axioscan data, especially when a TMA is inappropriately read as a series of independent samples. Works with birghtfield or fluorescence, but 1 Z, 1 time only! See here for images: https://forum.image.sc/t/importin-czi-tma-map-into-qupath/42224/18

-SlideScanner Flattening Macro: Whole workflow designed to remove artifacts caused by slides not laying perfectly flat in the slidescanner. Requires multiple python and Zen setting files (all found in subfolder, with instructions). See here for an example: https://link.springer.com/article/10.1007/s00125-021-05619-9

-AiryBatch: When doing Airyscan processing in Zen on a batch of files, it forces you to pick one filter strength for all channels. This will allow you to perform Airyscan processing on a folder of raw images with different Wiener filter strength for each channel. All .czi files in the folder will be processed with the same settings.  
