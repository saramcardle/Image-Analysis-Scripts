/*
Goes through all cell detections and summarizes their area by assigned class
Adds these to the measurement list for the first unnamed annotation object (likely the tissue detection)
Written by Sara McArdle of the La Jolla Institute Microscopy Core Facility, 2018
For questions, please contact smcardle@lji.org
 */
import qupath.lib.scripting.QPEx

allCells = QPEx.getDetectionObjects()
classes=allCells.collect{it.getPathClass()}
classList=classes.unique()
print(classList)

Areas = new double[classList.size()]

def summary = getAnnotationObjects().findAll {it.getDisplayedName().toString().contains('PathAnnotationObject') == true}

for (c=0; c<classList.size(); c++){
    classDet=allCells.findAll{it.getPathClass()==classList[c]}
    double classArea=0
    classDet.each{
        classArea+=it.getMeasurementList().getMeasurementValue("Cell: Area")
    }
    Areas[c]=classArea
    measurementName=classList[c].toString()+" Cell Area"
    summary[0].getMeasurementList().putMeasurement(measurementName, classArea)
}
summary[0].getMeasurementList().putMeasurement("Total Cell Area", Areas.sum())

fireHierarchyUpdate()