import qupath.ext.stardist.StarDist2D
// Get current image - assumed to have color deconvolution stains set
def imageData = getCurrentImageData()
def stains = imageData.getColorDeconvolutionStains()

// Set everything up with single-channel fluorescence model
def pathModel = 'C:\\Users\\92035\\Music\\Amemoire\\StarDist\\预训练模型\\dsb2018_heavy_augment.pb'

def stardist = StarDist2D.builder(pathModel)
        .preprocess(
            ImageOps.Channels.deconvolve(stains),
            ImageOps.Channels.extract(0),
            ImageOps.Filters.median(2),
            ImageOps.Core.divide(1.5)
         ) // Optional preprocessing (can chain multiple ops)
        .pixelSize(0.5)              
        .includeProbability(true)    
        .threshold(0.1)             
        .build()
        
// Run detection for the selected objects
//var imageData = getCurrentImageData()
var pathObjects = getSelectedObjects()
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
    return
}
stardist.detectObjects(imageData, pathObjects)
println 'Done!'