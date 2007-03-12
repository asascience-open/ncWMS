# Contains code for displaying images in Google Earth
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import urllib
import sys

from wmsExceptions import WMSException
from getmap import _getBbox, _getDatasetAndVariableID

def doGEarth(req, params, config):
    """ Generates the KML for display in Google Earth """
    # Find the source of the requested data
    layers = params.getParamValue("layers").split(",")
    dataset, varID = _getDatasetAndVariableID(layers, config.datasets)
    vars = datareader.getAllVariableMetadata(dataset)

    # See if we're getting the top-level KML or not
    try:
        bbox = params.getParamValue("bbox")
        _doNestedKML(req, vars[varID], params)
    except WMSException:
        # No BBOX has been specified
        _doTopLevelKML(req, vars[varID], params, layers)
    return

def _doTopLevelKML(req, var, params, layers):
    """ Generates the top-level KML containing the network link """
    # TODO: can we raise exceptions in KML format?

    s = StringIO()
    s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    s.write("<kml xmlns=\"http://earth.google.com/kml/2.1\">")
    s.write("<NetworkLink>")
    s.write("<name>%s</name>" % var.title)
    s.write("<description>%s</description>" % var.abstract)
    s.write("<Region>")
    s.write("<LatLonAltBox>")
    bboxEls = tuple([str(f) for f in var.bbox])
    s.write("<west>%s</west><south>%s</south><east>%s</east><north>%s</north>" % bboxEls)
    s.write("</LatLonAltBox>")
    s.write("<Lod><minLodPixels>0</minLodPixels><maxLodPixels>-1</maxLodPixels></Lod>")
    s.write("</Region>")
    s.write("<Link>")
    # TODO: is there a better way of reconstructing this URL?
    s.write("<href>http://%s%s?%s" % (req.server.server_hostname, req.unparsed_uri, params.queryString.replace("&", "&amp;")))
    s.write("&amp;BBOX=%s,%s,%s,%s</href>" % bboxEls)
    s.write("<viewRefreshMode>onRegion</viewRefreshMode>")
    s.write("</Link>")
    s.write("</NetworkLink>")

    s.write("</kml>")
    req.content_type = _getGoogleEarthFormat()
    req.headers_out["Content-Disposition"] = "inline; filename=%s.kml" % layers[0].replace("/", "_")
    req.write(s.getvalue())
    s.close()
    return

def _doNestedKML(req, var, params):
    """ Outputs the KML at a point in the hierarchy """
    bboxEls =  tuple([str(f) for f in _getBbox(params)])

    s = StringIO()
    s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    s.write("<kml xmlns=\"http://earth.google.com/kml/2.1\">")
    s.write("<Document>")
    # First the region for which this document is active
    s.write("<Region>")
    s.write("<LatLonAltBox>")
    s.write("<west>%s</west><south>%s</south><east>%s</east><north>%s</north>" % bboxEls)
    s.write("</LatLonAltBox>")
    s.write("<Lod><minLodPixels>0</minLodPixels><maxLodPixels>-1</maxLodPixels></Lod>")
    s.write("</Region>")

    # The overlay for this level of detail
    s.write("<GroundOverlay>")
    # TODO What should the drawOrder value really be?
    s.write("<drawOrder>21</drawOrder>")
    s.write("<Icon><href>http://%s%s?SERVICE=WMS&amp;REQUEST=GetMap&amp;VERSION=1.3.0" % (req.server.server_hostname, req.unparsed_uri))
    s.write("&amp;LAYERS=%s&amp;STYLES=%s" % (params.getParamValue("layers"), params.getParamValue("styles")))
    s.write("&amp;WIDTH=256&amp;HEIGHT=256&amp;FORMAT=image/png&amp;CRS=CRS:84")
    zValue = params.getParamValue("elevation", "")
    if zValue != "":
        s.write("&amp;ELEVATION=%s" % zValue)
    tValue = params.getParamValue("time", "")
    if tValue != "":
        s.write("&amp;TIME=%s" % tValue)
    s.write("&amp;BBOX=%s,%s,%s,%s</href></Icon>" % bboxEls)
    s.write("<LatLonBox>")
    s.write("<west>%s</west><south>%s</south><east>%s</east><north>%s</north>" % bboxEls)
    s.write("</LatLonBox>")
    s.write("</GroundOverlay>")
    s.write("</Document>")
    s.write("</kml>")

    req.content_type = "text/xml" #_getGoogleEarthFormat()
    req.write(s.getvalue())
    s.close()
    return

def doGEarth2(req, params, config):
    """ Generates the top-level KML containing the network link """
    # TODO: can we raise exceptions in KML format?

    layers = params.getParamValue("layers").split(",")
    styles = params.getParamValue("styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")
    # Find the source of the requested data
    dataset, varID = _getDatasetAndVariableID(layers, config.datasets)
    vars = datareader.getAllVariableMetadata(dataset)

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise WMSException("You may only request a single value of ELEVATION")

    tValue = params.getParamValue("time", "")
    if len(tValue.split(",")) > 1 or len(tValue.split("/")) > 1:
        raise WMSException("You may only request a single value of TIME")

    scale = params.getParamValue("scale", "")

    s = StringIO()
    s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    s.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">")
    s.write("<Folder>")
    # Make this a radiofolder so only one image can be loaded at once
    s.write("<Style><ListStyle>")
    s.write("<listItemType>radioFolder</listItemType>")
    s.write("</ListStyle></Style>")
    s.write("<name>%s</name>" % vars[varID].title)
    s.write("<description>%s</description>" % vars[varID].abstract)

    s.write("<NetworkLink>")
    s.write("<name>fixed scale</name>")
    s.write("<visibility>1</visibility>")
    s.write("<open>0</open>")
    s.write("<refreshVisibility>0</refreshVisibility>")
    s.write("<flyToView>0</flyToView>")

    s.write("<Url>")
    # We now simply call GetMap with output format=KML
    # TODO: get the path the WMS.py properly
    s.write("<href>http://%s/ncWMS/WMS.py?SERVICE=WMS&amp;REQUEST=GetMap&amp;VERSION=1.3.0&amp;FORMAT=%s&amp;LAYERS=%s&amp;STYLES=%s&amp;ELEVATION=%s&amp;TIME=%s&amp;WIDTH=500&amp;HEIGHT=500&amp;SCALE=%s</href>" %
        (req.server.server_hostname, _getGoogleEarthFormat(),
        ",".join(layers), ",".join(styles), zValue, tValue, scale))
    s.write("<refreshInterval>1</refreshInterval>")
    s.write("<viewRefreshMode>onStop</viewRefreshMode>")
    s.write("<viewRefreshTime>0</viewRefreshTime>")
    s.write("</Url>")

    s.write("</NetworkLink>")

    # Now create a folder for the auto-scaled picture
    # TODO: code repeated from above
    s.write("<NetworkLink>")
    s.write("<name>auto scale</name>")
    s.write("<visibility>0</visibility>")
    s.write("<open>0</open>")
    s.write("<refreshVisibility>0</refreshVisibility>")
    s.write("<flyToView>0</flyToView>")

    s.write("<Url>")
    # We now simply call GetMap with output format=KML
    # TODO: get the path the WMS.py properly
    s.write("<href>http://%s/ncWMS/WMS.py?SERVICE=WMS&amp;REQUEST=GetMap&amp;VERSION=1.3.0&amp;FORMAT=%s&amp;LAYERS=%s&amp;STYLES=%s&amp;ELEVATION=%s&amp;TIME=%s&amp;WIDTH=500&amp;HEIGHT=500</href>" %
        (req.server.server_hostname, _getGoogleEarthFormat(),
        ",".join(layers), ",".join(styles), zValue, tValue))
    s.write("<refreshInterval>1</refreshInterval>")
    s.write("<viewRefreshMode>onStop</viewRefreshMode>")
    s.write("<viewRefreshTime>0</viewRefreshTime>")
    s.write("</Url>")

    s.write("</NetworkLink>")

    s.write("</Folder>")
    s.write("</kml>")
    req.content_type = _getGoogleEarthFormat()
    req.headers_out["Content-Disposition"] = "inline; filename=%s_%s.kml" % tuple(layers[0].split("/"))
    req.write(s.getvalue())
    s.close()
    return
