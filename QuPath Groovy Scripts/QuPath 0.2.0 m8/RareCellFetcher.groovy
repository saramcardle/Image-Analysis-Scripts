/*
Function to help you annotate single, rare cells.
Run this after detecting cells and beginning to train an object classifier.
Shows you random cells that it thinks are in your desired class. You can assign them to a class,
and it automatically moves to the next cell. Update the classifier regularly to see your improved results.

Inspired by "fetch" command in Cell Profiler Analyst.

Written by Sara McArdle of the La Jolla Institute and Svidro, 2020.
 */
import javafx.application.Platform
import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.scripting.QPEx
import javafx.scene.control.ListView
import javafx.collections.FXCollections
import javafx.scene.layout.GridPane;
import javafx.scene.control.ChoiceBox
import javafx.geometry.Insets
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage
import javafx.scene.control.Label
import javafx.scene.control.Button
import javafx.beans.value.ChangeListener
import javafx.geometry.HPos
import javafx.scene.input.KeyCode

//function to get a random cell of a desired class that isn't already annotated
//chooses a cell, copies it's ROI to an annotation
def getNextCell(interest){
    def cells= QPEx.getDetectionObjects()
    def celltypes=cells.findAll{it.getPathClass()==getPathClass(interest)}
    def unassigned=celltypes.findAll{it.getParent().getPathClass()==null}
    
    Random rnd = new Random()
    def selection=unassigned[rnd.nextInt(unassigned.size())]

    def roi=selection.getROI()
    tempAnnot = PathObjects.createAnnotationObject(roi)

    QPEx.getCurrentHierarchy().selectionModel.setSelectedObject(tempAnnot,true)
}

//get existing classes
def cells= QPEx.getDetectionObjects()
if (cells.size()==0) {
    print("Must have detection objects")
}
def classifications = new ArrayList<>(cells.collect {it.getPathClass()} as Set)
if (classifications.size()<2){
    print("Must have cells assigned to at least 2 classes")
}

def classStr=classifications.collect{it.toString()}
def classObs= FXCollections.observableArrayList(classStr)

//list view for assigning classes (and label)
ListView<String> classListView = new ListView<String>(classObs)
if (classStr.size()<6) {
    classListView.setPrefHeight((classStr.size() * 24) + 4)
} else {
    classListView.setPrefHeight((6 * 24) + 4)
}
Label assignmentLabel = new Label("Assign to which class:")

//drop down for choosing which class to fetch (and label)
ChoiceBox classChoiceBox = new ChoiceBox(classObs)
classChoiceBox.setMaxWidth(Double.MAX_VALUE)
Label fetchLabel = new Label("Fetch which class: \n\n\n")

//Set up a button to assign a class
Button assignButton = new Button("Assign (A)")
assignButton.setOnAction {e ->
    if (classListView.selectionModel.selectedItem) { //only assign if a class is chosen
        tempAnnot.setPathClass(getPathClass(classListView.selectionModel.selectedItem))
        addObjects(tempAnnot)
        getCurrentHierarchy().insertPathObject(tempAnnot, true)

        getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString())
    }
}
assignButton.setMaxWidth(Double.MAX_VALUE)
assignButton.setDisable(true) //start disabled until a class is fetched the first time

//skip button for if you do not like a cell and do not want to annotate it
Button skipButton = new Button("Skip (S)")
skipButton.setOnAction {e ->
    getNextCell(classChoiceBox.selectionModel.getSelectedItem().toString())
}
skipButton.setMaxWidth(Double.MAX_VALUE)
skipButton.setDisable(true) //start disabled until a class is fetched the first time
/*skipButton.setOnKeyPressed(event->{
    if(event.getCode().equals(KeyCode.SPACE_BAR)){
        skipButton.fire();
    }
});*/
//highlight button in case you "un-select" a cell and forget which one is being shown
Button highlightButton = new Button("Highlight Cell (H)")
highlightButton.setOnAction {e ->
    getCurrentHierarchy().selectionModel.setSelectedObject(tempAnnot,true)
    QPEx.getCurrentViewer().setCenterPixelLocation(tempAnnot.getROI().getCentroidX(),tempAnnot.getROI().getCentroidY())
}
highlightButton.setMaxWidth(Double.MAX_VALUE)
highlightButton.setDisable(true)

//when a class is chosen, fetch a cell and enable all the rest of the buttons
classChoiceBox.getSelectionModel().selectedItemProperty().addListener({v,o,n->
    getNextCell(n.toString())
    assignButton.setDisable(false)
    skipButton.setDisable(false)
    highlightButton.setDisable(false)
} as ChangeListener)

//put all buttons into a grid pane
GridPane gridPane = new GridPane();
gridPane.setMinSize(100, 100);
gridPane.setPadding(new Insets(10, 10, 10, 10));
gridPane.setVgap(5);
gridPane.setHgap(10);
gridPane.setAlignment(Pos.CENTER);

//gridPane.add is read (object,Column,Row) and is 0-based
gridPane.add(fetchLabel,0,0)
gridPane.setHalignment(fetchLabel, HPos.RIGHT)
gridPane.add(classChoiceBox,1,0)
gridPane.add(assignmentLabel,0,1)
gridPane.setHalignment(assignmentLabel, HPos.CENTER)
gridPane.add(classListView,0,2,1,3)
gridPane.add(assignButton,1,2)
gridPane.add(skipButton,1,3)
gridPane.add(highlightButton,1,4)

gridPane.setOnKeyPressed{event ->
    KeyCode key = event.getCode()
    if(key.equals(KeyCode.S)){
        skipButton.fire();
    }
    if(key.equals(KeyCode.A)){
        assignButton.fire();
    }
    if(key.equals(KeyCode.H)){
        highlightButton.fire();
    }
}
//show the GUI
Platform.runLater { //something about threading I do not understand. Copied from Pete.
    def stage = new Stage()
    stage.initOwner(QuPathGUI.getInstance().getStage())
    stage.setScene(new Scene(gridPane))
    stage.setTitle("Fetch single cells to annotate")
    stage.show()
}