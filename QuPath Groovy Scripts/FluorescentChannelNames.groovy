guiscript=true
/**
 * Visualize color channels
 * Written largely by Pete Bankhead during the 2022 QuPath Hackathon
 * https://github.com/qupath/2022-qupath-hackathon/discussions/7
 */


import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ListChangeListener.Change
import javafx.scene.Scene
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.WindowEvent
import qupath.lib.display.ChannelDisplayInfo
import qupath.lib.gui.tools.ColorToolsFX

import static qupath.lib.gui.scripting.QPEx.*

//adjustable
int ROW_HEIGHT = 25
int TEXT_WIDTH = 13

def imageDisplay = getCurrentViewer().getImageDisplay()

//find the channels and keep them in the starting order (C1-CX)
def availableChannels = imageDisplay.availableChannels()
def channels = imageDisplay.selectedChannels()
def sortedChannels = channels.sorted((c1, c2) -> {
    // Compare in a better way here...
    int i1 = availableChannels.indexOf(c1)
    int i2 = availableChannels.indexOf(c2)
    return Integer.compare(i1, i2)
//    return c1.getName().compareTo(c2.getName())
})
// Or just use channels...
def listView = new ListView<>(sortedChannels)

//Create the Javafx stage
def stage = new Stage()
stage.initOwner(getQuPath().getStage())
def scene = new Scene(listView, -1, -1, javafx.scene.paint.Color.TRANSPARENT)
stage.setScene(scene)
stage.initStyle(StageStyle.TRANSPARENT);

//close on double click
stage.getScene().addEventFilter( MouseEvent.MOUSE_CLICKED, e -> {
    if (e.getClickCount() == 2) {
        stage.fireEvent(
                new WindowEvent(
                        stage,
                        WindowEvent.WINDOW_CLOSE_REQUEST
                )
        )
        e.consume()
    }
})

//potentially helpful for getting rid of persistent listeners?
stage.setOnCloseRequest(e -> {
    listView.setItems(FXCollections.observableArrayList())
})

listView.setCellFactory(lv -> new ChannelListCell())

//Turn this on to make the box have a white background when useInvertedBackground is selected
//only useful in 0.4.0

//listView.styleProperty().bind(
//        Bindings.createStringBinding(() -> {
//            if (imageDisplay.useInvertedBackground())
//                return "-fx-background-color: rgba(255, 255, 255, 0.75); -fx-background-radius: 10;"
//            else
//                return "-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 10;"
//        },
//        imageDisplay.useInvertedBackgroundProperty()
//))
listView.setStyle("-fx-background-color: rgba(0, 0, 0, 0.75); -fx-background-radius: 10;");

//update every time anything changes about the active channels
channels.addListener((Change e) -> {
    stage.setHeight(channels.size() * ROW_HEIGHT)
    double textLength=availableChannels.collect{it.getName().size()}.max()-5
    stage.setWidth(textLength*TEXT_WIDTH)
} as ListChangeListener)

//makes the screen movable
new MoveablePaneHandler(stage)
stage.show()

//sets the height to be based on the number of channels
stage.setHeight(ROW_HEIGHT * channels.size())
//sets the width to be based on the number of characters in the longest name
double textLength=availableChannels.collect{it.getName().size()}.max()-5
stage.setWidth(textLength*TEXT_WIDTH)

class ChannelListCell extends ListCell<ChannelDisplayInfo> {

    @Override
    protected void updateItem(ChannelDisplayInfo item, boolean empty) {
        // calling super here is very important - don't skip this!
        super.updateItem(item, empty);
//        setStyle("-fx-background-color: rgba(0, 0, 0, 0); -fx-font-size: 16;");
        setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        if (item == null || empty) {
            setText(null)
            setGraphic(null)
            return
        }
        def name = item.getName()
        setText(name.substring(0, name.lastIndexOf("(")).trim())
        def colorInteger = item.getColor()
        def colorFX = ColorToolsFX.getCachedColor(colorInteger)
        if (colorFX.getBrightness() < 0.2)
            setTextFill(Color.WHITE)
        else
            setTextFill(colorFX)


    }

}

//copied from Input Display Window
/**
 * Enable an undecorated stage to be moved by clicking and dragging within it.
 * Requires the scene to be set. Note that this will set mouse event listeners.
 */
class MoveablePaneHandler {

    private double xOffset = 0;
    private double yOffset = 0;

    MoveablePaneHandler(Stage stage) {
        var scene = stage.getScene();
        if (scene == null)
            throw new IllegalArgumentException("Scene must be set on the stage!");
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            xOffset = stage.getX() - e.getScreenX();
            yOffset = stage.getY() - e.getScreenY();
            e.consume();
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            stage.setX(e.getScreenX() + xOffset);
            stage.setY(e.getScreenY() + yOffset);
            e.consume();
        });
    }
}