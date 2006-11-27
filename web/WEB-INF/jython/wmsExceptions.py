# Code relevant to exceptions

import wmsUtils

class WMSException:
    """ Exception class for WMS exceptions: if an exception of this type
        is thrown, it will be returned to the client as an XML document """
    def __init__(self, message=None, code=None):
        self.message = message
        self.code = code
    
    def write(self, f):
        """ writes this exception as XML using the f.write() method """
        f.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        f.write("<ServiceExceptionReport version=\"" + wmsUtils.getWMSVersion() + "\"")
        f.write(" xmlns=\"http://www.opengis.net/ogc\"")
        f.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
        f.write(" xsi:schemaLocation=\"http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd\">")
        f.write("<ServiceException")
        if self.code:
            f.write(" code=\"" + self.code + "\"")
        f.write(">")
        if self.message:
            # Replace quotation marks with XML escape code
            f.write(self.message.replace("\"", "&quot;"))
        f.write("</ServiceException>")
        f.write("</ServiceExceptionReport>")

class OperationNotSupported(WMSException):
    """ Exception that is raised when a client requests an unsupported
       operation """
    def __init__(self, op):
        WMSException.__init__(self, "The " + op + " operation is not supported by this server",
            "OperationNotSupported")

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
       image or FeatureInfo format """
    def __init__(self, type, format, operation):
        WMSException.__init__(self, "The %s format \"%s\" is not supported by the %s operation" % (type, format, operation),
            "InvalidFormat")

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

class InvalidPoint(WMSException):
    """ Exception that is raised when a client provides an invalid
        value for I or J in GetFeatureInfo """
    def __init__(self, value=None):
        if value is None:
            WMSException.__init__(self, "Invalid integer for I or J", "InvalidPoint")
        else:
            WMSException.__init__(self, "Invalid I or J value " + str(value) +
                " in GetFeatureInfo", "InvalidPoint")

class InvalidUpdateSequence(WMSException):
    """ Exception that is raised when a client calls GetCapabilities
        with an updateSequence that is later than the current update
        sequence number """
    def __init__(self, value):
        WMSException.__init__(self, "The updateSequence value \"%s\" is greater than the current one" % value,
            "InvalidUpdateSequence")

class CurrentUpdateSequence(WMSException):
    """ Exception that is raised when a client calls GetCapabilities
        with an updateSequence that is equal to the current update
        sequence number """
    def __init__(self, value):
        WMSException.__init__(self, "The updateSequence value \"%s\" is equal to the current one" % value,
            "CurrentUpdateSequence")

class LayerNotQueryable(WMSException):
    """ Exception that is raised when a client tries to call
        GetFeatureInfo on a layer that is not queryable """
    def __init__(self, layer):
        WMSException.__init__(self, "The layer \"%s\" is not queryable" % layer,
            "LayerNotQueryable")
