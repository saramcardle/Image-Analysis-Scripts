import qupath.lib.regions.RegionRequest
import static qupath.lib.gui.scripting.QPEx.*
import static qupath.lib.scripting.QP.*

//Choose Downsample
double downsample = 4 //bigger value = smaller image

//get server
def server = getCurrentServer()

//find existing annotations
def annots = getAnnotationObjects()

//sort and name by location
def annotsX=annots.toSorted{it.getROI().getCentroidX()}
annotsX.eachWithIndex{annot,idx->
    annot.setName('Region'+idx.toString())
}

//define the folder the images will be saved to
def folder = buildFilePath(PROJECT_BASE_DIR,'exports')
//if necessary, create it
mkdirs(folder)

annotsX.each { //for each annotation
   def roi = it.getROI() //it = current annotation in this round of the loop
   def requestROI = RegionRequest.createInstance(server.getPath(), downsample, roi) //get the correct location at the set downsample
   
   def path = buildFilePath(folder, it.getName() + '.tif') //file name will be based on annotation name
   
    writeImageRegion(server, requestROI, path) //write as a tif
}

