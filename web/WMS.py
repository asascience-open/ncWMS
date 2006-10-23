# testJythonServlet source file (Jython servlet)

from javax.servlet.http import HttpServlet
from javax.servlet import ServletException
import sys

from ncWMS import doWMS

# Entry point for the WMS server
class WMS (HttpServlet):

    def doGet(self,request,response):
        queryString = request.getQueryString()
        writer = response.getWriter()
        # Perform the WMS operation
        doWMS(queryString, writer)

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")
