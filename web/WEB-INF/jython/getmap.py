# Implements the GetMap operation
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from wmsExceptions import *
from config import *
import javagraphics

def getMap(req, params, datasets):
    """ The GetMap operation.
       req = Apache request object (or faked object from Jython servlet)
       params = ncWMS.RequestParser object containing the request parameters
       datasets = dictionary of dataset.AbstractDatasets, indexed by unique id """
    
    version = params.getParamValue("version")
    if version != WMS_VERSION:
        raise WMSException("VERSION must be %s" % WMS_VERSION)
    
    layers = params.getParamValue("layers").split(",")
    if len(layers) > LAYER_LIMIT:
        raise WMSException("You may only request a maximum of " +
            str(LAYER_LIMIT) + " layer(s) simultaneously from this server")
    
    styles = params.getParamValue("styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")
    for style in styles:
        if style != "":
            # TODO: handle styles properly
            raise StyleNotDefined(style)
    
    bboxEls = params.getParamValue("bbox").split(",")
    if len(bboxEls) !=4:
        raise WMSException("Invalid bounding box format")
    try:
        bbox = [float(el) for el in bboxEls]
    except ValueError:
        raise WMSException("Invalid bounding box format")
    if bbox[0] >= bbox[2] or bbox[1] >= bbox[3]:
        raise WMSException("Invalid bounding box format")
    
    try:
        width = int(params.getParamValue("width"))
        height = int(params.getParamValue("height"))
        if width < 1 or width > MAX_IMAGE_WIDTH:
            raise WMSException("Image width must be between 1 and " +
                str(MAX_IMAGE_WIDTH) + " pixels inclusive")
        if height < 1 or height > MAX_IMAGE_HEIGHT:
            raise WMSException("Image height must be between 1 and " +
                str(MAX_IMAGE_HEIGHT) + " pixels inclusive")
    except ValueError:
        raise WMSException("Invalid integer provided for WIDTH or HEIGHT")
    
    format = params.getParamValue("format")
    if format not in SUPPORTED_IMAGE_FORMATS:
        raise InvalidFormat(format)
    
    # Generate a grid of lon,lat points, one for each image pixel
    crs = params.getParamValue("crs")
    if SUPPORTED_CRSS.has_key(crs):
        GridClass = SUPPORTED_CRSS[crs] # see grids.py
        grid = GridClass(bbox, width, height)
    else:
        raise InvalidCRS(crs)

    # Find the source of the requested data
    dsAndVar = layers[0].split(LAYER_SEPARATOR)
    if len(dsAndVar) != 2:
        raise LayerNotDefined(layers[0])
    if datasets.has_key(dsAndVar[0]):
        var = datasets[dsAndVar[0]].getVariable(dsAndVar[1])
        if var is None:
            raise LayerNotDefined(layers[0])
    
    # Extract the data from the data source using the requested grid
    fillValue = 1e20 # Can't use NaN due to lack of portability
    picData = var.readData(grid, fillValue)
    # TODO: close the data source
    # TODO: cache the data array
    
    # Turn the data into an image and output to the client
    javagraphics.makePic(req, picData, width, height, fillValue)
