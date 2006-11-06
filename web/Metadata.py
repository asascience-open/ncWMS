# Metadata servlet - gets information for populating Godiva2 web page

from javax.servlet.http import HttpServlet

from WMS import FakeModPythonRequestObject, WMS

import web

# Inherits from WMS so that we can use self.getConfigFileLines()
class Metadata (WMS):
    def doGet(self,request,response):
        web.metadata(FakeModPythonRequestObject(request, response), self.getConfigFileLines())
