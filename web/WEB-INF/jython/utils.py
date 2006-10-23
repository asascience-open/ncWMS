# Useful utility functions
import re

from exception import WMSException

# Regular expressions for replacing URL escape codes
urlEscape = re.compile("%2f", re.IGNORECASE)
spaceEscape = re.compile("%20", re.IGNORECASE)

def getParamValue(params, param_name, default=None):
    """ Gets the value of the requested parameter from the query string,
        returning the default value if a value is not given.
        This is *not* sensitive to the case of the param_name.
        If default==None, the parameter is compulsory and a WMSException
        will be raised if it is not found.  Specify default="" to allow optional
        parameters """
    for key in params.keys():
        if key.lower() == param_name.lower():
            # Make sure we return an empty string rather than None
            val = (params[key], "")[params[key] is None]
            # Replace URL escape codes with proper values
            return urlEscape.sub("/", spaceEscape.sub(" ", val))
    if default is None:
        raise WMSException("Must provide a " + param_name.upper() + " argument")
    else:
        return default
