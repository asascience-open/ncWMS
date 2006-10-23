# testJythonServlet source file (Jython servlet)

from javax.servlet.http import HttpServlet
from javax.servlet import ServletException

from ncWMS import wms

class FakeApacheRequest:
    """ Class that wraps an HttpServletResponse to provide the necessary methods of
        an Apache request (req) object.  This allows us to use identical code
        for both mod_python and Jython servlet implementations """

    def __init__(self, request, response):
        self._response = response
        self._writer = response.getWriter()
        self.args = request.getQueryString()
        # We would like content_type to be a class property but this is
        # not supported in Python 2.1
        self.content_type = "text/plain"
 
    def write(self, str):
        """ Sets the content type and writes data to the client """
        self._response.setContentType(self.content_type)
        self._writer.write(str)

# Entry point for the Jython WMS servlet
class WMS (HttpServlet):

    def doGet(self,request,response):
        """ Perform the WMS operation """
        wms(FakeApacheRequest(request, response))

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")
