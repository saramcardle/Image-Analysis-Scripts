/* Function to export cropped areas from a larger image
Works with RGB brightfield or Fluorescence, 2D or 3D
Select your regions with annotations (any shape will do)
Do NOT have tissue detection on!!!
***This DOES NOT have protections to prevent exporting images that are too large, so do not attempt to export the entire tissue region***
This script produces a full resolution version of the cropped regions as TIFFs to a subfolder in the original file location
if FocusSlice is set to true, it will also use the imageJ plugin "find focus slice" to attempt to find the best focus slice in each channel
(To use this, make sure you have the Find_Focused_Slice.jar plugin in the ImageJ folder, and you've set up the plugin path.)
Written by Sara McArdle of the La Jolla Institute Microscopy Core Facility, 2018
For questions, please contact smcardle@lji.org
 */
import qupath.lib.regions.RegionRequest
import qupath.lib.scripting.QPEx
import qupath.imagej.gui.IJExtension
import qupath.imagej.images.servers.ImagePlusServer
import qupath.imagej.images.servers.ImagePlusServerBuilder
import ij.plugin.HyperStackConverter
import ij.io.FileSaver
import ij.IJ
import ij.WindowManager
import ij.ImagePlus
import ij.plugin.Concatenator
import ij.plugin.ChannelSplitter
import ij.ImageStack



boolean FocusSlice = false  //set to true to attempt to find the best slice. Must have ImageJ plugins set up with  "Find_Focus_Slices" installed
double downsample = 1 //1 = full resolution. >1 = downsampled.

//get image information
def imageData = QPEx.getCurrentImageData()
def server = imageData.getServer()
ImagePlusServer serverIP = ImagePlusServerBuilder.ensureImagePlusWholeSlideServer(server)
// Output directory for storing the tiles
filePath=getCurrentImageData().getServer().getPath()
String fileFolder = filePath.substring(0,filePath.lastIndexOf(File.separator))
def pathOutput = QPEx.buildFilePath(fileFolder, server.getShortServerName()+'_ROIs')
QPEx.mkdirs(pathOutput)

//get manually selected annotations
annots=QPEx.getAnnotationObjects()
//organize from left to right
annots.sort{it.getROI().getBoundsX()}

//number of annotations determines the number of leading zeros we will need later
decimals=Math.ceil(Math.log10(annots.size()+1))

//find if fluorescent or brightfield
imgtype=(QPEx.getCurrentImageData().imageType.toString())

//for every annotation
annots.eachWithIndex{roi,i->
    int minX = roi.getROI().getBoundsX()
    int minY = roi.getROI().getBoundsY()
    int width = roi.getROI().getBoundsWidth()
    int height = roi.getROI().getBoundsHeight()

    //must process each z slice individually
    for (int z = 0; z < server.nZSlices(); z++) {
        //create region request
        RegionRequest request = RegionRequest.createInstance(server.getPath(), downsample, minX, minY, width, height, z, 0)
        ImagePlus IPImage = serverIP.readImagePlusRegion(request).getImage(false) //send that region to ImageJ
        if (z.equals(0)) {
            combined = IPImage
        } else {
            combined = Concatenator.run(combined, IPImage) //add new region to the stack
        }
    }

    if (imgtype.equals("Fluorescence")) {
        regionData = HyperStackConverter.toHyperStack(combined, server.nChannels(), server.nZSlices(), 1) //convert combined CZ stack into hyperstack
    } else {
        regionData=combined //brightfield images stay as a regular stack, do not separate RGB colors
    }
    def name = String[]

    //label each region in a human readable way, from left to right. Rename ROIs to match file names
    if (decimals==4){
        name = String.format('%s_Region_%04d.tif', server.getShortServerName(), i+1)
        String roiName=String.format('Region %04d',i+1)
        roi.setName(roiName)
    } else if (decimals == 3){
        name = String.format('%s_Region_%03d.tif', server.getShortServerName(), i+1)
        String roiName=String.format('Region %03d',i+1)
        roi.setName(roiName)
    } else if (decimals == 2){
        name = String.format('%s_Region_%02d.tif', server.getShortServerName(), i+1)
        String roiName=String.format('Region %02d',i+1)
        roi.setName(roiName)
    } else {
        name = String.format('%s_Region_%d.tif', server.getShortServerName(), i+1)
        String roiName=String.format('Region %d',i+1)
        roi.setName(roiName)
    }

    def fileOutput = new File(pathOutput, name)
    print(fileOutput) //display where the file will be saved
    //Write full stack
    if ((server.nZSlices()>1) || (imgtype.equals("Fluorescence") & (server.nChannels()>1))){ //multiple Z slices or multiple fluorescent channels
        new FileSaver(regionData).saveAsTiffStack(fileOutput.toString()) //save as a tiff stack
    } else {
        new FileSaver(regionData).saveAsTiff(fileOutput.toString()) //if 2D, 1 channel, save as a standard tiff (not stack)
    }

    if (server.nZSlices()<2) { //only use focusing for Z stacks
        FocusSlice = false
    }

    if (FocusSlice) {
        IJExtension.getImageJInstance()
        title = regionData.getTitle()

        //use ImageJ FindFocusSlice plugin to  write just the best section for each channel
        if (imgtype.equals("Fluorescence") & server.nChannels()>1) { //for multichannel IF images
            chs = server.nChannels()
            channels = ChannelSplitter.split(regionData) //find focus for each channel separate
            ImageStack ImgStack = new ImageStack(regionData.getWidth(), regionData.getHeight())

            channels.eachWithIndex { it, int ch ->
                IJ.run(it, "Find focused slices", "select=100 variance=0.000 edge") //select just the single best slice, with an edge filter
                windowname = "Focused slices of C" + (ch + 1).toString() + "-" + title + "_100.0%"
                ImgStack.addSlice(WindowManager.getImage(windowname).getProcessor()) //add that channel to the stack
            }

            ImagePlus ImgStackPlus = new ImagePlus("Focused", ImgStack) //convert ImageStack to ImagePlus

            Focused = HyperStackConverter.toHyperStack(ImgStackPlus, chs, 1, 1, "default", "Composite") //convert Z stack to C stack
            nameparts = name.split(/\./)

            def FocusOutput = new File(pathOutput, (nameparts[0] + "_Focused.tif")) //save this in the same place as above, with Focused in its name
            new FileSaver(Focused).saveAsTiffStack(FocusOutput.toString())
            print(FocusOutput)
        } else { //for 1 channel IF or brightfield images
            IJ.run(regionData, "Find focused slices", "select=100 variance=0.000 edge")
            //run Find Focused Slices plug in
            windowname = "Focused slices of " + title + "_100.0%"
            Focused = WindowManager.getImage(windowname)
            nameparts = name.split(/\./)
            def FocusOutput = new File(pathOutput, (nameparts[0] + "_Focused.tif"))
            new FileSaver(Focused).saveAsTiff(FocusOutput.toString()) //save just as Tiff, not stack
            print(FocusOutput)
        }

        WindowManager.closeAllWindows()
    }
}

fireHierarchyUpdate()
print 'Done!'