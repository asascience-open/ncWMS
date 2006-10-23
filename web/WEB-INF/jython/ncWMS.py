# Entry point for the ncWMS (both CDAT and nj22 implementations)

from exception import WMSException
from utils import getParamValue
from capabilities import getCapabilities

def doWMS(queryString, writer):
    """ Does the WMS operation.

        queryString = the query string of the URL. Must be a valid
                      string, even if empty
        writer = object through which we can write back to the client """
    
    # Turn the request object into a dictionary of key-value pairs
    params = {}
    try:
        # TODO: create a Params object
        if not queryString:
            raise WMSException("Must provide a SERVICE argument")
        for kvp in queryString.split("&"):
            (key, value) = kvp.split("=")
            params[key] = value
        service = getParamValue(params, "service")
        request = getParamValue(params, "request")
        if service != "WMS":
            raise WMSException("SERVICE parameter must be WMS")
        if request == "GetCapabilities":
            writer.write(getCapabilities(params))
        elif request == "GetMap":
            get_map(req, params)
        elif request == "GetFeatureInfo":
            raise WMSException("Operation not yet supported", "OperationNotSupported")
        else:
            raise WMSException("Invalid operation")
    except WMSException, e:
        # TODO req.content_type="text/xml"
        e.write(writer)