# Jython code for generating images for GetMap
from uk.ac.rdg.resc.ncwms.graphics import PicMaker
from uk.ac.rdg.resc.ncwms.exceptions import InvalidFormatException
from wmsExceptions import InvalidFormat

def getSupportedImageFormats():
    """ Returns the list of supported image formats as MIME types """
    # Convert from Java Set to a Python list
    return [f for f in PicMaker.getSupportedImageFormats()]

def getPicMaker(mimeType):
    """ Returns a PicMaker object that generates images for the given MIME type """
    try:
        return PicMaker.createPicMaker(mimeType)
    except InvalidFormatException, e:
        raise InvalidFormat("image", mimeType, "GetMap")

def writePicture(req, picMaker):
    """ Writes the picture back to the client """
    req.content_type = picMaker.mimeType
    picMaker.writeImage(req.getOutputStream())
