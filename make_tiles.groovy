/**
 * Script to export image tiles (can be customized in various ways).
 */

//Here we change the class type of annotations containing "ROI" in their name to that of the name

import qupath.lib.images.servers.*
getAnnotationObjects().forEach(it -> {

    if (it.getName().contains("ROI")) {
    println it.getName()
            cla = getPathClass(it.getName())
        it.setPathClass(cla)
    }
})


// Get the current image (supports 'Run for project')
def imageData = getCurrentImageData()

// Define output path (here, relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathTiles = buildFilePath('E:\\Amemoire\\ABeihang\\resplit\\train', 'tiles_downsample_10_overlap_10', name)
mkdirs(pathTiles)

// Define output resolution in calibrated units (e.g. 祄 if available)
double requestedPixelSize = 5.0

// Convert output resolution to a downsample factor
double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()
//double downsample = requestedPixelSize / pixelSize
double downsample = 10.0


// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
    //.channels(ColorTransforms.createColorDeconvolvedChannel(getCurrentImageData().getColorDeconvolutionStains(), 1))
    //.channels(0) // Select detection channel
    .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
    .addLabel('ROI', 1)      // Choose output labels (the order matters!)
    //.addLabel('ROI2', 1)
    //.addLabel('HSROI', 1)
    .multichannelOutput(true)  // If true, each label is a different channel (required for multiclass probability)
    .build()

// Create an exporter that requests corresponding tiles from the original & labeled image servers
new TileExporter(imageData)
    .downsample(downsample)     // Define export resolution
    .imageExtension('.png')     // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(512)              // Define size of each tile, in pixels
    .labeledServer(labelServer) // Define the labeled image server to use (i.e. the one we just built)
    .annotatedTilesOnly(true)  // If true, only export tiles if there is a (labeled) annotation present
    .overlap(10)                // Define overlap, in pixel units at the export resolution
    .writeTiles(pathTiles)     // Write tiles to the specified directory

print 'Done!'
