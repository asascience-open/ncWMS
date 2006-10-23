# Code for generating the Capabilities document
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from utils import getParamValue
from config import *

def getCapabilities(params):
    """ Implements the GetCapabilities operation """
    version = getParamValue(params, "version", "")
    format = getParamValue(params, "format", "")
    updatesequence = getParamValue(params, "updatesequence", "")    
    # We ignore the version and format arguments
    # TODO: deal with updatesequence
    
    # TODO: req.content_type = "text/xml"
    output = StringIO()
    output.write(XML_HEADER)
    output.write("<WMS_Capabilities version=\"" + WMS_VERSION + "\" xmlns=\"http://www.opengis.net/wms\"")
    output.write(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
    # The next two lines should be commented out if you wish to load this document
    # in Cadcorp SIS from behind the University of Reading firewall
    output.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    output.write(" xsi:schemaLocation=\"http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd\"")
    output.write(">")
    
    output.write("<Service>")
    output.write("<Name>WMS</Name>")
    output.write("<Title>CDAT-based Web Map Server</Title>")
    output.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"http://www.nerc-essc.ac.uk\"/>")
    output.write("<Fees>none</Fees>")
    output.write("<AccessConstraints>none</AccessConstraints>")
    output.write("<LayerLimit>" + str(LAYER_LIMIT) + "</LayerLimit>")
    output.write("<MaxWidth>" + str(MAX_IMAGE_WIDTH) + "</MaxWidth>")
    output.write("<MaxHeight>" + str(MAX_IMAGE_HEIGHT) + "</MaxHeight>")
    output.write("</Service>")
    
    output.write("<Capability>")
    output.write("<Request>")
    output.write("<GetCapabilities>")
    output.write("<Format>text/xml</Format>")
    # TODO: detect the full server context path
    output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
        req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    output.write("</GetCapabilities>")
    output.write("<GetMap>")
    for format in SUPPORTED_IMAGE_FORMATS:
        output.write("<Format>" + format + "</Format>")
    output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
        req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    output.write("</GetMap>")
    output.write("</Request>")
    # TODO: support more exception types
    output.write("<Exception>")
    for ex_format in SUPPORTED_EXCEPTION_FORMATS:
        output.write("<Format>" + ex_format + "</Format>")
    output.write("</Exception>")
    
    # Now for the layers
    # We recurse through the directory structure to find datasets that contain layers
    # TODO output_layers(req, ".")
    
    output.write("</Capability>")
    output.write("</WMS_Capabilities>")

    return output
