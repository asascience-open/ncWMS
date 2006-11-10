# Entry point (Jython servlet) for the WMS

from javax.servlet.http import HttpServlet
from javax.servlet import GenericServlet, ServletException
from java.io import InputStreamReader, BufferedReader
from java.net import URL
from java.util import Timer, TimerTask

from org.apache.log4j import PropertyConfigurator, Logger

from ucar.nc2.dataset import NetcdfDatasetCache

import ncWMS, config

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

    logger = Logger.getLogger("uk.ac.rdg.resc.ncwms.WMS")
    timer = None

    def init(self, cfg=None):
        """ This method will be called twice, once with a cfg parameter
            and once without """
        if cfg is None:
            HttpServlet.init(self)
        else:
            HttpServlet.init(self, cfg)
        # These are the things we only do once
        if WMS.timer is None:
            WMS.timer = Timer(1) # timer is a daemon
            # Load the Log4j configuration file
            prefix = self.getServletContext().getRealPath("/")
            file = self.getInitParameter("log4j-init-file")
            if file is not None:
                PropertyConfigurator.configure(prefix + file)
            WMS.logger.debug("Initialized logging system")
            # Initialize the cache of NetcdfDatasets
            NetcdfDatasetCache.init()
            WMS.logger.debug("Initialized NetcdfDatasetCache")
            # Start a timer that will clear the cache at regular intervals
            # so that NcML aggregations are reloaded
            intervalInMs = int(config.CACHE_REFRESH_INTERVAL * 60 * 1000)
            WMS.timer.scheduleAtFixedRate(CacheWiper(), intervalInMs, intervalInMs)
            WMS.logger.debug("Initialized NetcdfDatasetCache refresher")
            WMS.logger.debug("ncWMS Servlet initialized")

    def destroy(self):
        NetcdfDatasetCache.exit()
        if WMS.timer is not None:
            WMS.timer.cancel()
        WMS.logger.debug("ncWMS Servlet destroyed")

    def doGet(self,request,response):
        """ Perform the WMS operation """
        WMS.logger.debug("GET operation called")
        ncWMS.wms(FakeModPythonRequestObject(request, response), getConfigFileLines(self))

    def doPost(self,request,response):
        raise ServletException("POST method is not supported on this server")

def getConfigFileLines(servlet):
    """ Gets the lines of text (ignoring comments) from the config file """
    # First get the location of the config file
    # TODO: check last modified date to see if we need to read again
    # TODO: get the config file location from a servlet init parameter
    configFileURL = servlet.getServletContext().getResource("/WEB-INF/conf/config.txt")
    # TODO: check for configFileURL == None
    configReader = BufferedReader(InputStreamReader(configFileURL.openStream()))
    lines = []
    done = 0
    while not done:
        line = configReader.readLine()
        if line is None:
            done = 1
        elif line.strip() != "" and not line.startswith('#'):
            # Ignore blank lines and comment lines
            lines.append(line)
    configReader.close()
    return lines

class CacheWiper(TimerTask):
    """ Clears the NetcdfDatasetCache at regular intervals """
    def __init__(self):
        self.logger = Logger.getLogger("uk.ac.rdg.resc.ncwms.CacheWiper")
    def run(self):
        NetcdfDatasetCache.clearCache(1)
        self.logger.debug("Cleared cache")
        