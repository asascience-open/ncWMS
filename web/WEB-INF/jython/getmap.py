# Implements the GetMap operation

from wmsExceptions import *
from config import *
import javagraphics, nj22dataset # TODO import other modules for CDMS server

def getMap(req, params, datasets):
    """ The GetMap operation.
       req = mod_python request object (or FakeModPythonRequestObject from Jython servlet)
       params = ncWMS.RequestParser object containing the request parameters
       datasets = dictionary of ncWMS.Datasets, indexed by unique id """
    
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
        raise WMSException("Invalid bounding box format: need four elements")
    try:
        bbox = [float(el) for el in bboxEls]
    except ValueError:
        raise WMSException("Invalid bounding box format: all elements must be numeric")
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

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise WMSException("You may only request a single value of ELEVATION")

    tValue = params.getParamValue("time", "")
    if len(tValue.split(",")) > 1 or len(tValue.split("/")) > 1:
        raise WMSException("You may only request a single value of TIME")

    # Get the requested transparency and background colour for the layer
    trans = params.getParamValue("transparent", "false").lower()
    if trans == "false":
        transparent = 0
    elif trans == "true":
        transparent = 1
    else:
        raise WMSException("The value of TRANSPARENT must be \"TRUE\" or \"FALSE\"")
    
    bgc = params.getParamValue("bgcolor", "0xFFFFFF")
    if len(bgc) != 8 or not bgc.startswith("0x"):
        raise WMSException("Invalid format for BGCOLOR")
    try:
        bgcolor = eval(bgc) # Parses hex string into an integer
    except:
        raise WMSException("Invalid format for BGCOLOR")

    # Get the scale for colouring the map: this is an extension to the
    # WMS specification
    scale = params.getParamValue("scale", "0,0") # 0,0 signals auto-scale
    if len(scale.split(",")) == 2:
        try:
            scaleMin, scaleMax = [float(x) for x in scale.split(",")]
        except ValueError:
            raise WMSException("Invalid number in SCALE parameter")
    else:     
        raise WMSException("The SCALE parameter must be of the form SCALEMIN,SCALEMAX")
    
    # Generate a grid of lon,lat points, one for each image pixel
    crs = params.getParamValue("crs")
    if SUPPORTED_CRSS.has_key(crs):
        GridClass = SUPPORTED_CRSS[crs] # see grids.py
        grid = GridClass(bbox, width, height)
    else:
        raise InvalidCRS(crs)

    # Find the source of the requested data
    dsAndVar = layers[0].split(LAYER_SEPARATOR)
    picData = None
    if len(dsAndVar) == 2 and datasets.has_key(dsAndVar[0]):
        location = datasets[dsAndVar[0]].location
        varID = dsAndVar[1]
        fillValue = 1e20 # Can't use NaN due to lack of portability
        # TODO: check the cache of extracted data arrays
        # Extract the data from the data source using the requested grid
        picData = nj22dataset.readData(location, varID, tValue, zValue, grid, fillValue)
        # TODO: cache the data array
    if picData is None:
        raise LayerNotDefined(layers[0])
    else:
        # Turn the data into an image and output to the client
         javagraphics.makePic(req, picData, width, height, fillValue, transparent, bgcolor, scaleMin, scaleMax)
    
    return

