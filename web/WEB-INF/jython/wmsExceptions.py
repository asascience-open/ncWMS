# Code relevant to exceptions

from config import XML_HEADER, WMS_VERSION

class WMSException:
    """ Exception class for WMS exceptions: if an exception of this type
        is thrown, it will be returned to the client as an XML document """
    def __init__(self, message=None, code=None):
        self.message = message
        self.code = code
    
    def write(self, f):
        """ writes this exception as XML using the f.write() method """
        f.write(XML_HEADER)
        f.write("<ServiceExceptionReport version=\"" + WMS_VERSION + "\"")
        f.write(" xmlns=\"http://www.opengis.net/ogc\"")
        f.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
        f.write(" xsi:schemaLocation=\"http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd\">")
        f.write("<ServiceException")
        if self.code:
            f.write(" code=\"" + self.code + "\"")
        f.write(">")
        if self.message:
            f.write(self.message)
        f.write("</ServiceException>")
        f.write("</ServiceExceptionReport>")

class OperationNotSupported(WMSException):
    """ Exception that is raised when a client requests an unsupported
       operation """
    def __init__(self, message=None):
        WMSException.__init__(self, message, "OperationNotSupported")

class InvalidCRS(WMSException):
    """ Exception that is raised when a client requests an unsupported
       CRS """
    def __init__(self, crs):
        WMSException.__init__(self, "The CRS \"" + crs + "\" is not supported by this server",
            "InvalidCRS")

class StyleNotDefined(WMSException):
    """ Exception that is raised when a client requests an unsupported
       style """
    def __init__(self, style):
        WMSException.__init__(self, "The style \"" + style + "\" is not supported by this server",
            "StyleNotDefined")

class InvalidFormat(WMSException):
    """ Exception that is raised when a client requests an unsupported
       image format """
    def __init__(self, format):
        WMSException.__init__(self, "The image format \"" + format +
            "\" is not supported by this server", "InvalidFormat")

class LayerNotDefined(WMSException):
    """ Exception that is raised when a client requests a layer that is not
        provided by the server """
    def __init__(self, layer):
        WMSException.__init__(self, "The layer \"" + layer +
            "\" is not provided by this server", "LayerNotDefined")

class MissingDimensionValue(WMSException):
    """ Exception that is raised when a client fails to provide a value
        for a dimension that does not have a default value """
    def __init__(self, dimName):
        WMSException.__init__(self, "You must provide a value for the " +
            dimName.upper() + " dimension", "MissingDimensionValue")

class InvalidDimensionValue(WMSException):
    """ Exception that is raised when a client provides an invalid
        value for a dimension """
    def __init__(self, dimName, value):
        WMSException.__init__(self, "The value \"" + str(value) +
            "\" is not valid for the " + dimName.upper() + " dimension",
            "InvalidDimensionValue")
