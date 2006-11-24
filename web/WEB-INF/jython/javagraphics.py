# Jython code for generating a picture from source data
from uk.ac.rdg.resc.ncwms.graphics import SimplePicMaker

def getSupportedImageFormats():
    """ Returns the list of supported image formats as MIME types """
    # TODO: could get this from SimplePicMaker
    return ["image/png"]

def makePic(req, format, picData, width, height, fillValue, transparent, bgcolor, opacity, scaleMin, scaleMax):
    """ Generates a picture from the given data array.
        format = the MIME type of the image to be generated
        req = WMS.FakeModPythonRequestObject
        picData = image data as array of floats
        width, height = Image dimensions
        fillValue = value representing missing data
        transparent = true if the background of the picture is to be transparent
        bgcolor = colour for background pixels if background is not transparent
        opacity = percentage opacity of the data pixels
        scaleMin, scaleMax = values to use for the extremes of the colour scale """

    picMaker = SimplePicMaker(picData, width, height, fillValue, transparent, bgcolor, opacity, scaleMin, scaleMax)
    req.content_type = format
    # req.getOutputStream() is only available in the 
    # WMS.FakeModPythonRequestObject
    picMaker.createAndOutputPicture(req.getOutputStream())
    return
