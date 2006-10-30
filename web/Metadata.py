# Metadata servlet - gets information for populating Godiva2 web page

from javax.servlet.http import HttpServlet

from WMS import FakeModPythonRequestObject

import web

class Metadata (HttpServlet):
    def doGet(self,request,response):
        web.metadata(FakeModPythonRequestObject(request, response))

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")
