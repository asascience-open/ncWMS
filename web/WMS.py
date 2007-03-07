# Entry point (Jython servlet) for the WMS

from javax.servlet.http import HttpServlet
from javax.servlet import ServletException
from java.net import URL
from java.lang import RuntimeException

from org.apache.log4j import Logger

from uk.ac.rdg.resc.ncwms.exceptions import *
from uk.ac.rdg.resc.ncwms.config import Config

import time
import ncWMS
from wmsExceptions import *

class FakeModPythonServerObject:
    """ Class that fakes up the req.server mod_python object """
    def __init__(self, hostname):
        self.server_hostname = hostname

class FakeModPythonRequestObject:
    """ Class that wraps an HttpServletResponse to provide the necessary
        methods and properties of a mod_python request (req) object. 
        This allows us to use identical code for both mod_python and
        Jython servlet implementations """

    def __init__(self, request, response):
        self._response = response
        self.args = request.getQueryString()
        # We would like content_type to be a class property but this is
        # not supported in Python 2.1
        self.content_type = "text/plain"
        reqURL = URL(request.getRequestURL().toString())
        self.server = FakeModPythonServerObject("%s:%d" % (reqURL.getHost(), reqURL.getPort()))
        self.unparsed_uri = str(reqURL.getPath())
        self.headers_out = {} # Dictionary of HTTP headers
        self.headers_set = 0

    def _setHeaders(self):
        """ Sets the content type and other HTTP headers.  Does nothing
            in subsequent invocations """
        if not self.headers_set:
            self.headers_set = 1
            for key in self.headers_out.keys():
                self._response.setHeader(key, self.headers_out[key])
            self._response.setContentType(self.content_type)
 
    def write(self, str):
        """ Writes data to the client."""
        self._setHeaders()
        self._response.getWriter().write(str)

    def getOutputStream(self):
        """ Gets an OutputStream for writing binary data. """
        self._setHeaders()
        return self._response.getOutputStream()

# Entry point for the Jython WMS servlet
class WMS (HttpServlet):

    logger = Logger.getLogger("uk.ac.rdg.resc.ncwms.WMS")

    def init(self, cfg=None):
        """ This method will be called twice, once with a cfg parameter
            and once without """
        if cfg is None:
            HttpServlet.init(self)
        else:
            HttpServlet.init(self, cfg)
        # The config object has been created by the GlobalFilter
        self.config = self.servletContext.getAttribute("config")
        WMS.logger.debug("ncWMS Servlet initialized")

    def destroy(self):
        WMS.logger.debug("ncWMS Servlet destroyed")

    def doGet(self, request, response):
        """ Perform the WMS operation """
        WMS.logger.debug("GET operation called")
        req = FakeModPythonRequestObject(request, response)
        # We make sure we catch all the exceptions from the Java code
        # and re-raise as Python exceptions so that they are correctly
        # translated into XML and returned to the client
        try:
            try:
                # Do the WMS operation
                # TODO: get time of last metadata update somehow
                ncWMS.doWms(req, self.config)
            except InvalidDimensionValueException, e:
                raise InvalidDimensionValue(e.getDimName(), e.getValue())
            except MissingDimensionValueException, e:
                raise MissingDimensionValue(e.getDimName())
            except WMSExceptionInJava, e:
                raise WMSException(e.getMessage())
            except RuntimeException, e:
                self.logger.error(e.getMessage(), e)
                raise WMSException("%s: %s" % (e.getClass().getName(), e.getMessage()))
        except WMSException, e:
            req.content_type="text/xml"
            e.write(req)

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")

        