    # Entry point for the ncWMS (both CDAT and nj22 implementations)
import time, os

import ConfigParser
import wmsUtils
from wmsExceptions import *
from capabilities import getCapabilities
from getmap import getMap
from getfeatureinfo import getFeatureInfo
from getmetadata import getMetadata

def wms(req):
    """ Entry point with mod_python """
    # Config file is in this directory
    path = os.path.join(os.path.split(__file__)[0], "ncWMS.ini")
    doWms(req, path, time.time())

def doWms(req, configFile, lastUpdateTime):
    """ Does the WMS operation.
        req = mod_python request object (or FakeModPythonRequestObject
            from Jython servlet)
        configFile = location of the config file
        lastUpdateTime = time at which cache of data and metadata was last updated """
       
    try:
        # Read the config file
        config = _readConfig(configFile)
        args = req.args
        if req.args is None or req.args.strip() == "":
            # If there is no request string, return the front page
            args = "SERVICE=WMS&REQUEST=GetMetadata&item=frontpage"
        params = wmsUtils.RequestParser(args)
        service = params.getParamValue("service")
        request = params.getParamValue("request")
        if service != "WMS":
            raise WMSException("SERVICE parameter must be \"WMS\"")
        if request == "GetCapabilities":
            getCapabilities(req, params, config, lastUpdateTime)
        elif request == "GetMap":
            getMap(req, params, config)
        elif request == "GetFeatureInfo":
            if config.allowFeatureInfo:
                getFeatureInfo(req, params, config)
            else:
                raise OperationNotSupported("GetFeatureInfo")
        elif request == "GetMetadata":
            # This is a convenience extension to WMS for reading smaller
            # chunks of metadata
            getMetadata(req, config)
        else:
            raise WMSException("Invalid operation")
    except WMSException, e:
        req.content_type="text/xml"
        e.write(req)

class NcWMSConfig: pass
def _readConfig(configFile):
    """ returns an object containing the configuration of this server """
    config = ConfigParser.ConfigParser()
    config.read(configFile)
    configObj = NcWMSConfig()
    configObj.datasets = _getDatasets(config)
    configObj.title = config.get('server', 'title')
    configObj.url = config.get('server', 'url')
    configObj.allowFeatureInfo = config.getboolean('server', 'allowFeatureInfo')
    configObj.maxImageWidth = config.getint('server', 'maxImageWidth')
    configObj.maxImageHeight = config.getint('server', 'maxImageHeight')
    return configObj

class Dataset: pass # Simple class that will hold a dataset's title and location
def _getDatasets(config):
    """ Return dictionary of Dataset objects, keyed by the unique ID
        config = ConfigParser object """
    # TODO error checking
    datasets = {}
    for dsID in config.options('datasets'):
        line = config.get('datasets', dsID)
        els = line.split(",")
        dataset = Dataset()
        dataset.title = els[0].strip()
        dataset.location = els[1].strip()
        dataset.queryable = 0
        if len(els) > 2 and els[2].lower() == "true":
            dataset.queryable = 1
        datasets[dsID] = dataset
    return datasets
