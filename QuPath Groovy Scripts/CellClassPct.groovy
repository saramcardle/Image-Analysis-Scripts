import qupath.lib.objects.PathDetectionObject

def classes =[]
getDetectionObjects().each {
    classes.addAll(it.classifications)
}

def classSet = classes.toSet()
double pix = getCurrentServer().getPixelCalibration().getAveragedPixelSize()

def annots= getAnnotationObjects()
def hier = getCurrentHierarchy()
annots.each{annot->
    def cells = hier.getObjectsForROI(PathDetectionObject.class,annot.getROI())
    double cellNum = cells.size()
    classSet.each{c->
        
        double num  = cells.findAll{it.classifications.contains(c)}.size()
        //double area = cells.collect{it.getROI().getArea()*pix*pix}.sum()

        annot.measurements[c.toString() + ' Cell Num'] = num
        annot.measurements[c.toString() + ' Cell Pct']=num /cellNum * 100
        //annot.measurements[c.toString() + ' Cell Area'] = area
    }   
}

    