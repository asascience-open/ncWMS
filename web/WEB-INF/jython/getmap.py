# Implements the GetMap operation

import sys

if sys.platform.startswith("java"):
    # We're running on Jython
    import nj22dataset as datareader
    import javagraphics as graphics
else:
    # TODO: check for presence of CDAT
    import cdmsdataset as datareader
    import graphics
from wmsExceptions import *
import wmsUtils
import grids

def getLayerLimit():
    """ returns the maximum number of layers that can be requested in GetMap """
    return 1

def getSupportedImageFormats():
    """ returns the image formats supported by this operation """
    return graphics.getSupportedImageFormats()

def getSupportedExceptionFormats():
    """ The exception formats supported by this operation """
    # Supporting other exception formats (e.g. INIMAGE) will take a bit
    # of work in the exception-handling code
    return ["XML"]

def getMap(req, params, config):
    """ The GetMap operation.
       req = mod_python request object (or FakeModPythonRequestObject from Jython servlet)
       params = wmsUtils.RequestParser object containing the request parameters
       config = configuration object """
    
    _checkVersion(params) # Checks the VERSION parameter
    
    layers = params.getParamValue("layers").split(",")
    if len(layers) > getLayerLimit():
        raise WMSException("You may only request a maximum of " +
            str(getLayerLimit()) + " layer(s) simultaneously from this server")
    
    styles = params.getParamValue("styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")
    for style in styles:
        if style != "":
            # TODO: handle styles properly
            raise StyleNotDefined(style)
    
    format = params.getParamValue("format")
    if format not in graphics.getSupportedImageFormats():
        raise InvalidFormat("image", format, "GetMap")

    exception_format = params.getParamValue("exceptions", "XML")
    if exception_format not in getSupportedExceptionFormats():
        raise InvalidFormat("exception", exception_format, "GetMap")

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise WMSException("You may only request a single value of ELEVATION")

    tValue = params.getParamValue("time", "")
    if len(tValue.split(",")) > 1 or len(tValue.split("/")) > 1:
        # TODO: support animations
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
            if (scaleMin != 0 or scaleMax != 0) and scaleMin >= scaleMax:
                raise WMSException("SCALE min value must be less than max value")
        except ValueError:
            raise WMSException("Invalid number in SCALE parameter")
    else:     
        raise WMSException("The SCALE parameter must be of the form SCALEMIN,SCALEMAX")

    # Get the percentage opacity of the map layer: another WMS extension
    opa = params.getParamValue("opacity", "100")
    try:
        opacity = int(opa)
    except:
        raise WMSException("The OPACITY parameter must be a valid number in the range 0 to 100 inclusive")
    if opacity < 0 or opacity > 100:
        raise WMSException("The OPACITY parameter must be a valid number in the range 0 to 100 inclusive")
    
    # Generate a grid of lon,lat points, one for each image pixel
    grid = _getGrid(params, config)

    # Find the source of the requested data
    location, varID, queryable = _getLocationAndVariableID(layers, config.datasets)
    # Read the data for the image
    picData = datareader.readImageData(location, varID, tValue, zValue, grid, _getFillValue())
    # TODO: cache the data array
    # Turn the data into an image and output to the client
    graphics.makePic(req, format, picData, grid.width, grid.height, _getFillValue(), transparent, bgcolor, opacity, scaleMin, scaleMax)

    return

def _checkVersion(params):
    """ Checks that the VERSION parameter exists and is correct """
    version = params.getParamValue("version")
    if version != wmsUtils.getWMSVersion():
        raise WMSException("VERSION must be %s" % wmsUtils.getWMSVersion())

def _getGrid(params, config):
    """ Gets the grid for the map """
    # Get the bounding box
    bboxEls = params.getParamValue("bbox").split(",")
    if len(bboxEls) != 4:
        raise WMSException("Invalid bounding box format: need four elements")
    try:
        bbox = [float(el) for el in bboxEls]
    except ValueError:
        raise WMSException("Invalid bounding box format: all elements must be numeric")
    if bbox[0] >= bbox[2] or bbox[1] >= bbox[3]:
        raise WMSException("Invalid bounding box format")

    # Get the image width and height
    try:
        width = int(params.getParamValue("width"))
        height = int(params.getParamValue("height"))
        if width < 1 or width > config.maxImageWidth:
            raise WMSException("Image width must be between 1 and " +
                str(config.maxImageWidth) + " pixels inclusive")
        if height < 1 or height > config.maxImageHeight:
            raise WMSException("Image height must be between 1 and " +
                str(config.maxImageHeight) + " pixels inclusive")
    except ValueError:
        raise WMSException("Invalid integer provided for WIDTH or HEIGHT")

    # Get the Grid object
    crs = params.getParamValue("crs")
    if grids.getSupportedCRSs().has_key(crs):
        GridClass = grids.getSupportedCRSs()[crs] # see grids.py
        return GridClass(bbox, width, height)
    else:
        raise InvalidCRS(crs)

def _getLocationAndVariableID(layers, datasets):
    """ Returns a (location, varID, queryable) tuple containing the location of the dataset,
        the ID of the variable and a boolean which is true if the layer is queryable. 
        Only deals with one layer at the moment """
    dsAndVar = layers[0].split(wmsUtils.getLayerSeparator())
    if len(dsAndVar) == 2 and datasets.has_key(dsAndVar[0]):
        location = datasets[dsAndVar[0]].location
        varID = dsAndVar[1]
        return location, varID, datasets[dsAndVar[0]].queryable
    else:
        raise LayerNotDefined(layers[0])

def _getFillValue():
    """ returns the fill value to be used internally - can't be NaN because NaN is 
        not portable across Python versions or Jython """
    return 1.0e20 
        
