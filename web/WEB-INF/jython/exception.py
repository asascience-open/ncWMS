# Code relevant to exceptions
# TODO strongly type exceptions

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