# Code for generating the Capabilities document
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

import config

def getCapabilities(params):
    """ Returns the Capabilities document """
    version = params.getParamValue("version", "")
    format = params.getParamValue("format", "")
    updatesequence = params.getParamValue("updatesequence", "")    
    # TODO: deal with version, format and updatesequence
    
    output = StringIO()
    output.write(config.XML_HEADER)
    output.write("<WMS_Capabilities version=\"" + config.WMS_VERSION + "\" xmlns=\"http://www.opengis.net/wms\"")
    output.write(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
    # The next two lines should be commented out if you wish to load this document
    # in Cadcorp SIS from behind the University of Reading firewall
    output.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    output.write(" xsi:schemaLocation=\"http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd\"")
    output.write(">")
    
    output.write("<Service>")
    output.write("<Name>WMS</Name>")
    output.write("<Title>%s</Title>" % config.title)
    output.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"%s\"/>" % config.url)
    output.write("<Fees>none</Fees>")
    output.write("<AccessConstraints>none</AccessConstraints>")
    output.write("<LayerLimit>%s</LayerLimit>" % str(config.LAYER_LIMIT))
    output.write("<MaxWidth>%s</MaxWidth>" % str(config.MAX_IMAGE_WIDTH))
    output.write("<MaxHeight>%s</MaxHeight>" % str(config.MAX_IMAGE_HEIGHT))
    output.write("</Service>")
    
    output.write("<Capability>")
    output.write("<Request>")
    output.write("<GetCapabilities>")
    output.write("<Format>text/xml</Format>")
    # TODO: detect the full server context path
    #output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
    #    req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    output.write("</GetCapabilities>")
    output.write("<GetMap>")
    for format in config.SUPPORTED_IMAGE_FORMATS:
        output.write("<Format>%s</Format>" % format)
    #output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"http://" +
    #    req.server.server_hostname + "/godiva2cdat/godiva2.py/wms?\"/></Get></HTTP></DCPType>")
    output.write("</GetMap>")
    output.write("</Request>")
    # TODO: support more exception types
    output.write("<Exception>")
    for ex_format in config.SUPPORTED_EXCEPTION_FORMATS:
        output.write("<Format>%s</Format>" % ex_format)
    output.write("</Exception>")
    
    # Now for the layers
    # We recurse through the directory structure to find datasets that contain layers
    # TODO output_layers(req, ".")
    
    output.write("</Capability>")
    output.write("</WMS_Capabilities>")

    capdoc = output.getvalue()
    output.close() # Free the buffer
    return capdoc
