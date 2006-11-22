    # Entry point for the ncWMS (both CDAT and nj22 implementations)
import re
import config

from wmsExceptions import *
from capabilities import getCapabilities
from getmap import getMap
from getfeatureinfo import getFeatureInfo
from web import getMetadata

class Dataset: pass # Simple class that will hold a dataset's title and location

def wms(req, datasetLines, lastUpdateTime):
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
        if req.args is None or req.args.strip() == "":
            # If there is no request string, return the front page
            req.args = "SERVICE=WMS&REQUEST=GetMetadata&item=frontpage"
        params = RequestParser(req.args)
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

class RequestParser:
    """ Parses request parameters from the URL.  Parameter values are
        case-sensitive, but their names are not.  Translates URL
        escape codes (e.g. %2F) to proper characters (e.g. /). """

    def __init__(self, queryString):
        """ queryString is the unprocessed query string from the URL """

        # Regular expressions for replacing URL escape codes
        # TODO there are many more
        self._urlCodes = {}
        self._urlCodes[re.compile("%2f", re.IGNORECASE)] = "/"
        self._urlCodes[re.compile("%20", re.IGNORECASE)] = " "
        self._urlCodes[re.compile("%3a", re.IGNORECASE)] = ":"
        self._urlCodes[re.compile("%2c", re.IGNORECASE)] = ","

        self._params = {} # Hashtable for query parameters and values
        if queryString is not None:
            for kvp in queryString.split("&"):
                keyAndVal = kvp.split("=")
                if len(keyAndVal) == 2:
                    (key, value) = keyAndVal
                    # We always store the key in lower case and escape
                    # the URL % codes
                    self._params[key.lower()] = self._escapeURLCodes(value).strip()

    def _escapeURLCodes(self, str):
        """ Replaces all the URL escape codes with their proper characters """
        for regexp in self._urlCodes.keys():
            str = regexp.sub(self._urlCodes[regexp], str)
        return str

    def getParamValue(self, key, default=None):
        """ Gets the value of the given parameter. If default==None
           and the parameter does not exist, a WMSException is thrown.
           Otherwise, the parameter value is returned, or the default
           value if it does not exist """
        if self._params.has_key(key.lower()):
            return self._params[key.lower()]
        elif default is None:
            raise WMSException("Must provide a " + key.upper() + " argument")
        else:
            return default