/**
 * Merges all images in a project to one pseudo-fluorescence image using pre-calculated affine transforms
 * Creates a new project entry with the pseudofluor image
 * Each deconvolved DAB stain is a channel, and the CD45 HE is a channel
 * Based heavily on: https://forum.image.sc/t/qupath-multiple-channel-in-separate-files-how-to-merge-them/29455/3?u=smcardle
 */



/**Imports**/
import qupath.lib.display.ImageDisplay
import qupath.lib.images.servers.ImageChannel

import static qupath.lib.gui.scripting.QPEx.*
import org.locationtech.jts.geom.util.AffineTransformation
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.roi.GeometryTools
import java.awt.geom.AffineTransform
import javafx.scene.transform.Affine
import javafx.application.Platform
import qupath.lib.gui.commands.ProjectImportImagesCommand
import qupath.lib.images.ImageData
import qupath.lib.images.servers.ConcatChannelsImageServer
import qupath.lib.images.servers.ImageServerProvider



/**Set up the CD45-DAB and CD45-H&E as the first two channels**/
def stains = getCurrentImageData().getColorDeconvolutionStains()

String affinepath = "F:\\Dirk\\ALL\\6267_01\\Affine 6267-01_CD45 - 2019-11-10 13.58.58.ndpi"
def project = getProject()
def imageList = project.getImageList()

def channels = []
def servers=[]

def cd45Server =  new TransformedServerBuilder(getCurrentServer())
        .deconvolveStains(stains,1,2)
        .build()

String name = "CD45"
channels << ImageChannel.getInstance(name + "-" + stains.getStain(1).getName(), ImageChannel.getDefaultChannelColor(channels.size()+1))
channels << ImageChannel.getInstance(name + "-" + stains.getStain(2).getName(), ImageChannel.getDefaultChannelColor(channels.size()+1))

servers << cd45Server

/**Get DAB from each other image listed in the affine transforms folder**/
new File(affinepath).eachFile { f ->
    f.withObjectInputStream {
        matrix = it.readObject()
        def transform = new AffineTransform(
                matrix[0], matrix[3], matrix[1],
                matrix[4], matrix[2], matrix[5]
        )
        
        transform = transform.createInverse()

        def entry = imageList.find { it.getImageName() + ".aff" == f.getName() }
        if (entry == null) {
            print 'Could not find image with name ' + f.getName()
            return
        }

        def newserver = entry.getServerBuilder().build()
        def serverTransformed = new TransformedServerBuilder(newserver)
                .transform(transform)
                .deconvolveStains(entry.readImageData().getColorDeconvolutionStains(),2) //use their stain vectors
                .build()
                
                //shorten this
        String longname = entry.getImageName()
        longname = longname[7..-1]
        def shortname=longname[1..(longname.indexOf('-')-2)]

        channels << ImageChannel.getInstance(shortname + "-" + stains.getStain(2).getName(), ImageChannel.getDefaultChannelColor(channels.size()+1))

        servers << serverTransformed
    }
}


/**Create a concatenated server**/
def concatserver = new ConcatChannelsImageServer(getCurrentServer(),servers)
def concatData = new ImageData(concatserver)

setChannels(concatData, channels as ImageChannel[])

//def imageData = new ImageData(concatserver)
Platform.runLater {
 //   def project = getProject()
    def out = ProjectImportImagesCommand.addSingleImageToProject(project, concatserver, null)
    out.setImageName('Concatenated')
    project.syncChanges()
    getQuPath().refreshProject()
}


/**Subfunction to Prepend a base name to channel names**/
List<ImageChannel> updateChannelNames(String name, Collection<ImageChannel> channels) {
    return channels
            .stream()
            .map( c -> {
                return ImageChannel.getInstance(name + '-' + c.getName(), c.getColor())
            }
            ).collect(Collectors.toList())
}