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
import iso8601
from wmsExceptions import *
import wmsUtils
import grids

def getLayerLimit():
    """ returns the maximum number of layers that can be requested in GetMap """
    return 1

def getSupportedImageFormats():
    """ returns the image formats supported by this operation """
    return graphics.getSupportedImageFormats() + [_getGoogleEarthFormat()]

def _getGoogleEarthFormat():
    """ MIME type for KML """
    return "application/vnd.google-earth.kml+xml"

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
    
    # RequestParser replaces pluses with spaces: we must change back
    # to parse the format correctly
    format = params.getParamValue("format").replace(" ", "+")
    # Get a picture making object for this MIME type: this will throw
    # an InvalidFormat exception if the format is not supported
    if format != _getGoogleEarthFormat():
        picMaker = graphics.getPicMaker(format)
    else:
        class PicMaker: pass
        picMaker = PicMaker() # Dirty hack to prevent later errors

    exception_format = params.getParamValue("exceptions", "XML")
    if exception_format not in getSupportedExceptionFormats():
        raise InvalidFormat("exception", exception_format, "GetMap")

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise InvalidDimensionValue("elevation", "You may only request a single value")

    # Find the source of the requested data
    dataset, varID = _getDatasetAndVariableID(layers, config.datasets)
    # Get the metadata
    vars = datareader.getAllVariableMetadata(dataset)

    # Find the requested index/indices along the time axis
    if vars[varID].tvalues is None:
        # Ignore any time value that was given by the client (TODO OK?)
        tIndices = [0] # This layer has no time dimension
    else:
        # The time axis exists
        if params.getParamValue("time", "") == "":
            raise MissingDimensionValue("time")
        # Interpret the time specification
        tIndices = []
        for tSpec in params.getParamValue("time", "").split(","):
            startStopPeriod = tSpec.split("/")
            if len(startStopPeriod) == 1:
                # This is a single time value
                tIndex = vars[varID].findTIndex(startStopPeriod[0])
                tIndices.append(tIndex)
            elif len(startStopPeriod) == 2:
                # Extract all time values from start to stop inclusive
                start, stop = startStopPeriod
                startIndex = vars[varID].findTIndex(startStopPeriod[0])
                stopIndex = vars[varID].findTIndex(startStopPeriod[1])
                for i in xrange(startIndex, stopIndex + 1):
                    tIndices.append(i)
            elif len(startStopPeriod) == 3:
                # Extract time values from start to stop inclusive
                # with a set periodicity
                start, stop, period = startStopPeriod
                raise WMSException("Cannot yet handle animations with a set periodicity")
            else:
                raise InvalidDimensionValue("time", tSpec)

    # Get the requested transparency and background colour for the layer
    trans = params.getParamValue("transparent", "false").lower()
    if trans == "false":
        picMaker.transparent = 0
    elif trans == "true":
        picMaker.transparent = 1
    else:
        raise WMSException("The value of TRANSPARENT must be \"TRUE\" or \"FALSE\"")
    
    bgc = params.getParamValue("bgcolor", "0xFFFFFF")
    if len(bgc) != 8 or not bgc.startswith("0x"):
        raise WMSException("Invalid format for BGCOLOR")
    try:
        picMaker.bgColor = eval(bgc) # Parses hex string into an integer
    except:
        raise WMSException("Invalid format for BGCOLOR")

    # Get the extremes of the colour scale
    picMaker.scaleMin, picMaker.scaleMax = _getScale(params)

    # Get the percentage opacity of the map layer: another WMS extension
    opa = params.getParamValue("opacity", "100")
    try:
        picMaker.opacity = int(opa)
    except:
        raise WMSException("The OPACITY parameter must be a valid number in the range 0 to 100 inclusive")

    if format == _getGoogleEarthFormat():
        # This is a special case: we don't actually render the image,
        # we just return a KML document containing a link to the image
        if len(tIndices) > 1:
            # TODO: fix this - animations in GE are now possible!
            raise WMSException("Cannot display animations in Google Earth")

        # Set a suggested filename in the header
        # "inline" means "don't force a download dialog box in web browser"
        req.content_type = format
        req.headers_out["Content-Disposition"] = "inline; filename=%s.kml" % layers[0].replace("/", "_")

        req.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        req.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">")
        req.write("<Folder>")
        req.write("<visibility>1</visibility>")
        req.write("<GroundOverlay>")
        #if tValue != "":
            # TODO: GE doesn't understand something about ISO times
            #req.write("<TimeStamp><when>%s</when></TimeStamp>" % tValue)
        req.write("<name>%s</name>" % vars[varID].title)
        req.write("<description>%s</description>" % vars[varID].abstract)
        req.write("<visibility>1</visibility>")

        req.write("<Icon><href>")
        req.write("http://%s%s?SERVICE=WMS&amp;REQUEST=GetMap" %
            (req.server.server_hostname, req.unparsed_uri.split("?")[0]))
        req.write("&amp;VERSION=%s" % wmsUtils.getWMSVersion())
        req.write("&amp;LAYERS=%s" % layers[0])
        if styles == ['']:
            req.write("&amp;STYLES=")
        else:
            req.write("&amp;STYLES=%s" % styles[i])
        # TODO: get the FORMAT string properly
        req.write("&amp;FORMAT=image/png&amp;CRS=CRS:84&amp;TRANSPARENT=true")
        bboxEls = tuple([str(f) for f in _getBbox(params)])
        req.write("&amp;BBOX=%s,%s,%s,%s" % bboxEls)
        if zValue != "":
            req.write("&amp;ELEVATION=%s" % zValue)
        if params.getParamValue("time", "") != "":
            req.write("&amp;TIME=%s" % params.getParamValue("time", ""))
        # TODO get width and height more intelligently
        req.write("&amp;WIDTH=500&amp;HEIGHT=500")
        if not (scaleMin == 0.0 and scaleMax == 0.0):
            # TODO add an auto-scaled layer
            req.write("&amp;SCALE=%s,%s" % (scaleMin, scaleMax))
        req.write("</href></Icon>")

        req.write("<LatLonBox id=\"1\">")
        req.write("<west>%s</west><south>%s</south><east>%s</east><north>%s</north>" % bboxEls)
        req.write("<rotation>0</rotation>")
        req.write("</LatLonBox>")
        req.write("</GroundOverlay>")
        req.write("</Folder>")
        req.write("</kml>")
    else:
        # Generate a grid of lon,lat points, one for each image pixel
        grid = _getGrid(params, config)
        picMaker.picWidth, picMaker.picHeight = grid.width, grid.height
        # Read the data for the image frames
        # We do this as one large array so that the picture render can
        # automatically generate a colour scale if necessary
        picMaker.fillValue = _getFillValue()
        for tIndex in tIndices:
            # TODO: see if we already have this image in cache
            picData = datareader.readImageData(dataset, varID, tIndex, zValue, grid, _getFillValue())
            # TODO: cache the data array
            # Only add the label if this is an animation
            if len(tIndices) > 1:
                label = iso8601.tostring(vars[varID].tvalues[tIndex])
            else:
                label = ""
            picMaker.addFrame(picData, label)
        # Write the image to the client
        graphics.writePicture(req, picMaker)

    return

def _checkVersion(params):
    """ Checks that the VERSION parameter exists and is correct """
    version = params.getParamValue("version")
    if version != wmsUtils.getWMSVersion():
        raise WMSException("VERSION must be %s" % wmsUtils.getWMSVersion())

def _getBbox(params):
    """ Gets the bounding box as a list of four floating-point numbers """
    bboxEls = params.getParamValue("bbox").split(",")
    if len(bboxEls) != 4:
        raise WMSException("Invalid bounding box format: need four elements")
    try:
        bbox = [float(el) for el in bboxEls]
    except ValueError:
        raise WMSException("Invalid bounding box format: all elements must be numeric")
    if bbox[0] >= bbox[2] or bbox[1] >= bbox[3]:
        raise WMSException("Invalid bounding box format")
    return bbox

def _getGrid(params, config):
    """ Gets the grid for the map """
    # Get the bounding box
    bbox = _getBbox(params)

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

def _getScale(params):
    # Get the scale for colouring the map: this is an extension to the
    # WMS specification
    scale = params.getParamValue("scale", "0,0") # 0,0 signals auto-scale
    if len(scale.split(",")) == 2:
        try:
            scaleMin, scaleMax = [float(x) for x in scale.split(",")]
            if (scaleMin != 0 or scaleMax != 0) and scaleMin >= scaleMax:
                raise WMSException("SCALE min value must be less than max value")
            return scaleMin, scaleMax
        except ValueError:
            raise WMSException("Invalid number in SCALE parameter")
    else:     
        raise WMSException("The SCALE parameter must be of the form SCALEMIN,SCALEMAX")

def _getDatasetAndVariableID(layers, datasets):
    """ Returns a (dataset, varID) tuple containing the dataset and
        the ID of the variable 
        Only deals with one layer at the moment """
    dsAndVar = layers[0].split(wmsUtils.getLayerSeparator())
    if len(dsAndVar) == 2 and datasets.has_key(dsAndVar[0]):
        return datasets[dsAndVar[0]], dsAndVar[1]
    else:
        raise LayerNotDefined(layers[0])

def _getFillValue():
    """ returns the fill value to be used internally - can't be NaN because NaN is 
        not portable across Python versions or Jython """
    return 1.0e20
    
        
