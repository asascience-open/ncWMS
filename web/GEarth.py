# Servlet entry point for KML generator

from javax.servlet.http import HttpServlet
from javax.servlet import ServletException

import gearth
from WMS import FakeModPythonRequestObject

class GEarth (HttpServlet):
    def doGet(self, request, response):
        gearth.doGEarth(FakeModPythonRequestObject(request, response))

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")