/*********
Aligns images against Base Image.
 Run on CD45 image (NOT for project, it takes care of the looping)
 You only need to do this once per project, even if you repeat the islet detection and classification steps

 ********

 /**Imports**/


import javafx.scene.transform.Affine
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.indexer.Indexer
import org.opencv.core.TermCriteria
import qupath.lib.images.servers.ImageServer
import qupath.lib.images.servers.PixelCalibration
import qupath.lib.regions.RegionRequest
import qupath.opencv.tools.OpenCVTools

import java.awt.Graphics2D
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.Indexer;

import static qupath.lib.gui.scripting.QPEx.*

/**Align All Images**/
String registrationType="RIGID"
baseImageName = getProjectEntry().getImageName()
affinepath = buildFilePath(PROJECT_BASE_DIR, 'Affine '+baseImageName)
mkdirs(affinepath)

ImageServer<BufferedImage> serverBase= getCurrentImageData().getServer()

project.getImageList().each{
    if(it.getImageName() != baseImageName){
        println("Working on: "+it)
        String path = buildFilePath(PROJECT_BASE_DIR, 'Affine '+baseImageName,  it.toString()+".aff")
        ImageServer<BufferedImage> serverOverlay=it.readImageData().getServer()

        Affine affine=[]

        autoAlign(serverBase,serverOverlay,registrationType,affine,25)
        autoAlign(serverBase,serverOverlay,registrationType,affine,10)
        autoAlign(serverBase,serverOverlay,registrationType,affine,5)

        def matrix = []
        matrix << affine.getMxx()
        matrix << affine.getMxy()
        matrix << affine.getTx()
        matrix << affine.getMyx()
        matrix << affine.getMyy()
        matrix << affine.getTy()

        new File(path).withObjectOutputStream {
            it.writeObject(matrix)
        }
    }
}


static BufferedImage ensureGrayScale(BufferedImage img) {
    //Taken from here: https://github.com/qupath/qupath/blob/706e5b2e65a176c30f40b786ddc497328be2bd1a/qupath-experimental/src/main/java/qupath/lib/gui/align/ImageAlignmentPane.java#L632
    //with minimal modification
    if (img.getType() == BufferedImage.TYPE_BYTE_GRAY)
        return img
    if (img.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY)
        def colorModel = new ComponentColorModel(cs, 8 as int[], false, true,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE)
        return new BufferedImage(colorModel, img.getRaster(), false, null)
    }
    BufferedImage imgGray = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY)
    Graphics2D g2d = imgGray.createGraphics()
    g2d.drawImage(img, 0, 0, null)
    g2d.dispose()
    return imgGray
}

def autoAlign(ImageServer<BufferedImage> serverBase, ImageServer<BufferedImage> serverOverlay, String regionstrationType, Affine affine, double requestedPixelSizeMicrons) {
    // Taken from here: https://github.com/qupath/qupath/blob/706e5b2e65a176c30f40b786ddc497328be2bd1a/qupath-experimental/src/main/java/qupath/lib/gui/align/ImageAlignmentPane.java#L655
    //with minimal modification
    PixelCalibration calBase = serverBase.getPixelCalibration()
    double pixelSize = calBase.getAveragedPixelSizeMicrons()
    double downsample = 1
    if (!Double.isFinite(pixelSize)) {
        while (serverBase.getWidth() / downsample > 2000)
            downsample++;
        logger.warn("Pixel size is unavailable! Default downsample value of {} will be used", downsample)
    } else {
        downsample = requestedPixelSizeMicrons / calBase.getAveragedPixelSizeMicrons()
    }

    BufferedImage imgBase = serverBase.readBufferedImage(RegionRequest.createInstance(serverBase.getPath(), downsample, 0, 0, serverBase.getWidth(), serverBase.getHeight()))
    BufferedImage imgOverlay = serverOverlay.readBufferedImage(RegionRequest.createInstance(serverOverlay.getPath(), downsample, 0, 0, serverOverlay.getWidth(), serverOverlay.getHeight()))

    imgBase = ensureGrayScale(imgBase)
    imgOverlay = ensureGrayScale(imgOverlay)

    Mat matBase = OpenCVTools.imageToMat(imgBase)
    Mat matOverlay = OpenCVTools.imageToMat(imgOverlay)

    Mat matTransform = Mat.eye(2, 3, opencv_core.CV_32F).asMat()
// Initialize using existing transform
//		affine.setToTransform(mxx, mxy, tx, myx, myy, ty)
    try {
        FloatIndexer indexer = matTransform.createIndexer()
        indexer.put(0, 0, (float)affine.getMxx())
        indexer.put(0, 1, (float)affine.getMxy())
        indexer.put(0, 2, (float)(affine.getTx() / downsample))
        indexer.put(1, 0, (float)affine.getMyx())
        indexer.put(1, 1, (float)affine.getMyy())
        indexer.put(1, 2, (float)(affine.getTy() / downsample))
//			System.err.println(indexer)
    } catch (Exception e) {
        logger.error("Error closing indexer", e)
    }

    TermCriteria termCrit = new TermCriteria(TermCriteria.COUNT, 100, 0.0001)

    try {
        int motion
        switch (regionstrationType) {
            case "AFFINE":
                motion = opencv_video.MOTION_AFFINE
                break
            case "RIGID":
                motion = opencv_video.MOTION_EUCLIDEAN
                break
            default:
                logger.warn("Unknown registraton type {} - will use {}", regionstrationType, RegistrationType.AFFINE)
                motion = opencv_video.MOTION_AFFINE
                break
        }
        double result = opencv_video.findTransformECC(matBase, matOverlay, matTransform, motion, termCrit, null)
        logger.info("Transformation result: {}", result)
    } catch (Exception e) {
        logger.error("Unable to estimate transform", e)
        return
    }

// To use the following function, images need to be the same size
//		def matTransform = opencv_video.estimateRigidTransform(matBase, matOverlay, false);
    Indexer indexer = matTransform.createIndexer()
    affine.setToTransform(
            indexer.getDouble(0, 0),
            indexer.getDouble(0, 1),
            indexer.getDouble(0, 2) * downsample,
            indexer.getDouble(1, 0),
            indexer.getDouble(1, 1),
            indexer.getDouble(1, 2) * downsample
    )
    indexer.release()

    matBase.release()
    matOverlay.release()
    matTransform.release()
}
