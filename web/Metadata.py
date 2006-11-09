# Metadata servlet - gets information for populating Godiva2 web page

from javax.servlet.http import HttpServlet

from WMS import FakeModPythonRequestObject, getConfigFileLines

import web

class Metadata(HttpServlet):
    def doGet(self,request,response):
        web.metadata(FakeModPythonRequestObject(request, response), getConfigFileLines(self))
