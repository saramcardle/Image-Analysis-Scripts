import static qupath.lib.scripting.QP.*
import static qupath.lib.gui.scripting.QPEx.*


runObjectClassifier("CD20");

def cells = getCellObjects()
//def Bcells = cells.findAll{it.getPathClass()==getPathClass('CD20')}
def nonB=cells.findAll{it.getPathClass()==null}

def imageData = getCurrentImageData()
def TcellClass=loadObjectClassifier('CD4_CD8')
TcellClass.classifyObjects(imageData,nonB,true)

