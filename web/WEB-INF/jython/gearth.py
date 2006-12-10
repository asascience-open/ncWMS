# Contains code for displaying images in Google Earth
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import urllib

from wmsUtils import RequestParser
from wmsExceptions import WMSException
from getmap import _getBbox, _getGoogleEarthFormat

def gearth(req):
    """ Entry point with mod_python """
    doGEarth(req)

def doGEarth(req):
    """ Generates the top-level KML containing the network link """
    # TODO: can we raise exceptions in KML format?

    # Parse the URL arguments
    params = RequestParser(req.args)

    layers = params.getParamValue("layers").split(",")
    styles = params.getParamValue("styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")

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
    s.write("<NetworkLink>")
    s.write("<description>%s</description>" % "Description")
    s.write("<name>%s</name>" % "Name")
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

    s.write("</Folder>")
    s.write("</kml>")
    req.content_type = _getGoogleEarthFormat()
    req.headers_out["Content-Disposition"] = "inline; filename=%s_%s.kml" % tuple(layers[0].split("/"))
    req.write(s.getvalue())
    s.close()
    return
