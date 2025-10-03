import static qupath.lib.gui.scripting.QPEx.*

//grab micros
def micros=getDetectionObjects().findAll{it.getPathClass().toString().contains('MicrogliaShape')}

//find reactive/ "angry" micros
def angry = micros.findAll{it.measurements['CD68%']>35&it.measurements['BI']>=1&it.measurements['Soma Delaunay: Num neighbors']>0}
angry.each{it.setPathClass(getPathClass('MicrogliaShape: Reactive'))}
