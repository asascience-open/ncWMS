    # Entry point for the ncWMS (both CDAT and nj22 implementations)
import time

import config
import wmsUtils
from wmsExceptions import *
from capabilities import getCapabilities
from getmap import getMap
from getfeatureinfo import getFeatureInfo
from getmetadata import getMetadata

class Dataset: pass # Simple class that will hold a dataset's title and location

def wms(req):
    """ Entry point with mod_python """
    # TODO: read a config file to get the datasets
    doWms(req, [], time.time())

def doWms(req, datasetLines, lastUpdateTime):
    """ Does the WMS operation.
        req = mod_python request object (or FakeModPythonRequestObject
            from Jython servlet)
        datasetLines = array of lines in the dataset configuration.
            Each line is a comma-delimited string of the form
            "id,title,location"
        lastUpdateTime = time at which cache of data and metadata was last updated
        TODO: under mod_python, how can we identify where the config file is? """
       
    try:
        # Populate the dictionary that links unique IDs to dataset objects
        datasets = getDatasets(datasetLines)
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
            getCapabilities(req, params, datasets, lastUpdateTime)
        elif request == "GetMap":
            getMap(req, params, datasets)
        elif request == "GetFeatureInfo":
            if config.ALLOW_GET_FEATURE_INFO:
                getFeatureInfo(req, params, datasets)
            else:
                raise OperationNotSupported("GetFeatureInfo")
        elif request == "GetMetadata":
            # This is a convenience extension to WMS for reading smaller
            # chunks of metadata
            getMetadata(req, datasets)
        else:
            raise WMSException("Invalid operation")
    except WMSException, e:
        req.content_type="text/xml"
        e.write(req)

def getDatasets(lines):
    """ Return dictionary of Dataset objects, keyed by the unique ID """
    # TODO error checking
    datasets = {}
    for line in lines:
        els = line.split(",")
        dataset = Dataset()
        dataset.title = els[1].strip()
        dataset.location = els[2].strip()
        dataset.queryable = 0
        if len(els) > 3 and els[3].lower() == "true":
            dataset.queryable = 1
        datasets[els[0].strip()] = dataset # els[0] is the dataset's unique ID
    return datasets
