// Script written for QuPath v0.2.3
// Minimal working script to import labelled images and merge them into one
// (from the TileExporter) back into QuPath as annotations.
// https://forum.image.sc/t/import-labeled-tiles-into-qupath/43119/4?u=ep.zindy

import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import static qupath.lib.gui.scripting.QPEx.*
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import java.util.regex.Matcher
import java.util.regex.Pattern

def imageData = getCurrentImageData()

// Define output path (here, relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def directoryPath = buildFilePath(PROJECT_BASE_DIR, 'tiles', name)

File folder = new File(directoryPath);
File[] listOfFiles = folder.listFiles();

listOfFiles.each { file ->
    def path = file.getPath()
    def imp = IJ.openImage(path)
    
    // Only process the labelled images, not the originals
    if (!path.endsWith("-labelled.tif"))
        return
        
    print "Now processing: " + path
    
    // Parse filename to understand where the tile was located
    def parsedDXYWH = parseFilename(GeneralTools.getNameWithoutExtension(path))
   
    double downsample = parsedDXYWH[0] // TO CHANGE (if needed)
    ImagePlane plane = ImagePlane.getDefaultPlane()
    
    //imp.show(); //updateAndDraw();
    
    // Convert labels to ImageJ ROIs
    def ip = imp.getProcessor().convertToByte(false);
//    ip.threshold(125) //.setThreshold(1, 1);
    
    if (ip instanceof ColorProcessor) {
        throw new IllegalArgumentException("RGB images are not supported!")
    }
    
    int n = imp.getStatistics().max as int
    if (n == 0) {
        print 'No objects found!'
        return
    }
    def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, n)
    
    
    
    // Convert ImageJ ROIs to QuPath ROIs
    def rois = roisIJ.collect {
        if (it == null)
            return
        return IJTools.convertToROI(it, -parsedDXYWH[1]/downsample, -parsedDXYWH[2]/downsample, downsample, plane);
    }
    
    // Remove all null values from list
    rois = rois.findAll{null != it}
    
    // Convert QuPath ROIs to objects
    def pathObjects = rois.collect {
        pathObject = PathObjects.createAnnotationObject(it)
        pathObject.setPathClass(getPathClass("FromBitmap"))
        return pathObject
    }
    addObjects(pathObjects)
}

selectObjects {it.isAnnotation() && it.getPathClass() == getPathClass("FromBitmap")};
mergeSelectedAnnotations()

resolveHierarchy()



int[] parseFilename(String filename) {
    def p = Pattern.compile("\\[d=(.+?),x=(.+?),y=(.+?),w=(.+?),h=(.+?)]")
    parsedDXYWH = []
    Matcher m = p.matcher(filename)
    if (!m.find())
        throw new IOException("Filename does not contain tile position")
            
    parsedDXYWH << (m.group(1) as double)
    parsedDXYWH << (m.group(2) as double)
    parsedDXYWH << (m.group(3) as double)
    parsedDXYWH << (m.group(4) as double)
    parsedDXYWH << (m.group(5) as double)
    print(parsedDXYWH)
    return parsedDXYWH
}