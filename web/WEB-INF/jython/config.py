# Configuration of the WMS server.  If you change any of the values
# in here, you must reboot or redeploy the server

### End of configuration: do not edit anything below this point ###


# The supported output image formats
PNG_FORMAT = "image/png"
SUPPORTED_IMAGE_FORMATS = [PNG_FORMAT]
# The supported exception formats for GetMap
SUPPORTED_EXCEPTION_FORMATS = ["XML"]

# The separator used when generating unique layer IDs from dataset
# and variable IDs
LAYER_SEPARATOR = "/"

# The fill value to be used internally - can't be NaN because NaN is 
# not portable across Python versions or Jython
FILL_VALUE = 1.0e20

# The interval in minutes at which to clear the cache of NetcdfDatasets
# Setting this too small will affect performance (probably not hugely)
# Setting this too large will result in information being out of date
# (e.g. a new file added to an NcML aggregation will not appear until
# the cache is cleared)
# This does not have to be an integer
CACHE_REFRESH_INTERVAL = 1
