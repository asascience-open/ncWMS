# Web Map Server for Godiva2 website

import os, math, struct, types, zlib, re, sys, time
import MV, cdms
#import minipng

def wms(req):
    """ Entry point for the Web Map Server """
    # Turn the request object into a dictionary of key-value pairs
    params = {}
    try:
        if not req.args:
            raise WMSException("Must provide a SERVICE argument")
        for kvp in req.args.split("&"):
            (key, value) = kvp.split("=")
            params[key] = value
        service = get_param_value(params, "service")
        request = get_param_value(params, "request")
        if service != "WMS":
            raise WMSException("SERVICE parameter must be WMS")
        if request == "GetCapabilities":
            get_capabilities(req, params)
        elif request == "GetMap":
            get_map(req, params)
        elif request == "GetFeatureInfo":
            raise WMSException("Operation not yet supported", "OperationNotSupported")
        else:
            raise WMSException("Invalid operation")
    except WMSException, e:
        req.content_type="text/xml"
        e.write(req)

def get_capabilities(req, params):
    """ Implements the GetCapabilities operation """
    
    version = get_param_value(params, "version", "")
    format = get_param_value(params, "format", "")
    updatesequence = get_param_value(params, "updatesequence", "")    
    # We ignore the version and format arguments
    # TODO: deal with updatesequence
    
    req.content_type = "text/xml"
    req.write(XML_HEADER)
    req.write("<WMS_Capabilities version=\"" + WMS_VERSION + "\" xmlns=\"http://www.opengis.net/wms\"")
    req.write(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
    # The next two lines should be commented out if you wish to load this document
    # in Cadcorp SIS from behind the University of Reading firewall
    req.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    req.write(" xsi:schemaLocation=\"http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd\"")
    req.write(">")
    
    req.write("<Service>")
    req.write("<Name>WMS</Name>")
    req.write("<Title>CDAT-based Web Map Server</Title>")
    req.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"http://www.nerc-essc.ac.uk\"/>")
    req.write("<Fees>none</Fees>")
    req.write("<AccessConstraints>none</AccessConstraints>")
    req.write("<LayerLimit>" + str(LAYER_LIMIT) + "</LayerLimit>")
    req.write("<MaxWidth>" + str(MAX_IMAGE_WIDTH) + "</MaxWidth>")
    req.write("<MaxHeight>" + str(MAX_IMAGE_HEIGHT) + "</MaxHeight>")
    req.write("</Service>")
    
    req.write("<Capability>")
    req.write("<Request>")
    req.write("<GetCapabilities>")
    req.write("<Format>text/xml</Format>")
    # TODO: detect the full server context path
    req.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
        req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    req.write("</GetCapabilities>")
    req.write("<GetMap>")
    for format in SUPPORTED_IMAGE_FORMATS:
        req.write("<Format>" + format + "</Format>")
    req.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
        req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    req.write("</GetMap>")
    req.write("</Request>")
    # TODO: support more exception types
    req.write("<Exception>")
    for ex_format in SUPPORTED_EXCEPTION_FORMATS:
        req.write("<Format>" + ex_format + "</Format>")
    req.write("</Exception>")
    
    # Now for the layers
    # We recurse through the directory structure to find datasets that contain layers
    output_layers(req, ".")
    
    req.write("</Capability>")
    req.write("</WMS_Capabilities>")

def output_layers(req, relpath):
    """ Outputs the details of the layer at the given path, which is the name
        of a directory, relative to the root path """
        
    req.write("<Layer>")
    
    fullpath = os.path.join(root, relpath)
    cdmlFile = None
    childDirs = []
    ncFiles = []
    
    for file in os.listdir(fullpath):
        relchildpath = os.path.join(relpath, file)
        fullchildpath = os.path.join(root, relchildpath)
        if os.path.isdir(fullchildpath):
            childDirs.append(relchildpath)
        elif file.endswith(".cdml"):
            cdmlFile = relchildpath
        elif file.endswith(".nc"):
            ncFiles.append(relchildpath)
    
    if cdmlFile is None:
        # There was no CDML file in this directory so we just use the directory
        # name as the title, or the root name if this is the root directory
        if relpath == ".":
            req.write("<Title>ESSC Web Map Server</Title>")
            for crs in SUPPORTED_CRSS:
                req.write("<CRS>" + crs + "</CRS>")
            # Specify the supported Styles.  We support a rainbow style
            req.write("<Style>")
            req.write("<Name>rainbow</Name>")
            req.write("<Title>Rainbow</Title>")
            req.write("<Abstract>Rainbow colour scheme</Abstract>")
            req.write("<LegendURL width=\"40\" height=\"514\">")
            req.write("<Format>image/png</Format>")
            req.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
                req.server.server_hostname + "/godiva2cdat/images/rainbowScaleBar2.png\"/>")
            req.write("</LegendURL>")
            req.write("</Style>")
        else:
            req.write("<Title>" + os.path.basename(fullpath) + "</Title>")
        for ncFile in ncFiles:
            req.write("<Layer>")
            req.write("<Title>" + os.path.basename(ncFile) + "</Title>")
            output_layer(req, ncFile)
            req.write("</Layer>")
    else:
        # We found a CDML file in this directory
        f = cdms.open(os.path.join(root, cdmlFile))
        req.write("<Title>" + f.title + "</Title>")
        f.close()
        output_layer(req, cdmlFile)
    
    for childDir in childDirs:
        output_layers(req, childDir)
    
    req.write("</Layer>")
    
def output_layer(req, relpath):
    """ Outputs the contents of the given dataset (CDML file or NetCDF file)
        as a Layer.  relpath is the path to the dataset (file), relative to
        the root directory. """
    f = cdms.open(os.path.join(root, relpath))
    
    # For sake of compactness in the XML, we see whether all variables in the
    # same file have the same dimensions, bounding box, etc
    # TODO
    
    for varName in f.variables.keys():
        var = f[varName]
        layerName = relpath + "/" + varName
        req.write("<Layer>")
        req.write("<Name>" + layerName + "</Name>")
        # Use the standard name in the title if possible
        if hasattr(var, "standard_name"):
            req.write("<Title>" + var.standard_name + "</Title>")
        else:
            req.write("<Title>" + varName + "</Title>")
        # TODO: get the proper bounding box
        minLon = -180
        maxLon = 180
        minLat = -90
        maxLat = 90
        req.write("<EX_GeographicBoundingBox>")
        req.write("<westBoundLongitude>" + str(minLon) + "</westBoundLongitude>")
        req.write("<eastBoundLongitude>" + str(maxLon) + "</eastBoundLongitude>")
        req.write("<southBoundLatitude>" + str(minLat) + "</southBoundLatitude>")
        req.write("<northBoundLatitude>" + str(maxLat) + "</northBoundLatitude>")
        req.write("</EX_GeographicBoundingBox>")
        req.write("<BoundingBox CRS=\"CRS:84\" ")
        req.write("minx=\"" + str(minLon) + "\" maxx=\"" + str(maxLon) + "\" ")
        req.write("miny=\"" + str(minLat) + "\" maxy=\"" + str(maxLat) + "\"/>")
        # Now deal with the depth dimension
        z = var.getLevel()
        if z:
            zvals = z.getValue()
            try:
                if z.positive.lower() == 'down':
                    zvals = 0 - zvals
            except AttributeError:
                # z does not have a "positive" attribute
                pass
            req.write("<Dimension name=\"elevation\" units=\"" + z.units +
                "\" multipleValues=\"0\" nearestValue=\"0\" default=\"" +
                str(zvals[0]) + "\">")
            firstTime = True
            for lev in zvals:
                if not firstTime:
                    req.write(",")
                req.write(str(lev))
                firstTime = False
            req.write("</Dimension>")
        # Now deal with the time dimension
        t = var.getTime()
        if t:
            writeTimeDimension(req, t)
        # We expose metadata about the data through a non-standard XML document.
        # This contains the minimum and maximum values of the data (if available)
        # and the units
        req.write("<MetadataURL type=\"godiva2\">")
        req.write("<Format>text/xml</Format>")
        req.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
            req.server.server_hostname + "/godiva2cdat/godiva2.py/metadata?" +
            "layername=" + layerName + "\"/>")
        req.write("</MetadataURL>")
        req.write("</Layer>")
        
def writeTimeDimension(req, tAxis):
    """ Writes all the time values in the given tAxis as a comma-separated list
        to the client. We write the time values as an explicit list for now:
        this isn't very space-efficient in the case of a linearly-spaced axis
        (which is probably the general case) """
        
    req.write("<Dimension name=\"time\" units=\"ISO8601\" multipleValues=\"0\"" +
        " nearestValue=\"0\">") # TODO: deal with "current"
    firstTime = True
    for t in tAxis.asComponentTime():
        if not firstTime:
            req.write(",")
        s = repr(t)
        tt = s.split(' ')
    
        ttt = tt[0].split('-')
        yr=int(ttt[0])
        mo=int(ttt[1])
        da=int(ttt[2])
    
        ttt=tt[1].split(':')
        hr=int(ttt[0])
        mi=int(ttt[1])
        tttt = ttt[2].split('.')
        se=int(tttt[0])
        ms=int(tttt[1])
        
        req.write("%04d-%02d-%02dT%02d:%02d:%02d.%03dZ" % (yr,mo,da,hr,mi,se,ms))
        firstTime = False
    req.write("</Dimension>")

def metadata(req, layername):
    """ Returns a simple, non-standard set of metadata about the given layer.
        This metadata includes the minimum and maximum valid values of the data
        in the layer, together with the layer units. """
    dspath, variable = get_dataset_and_variable(layername)
    try:
        f = cdms.open(dspath)
        meta = f[variable] # Get the metadata for this variable
        valid_min, valid_max = get_valid_min_max(meta)
        if hasattr(meta, 'units'):
            units = meta.units
        else:
            units = ''
        req.content_type = "text/xml"
        req.write(XML_HEADER)
        req.write("<metadata format=\"godiva2\" layer=\"" + layername + "\">")
        req.write("<valid_min>" + str(valid_min) + "</valid_min>")   
        req.write("<valid_max>" + str(valid_max) + "</valid_max>")
        req.write("<units>" + str(units) + "</units>")
        req.write("</metadata>")
    except cdms.error.CDMSError:
        # Should not happen in operation, but should we do something different here?
        # Like a 404 file not found code?
        raise WMSException("There is no layer called " + layers[0], "LayerNotDefined")
    else:
        f.close()
    
    
def get_dataset_and_variable(layername):
    """ returns a tuple of (datasetpath, variablename) for the given layer name """
    pathEls = layername.split("/")
    reldatasetpath = "/".join(pathEls[:-1])
    fulldspath = os.path.normpath(os.path.join(root, reldatasetpath))
    return (fulldspath, pathEls[-1])

def get_valid_min_max(var):
    """ Gets the valid minimum and maximum values of the given variable """# If we read from a CDML file then valid_max is a single number.  If we
    # read from a NetCDF file, then valid_max is an array (it seems)
    # This doesn't seem like a terribly good way of doing this, but it works.
    try:
        scale_max = var.valid_max[0] # Throws "unscriptable object" if not an array
    except:
        scale_max = var.valid_max
    try:
        scale_min = var.valid_min[0]
    except:
        scale_min = var.valid_min
    return (scale_min, scale_max)
    
def get_map(req, params, debug=False):
    """ Implements the GetMap operation """
    
    version = get_param_value(params, "version")
    if version != WMS_VERSION:
        raise WMSException("VERSION must be " + WMS_VERSION)
    
    layers = get_param_value(params, "layers").split(",")
    if len(layers) > LAYER_LIMIT:
        raise WMSException("You may only request a maximum of " +
            str(LAYER_LIMIT) + " layer(s) simultaneously from this server")
    
    styles = get_param_value(params, "styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")
    for style in styles:
        if style != "rainbow" and style != "":
            raise WMSException("The style " + style + " is not supported by this server",
                "StyleNotDefined")
    
    crs = get_param_value(params, "crs")
    if crs not in SUPPORTED_CRSS:
        raise WMSException("The CRS " + crs + " is not supported by this server",
            "InvalidCRS")
    
    bboxEls = get_param_value(params, "bbox").split(",")
    if len(bboxEls) !=4:
        raise WMSException("Invalid bounding box format")
    try:
        bbox = [float(el) for el in bboxEls]
    except ValueError:
        raise WMSException("Invalid bounding box format")
    if bbox[0] >= bbox[2] or bbox[1] >= bbox[3]:
        raise WMSException("Invalid bounding box format")
    if crs == CRS_EPSG4326:
        # This CRS has latitude before longitude (I think)
        bbox = [bbox[1], bbox[0], bbox[3], bbox[2]]
    
    try:
        width = int(get_param_value(params, "width"))
        height = int(get_param_value(params, "height"))
        if width < 1 or width > MAX_IMAGE_WIDTH:
            raise WMSException("Image width must be between 1 and " +
                str(MAX_IMAGE_WIDTH) + " pixels inclusive")
        if height < 1 or height > MAX_IMAGE_HEIGHT:
            raise WMSException("Image height must be between 1 and " +
                str(MAX_IMAGE_HEIGHT) + " pixels inclusive")
    except ValueError:
        raise WMSException("Invalid integer provided for WIDTH or HEIGHT")
    
    format = get_param_value(params, "format")
    if format not in SUPPORTED_IMAGE_FORMATS:
        raise WMSException("The image format " + format + " is not supported by this server",
            "InvalidFormat")
    
    if crs == CRS_LONLAT or crs == CRS_EPSG4326:
        targetGrid = get_lat_lon_grid(bbox, width, height)
    elif crs == CRS_MERCATOR:
        targetGrid = get_mercator_grid(bbox, width, height)
    
    # Open the dataset
    # Just deal with the first layer for now
    dspath, variable = get_dataset_and_variable(layers[0])
    try:
        f = cdms.open(dspath)
        meta = f[variable] # Get the metadata for this variable
        elevation = get_param_value(params, "elevation", "")
        zSlice = None
        if elevation == "":
            # Use the default elevation, i.e. the first value
            zSlice = slice(0,1)
        elif len(elevation.split(",")) > 1 or len(elevation.split("/")) > 1:
            raise WMSException("Server cannot handle requests for multiple ELEVATION values")
        else:
            # Look for the requested elevation in the metadata
            zVal = float(elevation)
            z = meta.getLevel()
            if z:
                zvals = z.getValue()
                try:
                    if z.positive.lower() == 'down':
                        zvals = 0 - zvals
                except AttributeError:
                    # z does not have a "positive" attribute
                    pass
                for i in xrange(len(zvals)):
                    # Allow small amount of tolerance in request value
                    if abs((zvals[i] - zVal) / zvals[i]) < 1e-6:
                        zSlice = slice(i,i+1)
                        break
                if not zSlice:
                    raise WMSException("ELEVATION value not recognized", "InvalidDimensionValue")
                    
        if format == PNG_FORMAT:
            output_png(req, params, f, variable, bbox, zSlice, targetGrid, debug)
        
    except cdms.error.CDMSError:
        raise WMSException("There is no layer called " + layers[0], "LayerNotDefined")
    except ValueError:
        # Thrown by "zVal = float(elevation)"
        raise WMSException("Invalid number provided for ELEVATION", "InvalidDimensionValue")
    else:
        f.close()

def output_png(req, params, f, variable, bbox, zSlice, targetGrid, debug=False):
    """ outputs a PNG picture of the requested data """
    
    # Get the variable from the file f at the requested depth level and project
    # onto the requested grid
    
    # We need to compute the start, stride and count for the lat and lon axes:
    # This data extraction step is generally the slowest step so we must be
    # careful not to extract unnecessarily-large amounts of data, otherwise this
    # method will be unacceptably slow for high-resolution data
    # lon = slice(start, stop, step)
    
    # TODO: This assumes that the latitude and longitude axes are regularly-spaced,
    # which could be a poor assumption with some datasets!
    
    # Get the metadata for the axis
    metadata = f[variable]
    
    # Get the minimum and maximum indices along the latitude direction that
    # are required to get the requested data.  The math.floor() and math.ceil()
    # calls avoid round-off errors
    minj, maxj = metadata.getLatitude().mapInterval((math.floor(bbox[0]), math.ceil(bbox[2])))
    
    # Do the same for longitude
    mini, maxi = metadata.getLongitude().mapInterval((math.floor(bbox[1]), math.ceil(bbox[3])))
    if debug:
        sys.stderr.write("lon=slice(%d,%d) lat=slice(%d,%d)\n" % (mini, maxi, minj, maxj))
    
    start = time.time()
    #var = f(variable, lon=slice(mini, maxi), lat=slice(minj, maxj),
    #    lev=zSlice, squeeze=1, grid=targetGrid)
    var = f(variable, lon=(math.floor(bbox[0]), math.ceil(bbox[2])),
        lat=(math.floor(bbox[1]), math.ceil(bbox[3])),
        lev=zSlice, squeeze=1, grid=targetGrid)
    if debug:
        sys.stderr.write("Time to extract data: %f\n" % (time.time() - start))
    
    # Create the picture object.

    # Calculate the scale factors (index = m * value + c)
    try:
        scale_min, scale_max = [float(x) for x in get_param_value(params, "scale", "").split(",")]
    except:
        # Get the default scale if not specified in the request parameters
        scale_min, scale_max = get_valid_min_max(var)
    
    m = 253 / (scale_max - scale_min)
    c = 2 - m * scale_min
    
    # Calculate an array of colour indices
    start = time.time()
    var *= m
    var += c
    if debug:
        sys.stderr.write("Time to calculate colour array: %f\n" % (time.time() - start))
    
    # Set out-of-range values to index 1
    start = time.time()
    var = MV.choose(var > 255.0 and var < 2.0, (var, 1))
    if debug:
        sys.stderr.write("Time to deal with out-of-range values: %f\n" % (time.time() - start))
    
    # Turn array into array of unsigned bytes.  This seems to turn missing
    # values into zeros automatically
    start = time.time()
    b = var.astype(MV.UnsignedInt8)
    if debug:
        sys.stderr.write("Time to convert to unsigned bytes: %f\n" % (time.time() - start))
    
    # Write the PNG file
    start = time.time()
    write_indexed_png(req, b.getValue().tolist(), get_palette(), get_alpha_channel(100))
    if debug:
        sys.stderr.write("Time to write PNG data: %f\n" % (time.time() - start))
    

def get_lat_lon_grid(bbox, width, height):
    """ Returns a grid in Plate Carree according to the given BBOX.
        The BBOX is a list of four floating-point numbers representing minx,
        miny, maxx and maxy.  We have already checked that maxx > minx and
        maxy > miny. """
    (minx, miny, maxx, maxy) = bbox
    if minx < -180 or minx > 180 or miny < -90 or miny > 90 or \
       maxx < -180 or maxx > 180 or maxy < -90 or maxy > 90:
        raise WMSException("Invalid bounding box format")
    
    # Calculate the pixel sizes in the x and y directions
    dx = float(maxx - minx) / width
    dy = float(maxy - miny) / height
    # Calculate the coordinates of each pixel in the x and y directions:
    # the coordinates are in the centre of each pixel
    lons = [(minx + (i + 0.5) * dx) for i in xrange(width)]
    # The latitude axis is flipped
    lats = [(miny + ((height - j - 1) + 0.5) * dy) for j in xrange(height)]
    
    return cdms.grid.createGenericGrid(lonArray = lons, latArray = lats)
    
def get_mercator_grid(bbox, width, height):
    """ Returns a grid in Mercator according to the given BBOX.
        The BBOX is a list of four floating-point numbers representing minx,
        miny, maxx and maxy.  We have already checked that maxx > minx and
        maxy > miny. """
    (minx, miny, maxx, maxy) = bbox
    if minx < -180 or minx > 180 or miny < -90 or miny > 90 or \
       maxx < -180 or maxx > 180 or maxy < -90 or maxy > 90:
        raise WMSException("Invalid bounding box format")
    
    # Calculate the pixel sizes in the x and y directions
    dx = float(maxx - minx) / width
    dy = float(maxy - miny) / height
    # Calculate the coordinates of each pixel in the x and y directions:
    # the coordinates are in the centre of each pixel
    lons = [(minx + (i + 0.5) * dx) for i in xrange(width)]
    lats = get_mercator_latitude_array(miny, maxy, height)
    
    return cdms.grid.createGenericGrid(lonArray = lons, latArray = lats)

def get_mercator_latitude_array(minlat, maxlat, n):
    """ Returns an array (list) of latitudes in Mercator projection where
        the bounding box has the given minimum and maximum latitudes, and n
        is the number of points in the array. """
    # Get the minimum and maximum y coordinates, being careful to avoid
    # the singularities at lat = +- 90
    miny = _latToY((minlat, -89.9999)[minlat < -89.9999])
    maxy = _latToY((maxlat, 89.9999)[maxlat > 89.9999])
    dy = (maxy - miny) / n
    # Remember that the latitude axis is flipped
    return [_yToLat(miny + ((n - j - 1) + 0.5) * dy) for j in xrange(n)]

def _yToLat(y):
    """ Calculates the latitude (in degrees) of a given y coordinate.
        lat = arctan(sinh(y)) """
    return math.degrees(math.atan(math.sinh(y)))

def _latToY(lat):
    """ Calculates the y coordinate of a given latitude value in degrees.
        y = ln(tan(pi/4 + lat/2)) """
    return math.log(math.tan(math.pi / 4 + math.radians(lat) / 2))
    
def get_palette():
        """ returns a colour palette as a list of 256 values.  The first colour doesn't
            matter because it will be fully transparent (representing missing values).
            The second colour will be black. """

        # This palette was adapted from the Scientific Graphics Toolkit.  It is a rainbow
        # palette, showing large values as red and small values as blue
        return [[0, 0, 0], [0, 0, 0], [0, 0, 143], [0, 0, 146], [0, 0, 150], [0, 0, 154],
                [0, 0, 158], [0, 0, 162], [0, 0, 166], [0, 0, 170], [0, 0, 174], [0, 0, 178],
                [0, 0, 182], [0, 0, 186], [0, 0, 190], [0, 0, 193], [0, 0, 197], [0, 0, 201],
                [0, 0, 205], [0, 0, 209], [0, 0, 213], [0, 0, 217], [0, 0, 221], [0, 0, 225],
                [0, 0, 229], [0, 0, 233], [0, 0, 237], [0, 0, 241], [0, 0, 244], [0, 0, 248],
                [0, 0, 252], [0, 1, 255], [0, 3, 255], [0, 6, 255], [0, 9, 255], [0, 12, 255],
                [0, 16, 255], [0, 20, 255], [0, 24, 255], [0, 28, 255], [0, 31, 255], [0, 35, 255],
                [0, 39, 255], [0, 43, 255], [0, 47, 255], [0, 51, 255], [0, 55, 255], [0, 59, 255],
                [0, 63, 255], [0, 67, 255], [0, 71, 255], [0, 75, 255], [0, 79, 255], [0, 82, 255],
                [0, 86, 255], [0, 90, 255], [0, 94, 255], [0, 98, 255], [0, 102, 255], [0, 106, 255],
                [0, 110, 255], [0, 114, 255], [0, 118, 255], [0, 122, 255], [0, 126, 255], [0, 130, 255],
                [0, 133, 255], [0, 137, 255], [0, 141, 255], [0, 145, 255], [0, 149, 255], [0, 153, 255],
                [0, 157, 255], [0, 161, 255], [0, 165, 255], [0, 169, 255], [0, 173, 255], [0, 177, 255],
                [0, 180, 255], [0, 184, 255], [0, 188, 255], [0, 192, 255], [0, 196, 255], [0, 200, 255],
                [0, 204, 255], [0, 208, 255], [0, 212, 255], [0, 216, 255], [0, 220, 255], [0, 224, 255],
                [0, 228, 255], [0, 231, 255], [0, 235, 255], [0, 239, 255], [0, 243, 255], [0, 247, 255],
                [0, 251, 254], [1, 252, 252], [3, 253, 250], [5, 254, 248], [7, 255, 246], [11, 255, 242],
                [15, 255, 238], [19, 255, 234], [22, 255, 231], [26, 255, 227], [30, 255, 223],
                [34, 255, 219], [38, 255, 215], [42, 255, 211], [46, 255, 207], [50, 255, 203],
                [54, 255, 199], [58, 255, 195], [62, 255, 191], [66, 255, 187], [69, 255, 184],
                [73, 255, 180], [77, 255, 176], [81, 255, 172], [85, 255, 168], [89, 255, 164],
                [93, 255, 160], [97, 255, 156], [101, 255, 152], [105, 255, 148], [109, 255, 144],
                [113, 255, 140], [117, 255, 136], [120, 255, 133], [124, 255, 129], [128, 255, 125],
                [132, 255, 121], [136, 255, 117], [140, 255, 113], [144, 255, 109], [148, 255, 105],
                [152, 255, 101], [156, 255, 97], [160, 255, 93], [164, 255, 89], [168, 255, 85],
                [171, 255, 82], [175, 255, 78], [179, 255, 74], [183, 255, 70], [187, 255, 66],
                [191, 255, 62], [195, 255, 58], [199, 255, 54], [203, 255, 50], [207, 255, 46],
                [211, 255, 42], [215, 255, 38], [218, 255, 35], [222, 255, 31], [226, 255, 27],
                [230, 255, 23], [234, 255, 19], [238, 255, 15], [242, 255, 11], [246, 255, 7],
                [248, 253, 5], [250, 251, 3], [252, 249, 2], [254, 247, 0], [255, 243, 0],
                [255, 240, 0], [255, 236, 0], [255, 232, 0], [255, 228, 0], [255, 224, 0],
                [255, 220, 0], [255, 216, 0], [255, 212, 0], [255, 208, 0], [255, 204, 0],
                [255, 200, 0], [255, 196, 0], [255, 192, 0], [255, 189, 0], [255, 185, 0],
                [255, 181, 0], [255, 177, 0], [255, 173, 0], [255, 169, 0], [255, 165, 0],
                [255, 161, 0], [255, 157, 0], [255, 153, 0], [255, 149, 0], [255, 145, 0],
                [255, 142, 0], [255, 138, 0], [255, 134, 0], [255, 130, 0], [255, 126, 0],
                [255, 122, 0], [255, 118, 0], [255, 114, 0], [255, 110, 0], [255, 106, 0],
                [255, 102, 0], [255, 98, 0], [255, 94, 0], [255, 91, 0], [255, 87, 0],
                [255, 83, 0], [255, 79, 0], [255, 75, 0], [255, 71, 0], [255, 67, 0],
                [255, 63, 0], [255, 59, 0], [255, 55, 0], [255, 51, 0], [255, 47, 0],
                [255, 43, 0], [255, 40, 0], [255, 36, 0], [255, 32, 0], [255, 28, 0],
                [255, 24, 0], [255, 20, 0], [255, 16, 0], [255, 12, 0], [255, 8, 0], [253, 6, 0],
                [251, 4, 0], [249, 2, 0], [247, 0, 0], [243, 0, 0], [239, 0, 0], [235, 0, 0],
                [230, 0, 0], [226, 0, 0], [222, 0, 0], [217, 0, 0], [213, 0, 0], [209, 0, 0],
                [205, 0, 0], [200, 0, 0], [196, 0, 0], [191, 0, 0], [187, 0, 0], [183, 0, 0],
                [178, 0, 0], [174, 0, 0], [170, 0, 0], [165, 0, 0], [161, 0, 0], [157, 0, 0],
                [153, 0, 0], [148, 0, 0], [144, 0, 0], [140, 0, 0]]


def get_alpha_channel(opacity):
        """ returns a set of transparency values, one for each item in the colour palette.
            Pixel at index 0 is fully transparent. """
        alpha = int(2.55 * float(opacity))
        return [0] + [alpha for i in xrange(255)]

##### The routines below are copied or adapted from the minipng library, with
##### thanks to Dan Sandler: see http://dsandler.org/soft/python/minipng.py
##### for original source

##### There is a lot of string concatenation in this code: this could probably
##### be made more efficient using cStringIO

def _flatten(l):
    """Recursively flatten a list.  Ruby 1, Python 0."""
    r = []
    for x in l:
        if type(x) is types.ListType:
            r += _flatten(x)
        else:
            r.append(x)
    return r

# TODO: Internet Explorer does not seem to be able to display these images!  Something
# to do with the alpha channel?  Or the use of an indexed PNG?
def write_indexed_png(req, image, palette, alpha):
    """build_indexed_png(image, palette, alpha) -- Return a string containing a
       PNG representing the given image data
 
    req: the request object to which the picture is to be written
    image: a list of rows of pixels; each row is a sequence of pixels;
        each pixel is a single byte corresponding to an index in the palette
    palette: set of (R, G, B) triples giving the colour palette
    alpha: a list of alpha values (between 0 and 255) for each colour in the palette
    """

    PNG_CTYPE_INDEXED = 3
    width = len(image[0])
    height = len(image)

    req.content_type = PNG_FORMAT
    # Write the header bytes
    req.write(struct.pack("8B", 137, 80, 78, 71, 13, 10, 26, 10))
    
    ihdr_data = struct.pack(">LLBBBBB", width, height, 8, PNG_CTYPE_INDEXED, 0, 0, 0)
    
    pal_data = apply(struct.pack, ["%dB" % len(palette * 3)] + _flatten(palette))

    raw_data = apply(struct.pack, ["%dB" % ((width + 1) * height)]
        + _flatten([[0] + image[i] for i in xrange(height)]))
    
    req.write(build_png_chunk("IHDR", ihdr_data))
    req.write(build_png_chunk("PLTE", pal_data))
    req.write(build_png_chunk("tRNS", apply(struct.pack, ["%dB" % len(alpha)] + alpha)))
    req.write(build_png_chunk("IDAT", zlib.compress(raw_data)))
    req.write(build_png_chunk("IEND", ""))

    return

def build_png_chunk(chunk_type, data):
    """build_png_chunk(chunktype, data) -- Construct a PNG chunk.  (Internal.) """
    to_check = chunk_type + data
    return struct.pack('>L', len(data)) \
        + to_check \
        + struct.pack('>L', zlib.crc32(to_check))


class FakeReq(object):
    """ A fake Apache request object used in testing """
    def __init__(self, f=sys.stdout):
        self._f = f
        self.content_type = ''
    
    def write(self, s):
        self._f.write(s)
    
def main(argv=None):
    """ Simple test harness for generating pictures """
    # Construct a get_map request
    params = {}
    params['VERSION'] = WMS_VERSION
    params['LAYERS'] = './FOAM_20050422.0.nc/TMP'
    params['STYLES'] = ''
    params['CRS'] = CRS_LONLAT
    params['BBOX'] = '0,0,90,90'
    params['WIDTH'] = '100'
    params['HEIGHT'] = '100'
    params['FORMAT'] = PNG_FORMAT
    
    # Write output to stdout through a fake Apache request object
    try:
        get_map(FakeReq(sys.stdout), params, debug=True)
    except WMSException, e:
        sys.stderr.write(e.message + "\n")
    
if __name__ == "__main__":
    main()
