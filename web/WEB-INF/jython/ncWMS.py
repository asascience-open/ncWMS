    # Entry point for the ncWMS (both CDAT and nj22 implementations)
import time, os

import ConfigParser
import wmsUtils
from wmsExceptions import *
from capabilities import getCapabilities
from getmap import getMap
from getfeatureinfo import getFeatureInfo
from getmetadata import getMetadata
from gearth import doGEarth2

def wms(req):
    """ Entry point with mod_python """
    # Config file is in the same directory as this script
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
        # Parse the URL parameters
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
        elif request == "GetKML":
            # Used to get the top-level KML document
            doGEarth2(req, params, config)
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
    configObj.abstract = config.get('server', 'abstract')
    configObj.url = config.get('server', 'url')
    configObj.keywords = config.get('server', 'keywords').split(',')
    configObj.allowFeatureInfo = config.getboolean('server', 'allowFeatureInfo')
    configObj.maxImageWidth = config.getint('server', 'maxImageWidth')
    configObj.maxImageHeight = config.getint('server', 'maxImageHeight')
    configObj.contactName = config.get('contact', 'name')
    configObj.contactOrg = config.get('contact', 'organization')
    configObj.contactTel = config.get('contact', 'tel')
    configObj.contactEmail = config.get('contact', 'email')
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
        dataset.queryable = 1
        if len(els) > 2 and els[2].lower() == "false":
            dataset.queryable = 0
        # dataset.reader is set to an empty string: this will cause 
        # the default data reader to be used
        dataset.reader = ""
        datasets[dsID] = dataset

    # Now override the datareaders for those datasets that are specified
    for dsID in config.options('datareaders'):
        datasets[dsID].reader = config.get('datareaders', dsID)

    return datasets
