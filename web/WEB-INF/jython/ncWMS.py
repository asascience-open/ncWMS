# Entry point for the ncWMS (both CDAT and nj22 implementations)
import re

from wmsExceptions import *
from capabilities import getCapabilities
from getmap import getMap
import config

def wms(req, DatasetClass=None):
    """ Does the WMS operation.
       req = Apache request object (or fake object from Jython servlet)
       DatasetClass = Class that inherits from AbstractDataset, used
           for reading data and metadata """
       
    try:
        # If no datasetclass is specified we default to the cdms implementation
        if DatasetClass is None:
            import cdmsdataset
            DatasetClass = cdmsdataset.CDMSDataset
        # Populate the dictionary that links unique IDs to dataset objects
        datasets = {}
        for d in config.datasets:
            datasets[d[0]] = DatasetClass(d[1], d[2])
        params = RequestParser(req.args)
        service = params.getParamValue("service")
        request = params.getParamValue("request")
        if service != "WMS":
            raise WMSException("SERVICE parameter must be WMS")
        if request == "GetCapabilities":
            capdoc = getCapabilities(req, params, datasets)
            req.content_type="text/xml"
            req.write(capdoc)
        elif request == "GetMap":
            getMap(req, params, datasets)
        elif request == "GetFeatureInfo":
            raise OperationNotSupported("%s is not supported on this server" % request)
        else:
            raise WMSException("Invalid operation")
    except WMSException, e:
        req.content_type="text/xml"
        e.write(req)


class RequestParser:
    """ Parses request parameters from the URL.  Parameter values are
        case-sensitive, but their names are not.  Translates URL
        escape codes (e.g. %2F) to proper characters (e.g. /). """

    def __init__(self, queryString):
        """ queryString is the unprocessed query string from the URL """

        # Regular expressions for replacing URL escape codes
        self._urlCodes = {}
        self._urlCodes[re.compile("%2f", re.IGNORECASE)] = "/"
        self._urlCodes[re.compile("%20", re.IGNORECASE)] = " "

        self._params = {} # Hashtable for query parameters and values
        if queryString is not None:
            for kvp in queryString.split("&"):
                keyAndVal = kvp.split("=")
                if len(keyAndVal) == 2:
                    (key, value) = keyAndVal
                    # We always store the key in lower case
                    self._params[key.lower()] = self._escapeURLCodes(value)

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