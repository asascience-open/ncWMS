# Implements the GetFeatureInfo operation.
# WMS1.3.0 spec is a little ambiguous regarding this operation.
# The spec implies that we need all of the GetMap parameters except
# VERSION and REQUEST.  However, the spec also defines QUERY_LAYERS,
# which seems to duplicate LAYERS from GetMap.  I can see no use
# for GetMap's STYLES, FORMAT, TRANSPARENT or BGCOLOR parameters here.

import sys

if sys.platform.startswith("java"):
    # We're running on Jython and we need a function to convert fill
    # values to "none"
    from java.lang import Float
    def _checkFillValue(datavalue, fillvalue):
        """ if datavalue == fillvalue, returns None, else returns datavalue """
        if datavalue == Float(fillvalue).floatValue():
            return None
        else:
            return datavalue
else:
    def _checkFillValue(datavalue, fillvalue):
        """ if datavalue == fillvalue, returns None, else returns datavalue """
        if datavalue == fillvalue:
            return None
        else:
            return datavalue
        
import iso8601
from wmsExceptions import *
from getmap import _getBbox, _getGrid, _checkVersion, _getDatasetAndVariableID, _getTIndices, _getFillValue

from org.jfree.chart import ChartFactory, ChartUtilities
from org.jfree.data.time import TimeSeries, TimeSeriesCollection, Millisecond
from java.util import Date
from java.lang import Double

def getSupportedFormats():
    """ returns list of output formats supported by GetFeatureInfo """
    return ["text/xml", "image/png"]

def getFeatureInfo(req, params, config):
    """ The GetFeatureInfo operation.
       req = mod_python request object (or FakeModPythonRequestObject from Jython servlet)
       params = ncWMS.RequestParser object containing the request parameters
       config = configuration object """
    
    _checkVersion(params)
    bbox = _getBbox(params)
    grid = _getGrid(params, bbox, config)

    query_layers = params.getParamValue("query_layers").split(",")
    if len(query_layers) > 1:
        raise WMSException("You may only perform GetFeatureInfo on a single layer")

    info_format = params.getParamValue("info_format")
    if info_format not in getSupportedFormats():
        raise InvalidFormat("info", info_format, "GetFeatureInfo")

    exception_format = params.getParamValue("exceptions", "XML")
    if exception_format != "XML":
        raise InvalidFormat("exception", exception_format, "GetFeatureInfo")

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise WMSException("You may only request a single value of ELEVATION")

    # Get the i and j coordinate of the feature in pixels
    try:
        i = int(params.getParamValue("i"))
        j = int(params.getParamValue("j"))
        if i < 0 or i >= grid.width:
            raise InvalidPoint(i)
        if j < 0 or j >= grid.height:
            raise InvalidPoint(j)
    except ValueError:
        raise InvalidPoint()

    feature_count = params.getParamValue("feature_count", "1")
    try:
        if int(feature_count) != 1:
            raise WMSException("Can only provide FeatureInfo for 1 feature per layer")
    except ValueError:
        raise WMSException("Invalid integer for FEATURE_COUNT")

    # Get the longitude and latitude of the data point
    lon, lat = grid.getLonLat(i, j)

    # Read the data point
    dataset, varID = _getDatasetAndVariableID(query_layers, config.datasets)
    if not dataset.queryable:
        raise LayerNotQueryable(query_layers[0])
    # Get the index along the time axis
    # Get the metadata
    var = dataset.variables[varID]
    tIndices = _getTIndices(var, params)

    # Read the data points
    datavalues = [dataset.read(var, t, zValue, [lat], [lon], _getFillValue())[0] for t in tIndices]
    # Check for fill values, replacing with "None"
    datavalues = [_checkFillValue(val, _getFillValue()) for val in datavalues]

    req.content_type = info_format

    # Output in simple XML. TODO make this GeoRSS and/or KML?
    if info_format == "text/xml":
        req.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        req.write("<FeatureInfoResponse>")
        req.write("<longitude>%f</longitude>" % lon)
        req.write("<latitude>%f</latitude>" % lat)
        for i in xrange(len(tIndices)):
            req.write("<FeatureInfo>")
            if len(var.tvalues) > 0:
                tval = var.tvalues[tIndices[i]]
                req.write("<time>%s</time>" % iso8601.tostring(tval))
            if datavalues[i] is None:
                req.write("<value>none</value>") 
            else:
                req.write("<value>%s</value>" % str(datavalues[i]))
            req.write("</FeatureInfo>")
        req.write("</FeatureInfoResponse>")
    else:
        # Output the data as a PNG JFreeChart (most useful for timeseries)
        # TODO: this needs to be separated to preserve pure Python
        ts = TimeSeries("Data", Millisecond)
        for i in xrange(len(tIndices)):
            tval = var.tvalues[tIndices[i]]
            tvalLong = Double(tval * 1000).longValue()
            date = Date(tvalLong)
            ts.add(Millisecond(date), datavalues[i])
        xydataset = TimeSeriesCollection()
        xydataset.addSeries(ts)
        xydataset.setDomainIsPointsInTime(1)

        # Creat a chart with no legend, tooltips or URLs
        title = "Lon: %f, Lat: %f" % (lon, lat)
        yLabel = "%s (%s)" % (var.title, vars[varID].units)
        chart = ChartFactory.createTimeSeriesChart(title, "Date / time", yLabel, xydataset, 0, 0, 0)
        # Output the chart. TODO: control the plot size
        ChartUtilities.writeChartAsPNG(req.getOutputStream(), chart, 400, 300)

    return