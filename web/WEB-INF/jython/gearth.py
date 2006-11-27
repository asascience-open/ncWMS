# Contains code for displaying images in Google Earth
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import urllib

from wmsUtils import RequestParser
from wmsExceptions import WMSException

def gearth(req):
    """ Entry point with mod_python """
    doGEarth(req)

def doGEarth(req):
    """ Generates the KML """
    # TODO: can we raise exceptions in KML format?

    # Parse the URL arguments
    params = RequestParser(req.args)
    # We generate the KML in two stages
    stage = params.getParamValue("stage", "1")
    if stage == "1":
        doStageOne(req, params)
    elif stage == "2":
        doStageTwo(req, params)
    else:
        raise WMSException("STAGE must be \"1\" or \"2\"")

def doStageOne(req, params):
    """ Generates the top-level KML containing the network link """

    layers = params.getParamValue("layers").split(",")
    styles = params.getParamValue("styles").split(",")
    # We must either have one style per layer or else an empty parameter: "STYLES="
    if len(styles) != len(layers) and styles != ['']:
        raise WMSException("You must request exactly one STYLE per layer, or use"
           + " the default style for each layer with STYLES=")

    # Get the URL to the Web Map Server
    url = urllib.quote(params.getParamValue("url"))

    zValue = params.getParamValue("elevation", "")
    if len(zValue.split(",")) > 1 or len(zValue.split("/")) > 1:
        raise WMSException("You may only request a single value of ELEVATION")

    tValue = params.getParamValue("time", "")
    if len(tValue.split(",")) > 1 or len(tValue.split("/")) > 1:
        # TODO: support animations
        raise WMSException("You may only request a single value of TIME")

    s = StringIO()
    s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    s.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">")
    s.write("<Folder>")

    for i in xrange(len(layers)):
        # Serve each layer as a separate NetworkLink
        # TODO: is this right? Isn't a single NL sufficient?
        s.write("<NetworkLink>")
        s.write("<description>%s</description>" % layers[i])
        s.write("<name>%s</name>" % layers[i])
        s.write("<visibility>1</visibility>")
        s.write("<open>0</open>")
        s.write("<refreshVisibility>0</refreshVisibility>")
        s.write("<flyToView>0</flyToView>")

        s.write("<Url>")
        s.write("<href>http://%s%s?STAGE=2&amp;URL=%s&amp;LAYERS=%s&amp;STYLES=%s&amp;ELEVATION=%s&amp;TIME=%s</href>" %
            (req.server.server_hostname, req.unparsed_uri.split("?")[0],
            url, ",".join(layers), ",".join(styles), zValue, tValue))
        s.write("<refreshInterval>1</refreshInterval>")
        s.write("<viewRefreshMode>onStop</viewRefreshMode>")
        s.write("<viewRefreshTime>0</viewRefreshTime>")
        s.write("</Url>")

        s.write("</NetworkLink>")

    s.write("</Folder>")
    s.write("</kml>")
    req.content_type = "text/xml"
    # TODO: set a suggested file name
    req.write(s.getvalue())
    s.close()
    return

def doStageTwo(req, params):
    """ Generates the second-level KML containing the link to the image """

    # Get the URL to the Web Map Server
    url = params.getParamValue("url")

    s = StringIO()
    s.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    s.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">")

    s.write("<Folder>")
    s.write("<visibility>1</visibility>")
    # TODO: do a GroundOverlay for each layer
    s.write("<GroundOverlay>")
    s.write("<description>Description goes here</description>")
    s.write("<name>Ocean data</name>")
    s.write("<visibility>1</visibility>")
    s.write("<Icon><href>%s</href></Icon>" % url)
    s.write("<LatLonBox id=\"1\">")
    s.write("<north>%s</north><south>%s</south><east>%s</east><west>%s</west>" %
        ("n", "s", "e", "w"))
    s.write("<rotation>0</rotation>")
    s.write("</LatLonBox>")
    s.write("</GroundOverlay>")
    s.write("</Folder>")

    s.write("</kml>")

    req.content_type = "text/xml"
    req.write(s.getvalue())
    s.close()
    return
