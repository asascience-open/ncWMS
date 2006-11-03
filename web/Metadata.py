# Metadata servlet - gets information for populating Godiva2 web page

from javax.servlet.http import HttpServlet

from WMS import FakeModPythonRequestObject, WMS

import web

class Metadata (WMS):
    def doGet(self,request,response):
        web.metadata(FakeModPythonRequestObject(request, response), self.getConfigFileLines())
