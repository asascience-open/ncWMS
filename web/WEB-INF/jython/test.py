# simple script for testing and profiling operations

import profile, sys

from java.net import URL
from java.io import FileOutputStream

from ncWMS import wms

class FakeModPythonServerObject:
    """ Class that fakes up the req.server mod_python object """
    def __init__(self, hostname):
        self.server_hostname = hostname

class FakeModPythonRequestObject:
    """ Class that wraps an HttpServletResponse to provide the necessary
        methods and properties of a mod_python request (req) object. 
        This allows us to use identical code for both mod_python and
        Jython servlet implementations """

    def __init__(self, queryString):
        self.args = queryString
        # We would like content_type to be a class property but this is
        # not supported in Python 2.1
        self.content_type = "text/plain"
        reqURL = URL("http://myhost/mywms/")
        self.server = FakeModPythonServerObject("%s:%d" % (reqURL.getHost(), reqURL.getPort()))
        self.unparsed_uri = str(reqURL.getPath())
 
    def write(self, str):
        """ Sets the content type and writes data to the client """
        sys.stdout.write(str)

    def getOutputStream(self):
        """ Gets an OutputStream for writing binary data. """
        return FileOutputStream("test.png")

cap = "SERVICE=WMS&REQUEST=GetCapabilities"
map = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&LAYERS=OSTIA/sst_foundation&CRS=CRS:84&" \
        + "FORMAT=image/png&BBOX=-180,-90,180,90&STYLES=&WIDTH=256&HEIGHT=256"

def runWMS():
    fakeReq = FakeModPythonRequestObject(map)
    wms(fakeReq)

if __name__ == '__main__':
    profile.run("runWMS()")