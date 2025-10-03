/*
Script to import a CSV file with cell names and metrics back to the QuPath project
The name of the CSV file must match the image name in the project.

Taken from https://forum.image.sc/t/there-and-back-again-qupath-cytomap-cluster-analysis/43352/5? (Michael Nelson, 2020)
and here: https://forum.image.sc/t/importing-cell-types-back-into-qupath-by-object-id/76718/8 (Pete Bankhead, 2023)

After running, you must reload the image file (File > Reload Data)
*/

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.gui.dialogs.Dialogs

def delim = ","
def Aproject = getProject()
def cellClass = "MicrogliaShape"
def imgExtension = '.ome.tiff' //can change this to '' to skip the filter

//select a folder where each CSV corresponds to 1 image with a matching name, other than extensions
def folder = Dialogs.promptForDirectory(null)
print(folder)
folder.listFiles().each { file ->

    //find matching image in project
    String imgName=file.name.indexOf('.').with {it != -1 ? file.name[0..<it] : file.name}
    print(imgName)

    def entry = Aproject .getImageList().find {it.getImageName() == imgName + imgExtension}

    if (entry == null){
        print "NO ENTRIES FOR IMAGE "+ imgName;
        return;
    }

    //get image object hierarchy
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()

    //find the cells you care about
    def cells = hierarchy.getDetectionObjects().findAll{it.getPathClass()==getPathClass(cellClass)}

    //create a map of their names. Can be altered to be UUID instead.
    def cellsByName = cells.groupBy{it.getName()}

    //read the csv data
    def lines = new File(file.getAbsolutePath()).readLines()
    def header = lines.pop().split(delim)
    print(header)

    //apply new measurements to each cell
    lines.each{line->
        //go from a row of a csv to a formatted Map with the headers
        def map = lineToMap(header, line.split(delim))

        //find the correct cell
        def cellName = map['Name']
        def cell = cellsByName[cellName]
        print(cell)

        //warnings
        if (cell == null) {
            println "WARN: No cell found for $cellName"

        } else if (cell.size() != 1) {
            println "WARN: ${cell.size()} cells for $cellName - will skip"

        } else {
            //add csv row map to call measurement map
            map.each { m ->
                if (m.key != 'Name') { //skip all entries that aren't doubles
                    if (m.value.isNumber()){
                    cell[0].measurements[m.key] = m.value as double
                    } else {
                        cell[0].measurements[m.key]=Double.NaN
                    }
                }
            }
        }
    }
    entry.saveImageData(imageData) //save
    //make sure to reload the image data at the end!
}
print "Done with all csv files!"

// Helper function to create a map from column headings -> values
Map lineToMap(String[] header, String[] content) {
    def map = [:]
    if (header.size() != content.size()) {
        throw new IllegalArgumentException("Header length doesn't match the content length!")
    }
    for (int i = 0; i < header.size(); i++)
        map[header[i]] = content[i]
    return map
}


