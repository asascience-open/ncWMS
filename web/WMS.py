# Entry point (Jython servlet) for the WMS

from javax.servlet.http import HttpServlet
from javax.servlet import ServletException
from java.net import URL

import ncWMS

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
 
    def write(self, str):
        """ Sets the content type and writes data to the client """
        self._response.setContentType(self.content_type)
        self._response.getWriter().write(str)

    def getOutputStream(self):
        """ Gets an OutputStream for writing binary data. """
        self._response.setContentType(self.content_type)
        return self._response.getOutputStream()

# Entry point for the Jython WMS servlet
class WMS (HttpServlet):

    def doGet(self,request,response):
        """ Perform the WMS operation """
        ncWMS.wms(FakeModPythonRequestObject(request, response))

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")
