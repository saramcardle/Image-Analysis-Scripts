#@ File    (label = "Input directory", style = "directory") outerFolder
#@Context context
#@LogService log

"""
file:       Sholl_Extract_Profile_From_Image_Demo.py
author:     Tiago Ferreira
info:       Demonstrates how to programmatically obtain a Sholl profile from a 
            segmented image

Edited by Sara McArdle to:
    -recursively work through folders (subfunctions inner and outer)
    -automatically set the starting radius from the nucleus outline (subfunction getRadius)
    -Also perform skeletonization and analyze the skeleton (subfunction skeletonize)
When it asks for a directory, select the MicroShapes folder
"""
from ij import IJ, ImagePlus, Prefs
from sc.fiji.snt.analysis.sholl import (Profile, ShollUtils)
from sc.fiji.snt.analysis.sholl.parsers import (ImageParser2D, ImageParser3D)
from sc.fiji.snt.analysis.sholl.math import LinearProfileStats
from sc.fiji.snt.analysis.sholl.gui import ShollPlot
from ij.plugin.frame import RoiManager
from ij.plugin import RoiEnlarger 
from ij.measure import ResultsTable
from ij.gui import PointRoi
import csv, os


def shollstats(imp, radius):

    # We may want to set specific options depending on whether we are parsing a
    # 2D or a 3D image. If the image has multiple channels/time points, we set
    # the C,T position to be analyzed by activating them. The channel and frame
    # will be stored in the profile properties map and can be retrieved later):
    if imp.getNSlices() == 1:
        parser = ImageParser2D(imp, context)
        parser.setRadiiSpan(1, ImageParser2D.MEAN) # mean of 4 measurements at every radius
        parser.setPosition(1, 1, 1) # channel, frame, Z-slice
    else: 
        parser = ImageParser3D(imp, context)
        parser.setSkipSingleVoxels(True) # ignore isolated voxels
        parser.setPosition(1, 1) # channel, frame
  
    # Segmentation: we can set the threshold manually using one of 2 ways:
    # 1. manually: parser.setThreshold(lower_t, upper_t)
    # 2. from the image itself: e.g., IJ.setAutoThreshold(imp, "Huang")
    # If the image is already binarized, we can skip setting threshold levels:
    if not (imp.isThreshold() or imp.getProcessor().isBinary()):
        IJ.setRawThreshold(imp, 100, 255);

    # Center: the x,y,z coordinates of center of analysis. In a real-case usage
    # these would be retrieved from ROIs or a centroid of a segmentation routine.
    # If no ROI exists coordinates can be set in spatially calibrated units
    # (floats) or pixel coordinates (integers):
    parser.setCenterFromROI()
    stepsize = 0.25

    # Sampling distances: start radius (sr), end radius (er), and step size (ss).
    # A step size of zero would mean 'continuos sampling'. Note that end radius
    # could also be set programmatically, e.g., from a ROI
    parser.setRadii(radius, stepsize, 50) # (sr, ss, er)

    # We could now set further options as we would do in the dialog prompt:
    parser.setHemiShells('none')  ## Use hemi-shells?
    parser.setThreshold(100, 255) ## Threshold values for image segmentation
    # (...)

    # Some options are only available in 2D, while others in 3D:
    if imp.getNSlices() == 1:
        parser.setRadiiSpan(1, ImageParser2D.MEDIAN)  ## 5 samples/radius w/ median integration
        parser.setPosition(1, 1, 1)  ## the channel, slice, and frame to be parsed
 
    else:
        parser.setSkipSingleVoxels(True)  ## ignore small speckles?
        parser.setPosition(1, 1)  ## the channel, and frame to be parsed
        

    # Parse the image. This may take a while depending on image size. 3D images
    # will be parsed using the number of threads specified in ImageJ's settings:
    parser.parse()
    if not parser.successful():
        log.error(imp.getTitle() + " could not be parsed!!!")
        stats = [0,0,0,0,0]
        return stats
        
    # We can e.g., access the 'Sholl mask', a synthetic image in which foreground
    # pixels have been assigned the no. of intersections:
    #parser.getMask().show()

    # Now we can access the Sholl profile:
    profile = parser.getProfile()


    if profile.isEmpty():
        log.error("All intersection counts were zero! Invalid threshold range!?")
        return
        

    lStats = LinearProfileStats(profile)
    #profile.trimZeroCounts()
    #bestDegree2 = lStats.findBestFit(1, 30, 0.70, -1)   
    #print(bestDegree2)
    #plot = ShollPlot(lStats)
    
    xvals =lStats.getXvalues()
    yvals = lStats.getYvalues()
    
    #vals = zip(xvals,yvals)
    
    BI = 0
    for i in range(1,len(yvals)):
        change=yvals[i]-yvals[i-1]
        if change>0:
            BI = BI + change*stepsize
    
    #if lStats.validFit():
        #stats = [BI, lStats.getPrimaryBranches(True), lStats.getRamificationIndex(True), lStats.getMax(True), lStats.getMaxima(True)[0].x]
    #else:
    stats = [BI, lStats.getPrimaryBranches(False), lStats.getRamificationIndex(False), lStats.getMax(False), lStats.getMaxima(False)[0].x]
    return stats
    

def getRadius(imp):
    Prefs.blackBackground = True;

    # turn the overlay into a ROI
    imp = IJ.getImage();
    rm = RoiManager.getRoiManager();
    rm.addRoi(imp.getRoi());
    rm.select(0);

    #get the centroid
    IJ.run(imp, "Measure", "");
    table = ResultsTable.getResultsTable()
    cx=table.getValue("X",0)
    cy=table.getValue("Y",0)

    #get image dimensions
    ip=imp.getProcessor()
    width = ip.getWidth()
    height = ip.getHeight()

    #Initialize a new image with those dimensions
    edm = IJ.createImage("EDM", "8-bit black", width, height, 1);
    #edm.show()

    #drop a point at the nucleus centroid
    roi=PointRoi(cx*5.8088,cy*5.8088,"small yellow hybrid"); #5.8068 = 1/pixelSize
    rm.addRoi(roi);
    rm.select(1);
    rm.runCommand(edm,"Draw"); #turn 1 pixel white on a black background

    #Euclidean Distance Map from the centroid
    IJ.setRawThreshold(edm, 0, 1);
    IJ.run(edm, "Convert to Mask", ""); #0-1 instead of 0-255
    IJ.run(edm, "Distance Map", "");

    rm.select(0); #select nucleus
    rm.runCommand(edm,"Measure"); #measure intensity, where intensity = distance from centroid

    table = ResultsTable.getResultsTable()
    distpix = table.getValue("Max",1) #get max distnace (pixels)
    distum = (distpix-0.5)/5.8088 #shrink by 0.5 pixels, then convert to microns

    #clear results for next cell
    IJ.run("Clear Results", "");
    
    #clear ROI manager
    rm.runCommand(imp,"Deselect");
    rm.runCommand(imp,"Delete");
    #rm.select(0);
    return distum
    
def skeletonize(imp):
    #get nucleus roi
    rm = RoiManager.getRoiManager();
    rm.addRoi(imp.getRoi());
    rm.select(0);
    #expand by 3 pixels
    #this is necessary to ensure each branch becomes it's own skeleton
    RoiEnlarger.enlarge(imp, 3);
    rm.runCommand(imp,"Update");
    rm.select(0);
    #clear soma area from binary image
    IJ.run(imp, "Clear", "slice");
    rm.runCommand(imp,"Deselect");

    #use binary Skeleton and standard ImageJ Analyze Skeleton to get stats
    IJ.setRawThreshold(imp, 120, 255);
    IJ.run(imp, "Convert to Mask", "");
    IJ.run(imp, "Skeletonize", "");
    IJ.run(imp, "Analyze Skeleton (2D/3D)", "prune=none");

    table = ResultsTable.getResultsTable()
    rows = table.size()

    slabs=0
    endpts=0
    junctions=0

    #sum stats for all branches
    for row in range(rows):
        endpts = endpts + table.getValue("# End-point voxels",row)
        junctions = junctions + table.getValue("# Junction voxels",row)
        slabs = slabs + table.getValue("# Slab voxels",row)

    #total skeleton length
    length = endpts + junctions + slabs

    skeletonStats = [length, endpts, junctions] #create 1 array

    #clear for next cell
    IJ.run("Clear Results", "");
    rm.runCommand(imp,"Deselect");
    rm.runCommand(imp,"Delete");
    return skeletonStats


def inner(folderpath):
    statList = [['Name','BI','Num Branches','SRI','Crit Val','Crit Radius','Skeleton Length','End pts','Branch pts']]
    print(folderpath)
    rm = RoiManager.getRoiManager();
    for root, directories, filenames in os.walk(folderpath):
        for filename in filenames:
            print(filename)
            if not filename.endswith('.tif'):
                continue
      
            image = IJ.openImage(os.path.join(root, filename))

            if image:
                image.show() ## Could be omitted. Image does not need to be displayed
                radius = getRadius(image) #get starting radius
                stats = shollstats(image, radius) #get sholl stats
                skeletonStats = skeletonize(image)        #get skeleton stats

                #if this failed (when cell boundary = nucleus boundary)
                if stats is None:
                    stats = [filename[0:-4], 0,0,0,0,0] #write 0 for all stats
                else:
                    stats.insert(0,filename[0:-4]) #append stats to giant list

                if skeletonStats is None:
                    skeletonStats = [0,0,0]
                    
                                    
                allStats = stats+skeletonStats

                statList.append(allStats)
                IJ.run("Close All", "");
            else:
                print("Image could not be obtained", folderPath, filename)
     
     #print to csv
    outfile = os.path.join(folderpath,os.path.basename(folderpath)+'.csv')
    with open(outfile, 'wb') as csvfile:
        csvwriter = csv.writer(csvfile)
        csvwriter.writerows(statList)

def outer():
    #set up neceesary measurements
    IJ.run("Set Measurements...", "area mean min centroid display redirect=None decimal=9");
    #ensure background is black
    IJ.run("Colors...", "foreground=white background=black selection=yellow");
    srcDir = outerFolder.getAbsolutePath()

    #recursively go
    for root, directories, filenames in os.walk(srcDir,topdown=True):
        for directory in directories:
              inner(os.path.join(root,directory))

#begin analysis
outer() #Select MicroShapes folder!

