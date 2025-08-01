
String prefix = 'cellType_' + getCurrentImageNameWithoutExtension() + '_'
int downsample = 1

def viewer = getCurrentViewer()
def server = getCurrentServer()

def obj = getSelectedObject()
def annotName = obj.getName()
def roi = obj.getROI()

resetSelection();

viewer.getOverlayOptions().setShowAnnotations(false)

def requestROI = RegionRequest.createInstance(server.getPath(), downsample, roi)

def folder = buildFilePath(PROJECT_BASE_DIR,'exports')
mkdirs(folder)

String fileName = annotName? prefix + '_'+annotName : prefix 
    
def path = buildFilePath(folder,fileName+'_all.tif')
writeRenderedImageRegion(viewer, requestROI,path)


def display = viewer.getImageDisplay()
def selected = display.selectedChannels()
def startedOn = selected.toList()

startedOn.each {    
    display.setChannelSelected(it,false)
}

startedOn.each {    
    display.setChannelSelected(it,true)
    def path2 = buildFilePath(folder,fileName+'_'+it.getName()+'.tif')
    writeRenderedImageRegion(viewer, requestROI,path2)
    display.setChannelSelected(it,false)
}


startedOn.each {    
    display.setChannelSelected(it,true)
}

viewer.getOverlayOptions().setShowAnnotations(true)