# Jython code for generating a picture from source data
from uk.ac.rdg.resc.ncwms.graphics import SimplePicMaker

def makePic(req, picData, width, height, fillValue, transparent, bgcolor, scaleMin, scaleMax):
    """ Generates a picture from the given data array.
        req = WMS.FakeModPythonRequestObject
        picData = image data as array of floats
        width, height = Image dimensions
        fillValue = value representing missing data
        transparent = true if the background of the picture is to be transparent
        bgcolor = colour for background pixels if background is not transparent """

    picMaker = SimplePicMaker(picData, width, height, fillValue, transparent, bgcolor, scaleMin, scaleMax)
    req.content_type = "image/png"
    # req.getOutputStream() is only available in the 
    # WMS.FakeModPythonRequestObject
    picMaker.createAndOutputPicture(req.getOutputStream())
    return
