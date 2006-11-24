# Common utility routines

import urllib

from wmsExceptions import WMSException

def getWMSVersion():
    """ Returns the version of this WMS server """
    return "1.3.0"

class RequestParser:
    """ Parses request parameters from the URL.  Parameter values are
        case-sensitive, but their names are not.  Translates URL
        escape codes (e.g. %2F) to proper characters (e.g. /). """

    def __init__(self, queryString):
        """ queryString is the unprocessed query string from the URL """
        self._params = {} # Hashtable for query parameters and values
        if queryString is not None:
            for kvp in queryString.split("&"):
                keyAndVal = kvp.split("=")
                if len(keyAndVal) == 2:
                    (key, value) = keyAndVal
                    # We always store the key in lower case, escape
                    # the URL % codes and replace "+" with a space
                    self._params[key.lower()] = urllib.unquote_plus(value).strip()

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

def getLayerSeparator():
    """ Returns the string used to delimit dataset and variable names in the
        construction of a layer's name """
    return "/"

