# Jython code for generating a picture from source data
from uk.ac.rdg.resc.ncwms.graphics import PicMaker
from uk.ac.rdg.resc.ncwms.exceptions import InvalidFormatException

from wmsExceptions import InvalidFormat

def getSupportedImageFormats():
    """ Returns the list of supported image formats as MIME types """
    fs = []
    formats = PicMaker.getSupportedImageFormats()
    for format in formats:
        fs.append(format)
    return fs

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
