# Configuration of the WMS server

title = "Web Map Service for NetCDF data"
url = "http://www.nerc-essc.ac.uk"

### End of configuration: do not edit anything below this point ###

WMS_VERSION = "1.3.0"
XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

# The supported map projections (coordinate reference systems). We map
# the codes to implementations of AbstractGrid that generate the grid
# objects themselves
import grids
SUPPORTED_CRSS = {
    "CRS:84" : grids.PlateCarreeGrid,
    "EPSG:41001" : grids.MercatorGrid
}

# The maximum number of layers that can be requested simultaneously by GetMap
LAYER_LIMIT = 1
# The supported output image formats
PNG_FORMAT = "image/png"
SUPPORTED_IMAGE_FORMATS = [PNG_FORMAT]
# The supported exception formats
SUPPORTED_EXCEPTION_FORMATS = ["XML"]
# The maximum allowed width and height of images
MAX_IMAGE_WIDTH = 1000
MAX_IMAGE_HEIGHT = 1000

# The separator used when generating unique layer IDs
LAYER_SEPARATOR = "/"

# The interval in minutes at which to clear the cache of NetcdfDatasets
# Setting this too small will affect performance (probably not hugely)
# Setting this too large will result in information being out of date
# (e.g. a new file added to an NcML aggregation will not appear until
# the cache is cleared)
# This does not have to be an integer
CACHE_REFRESH_INTERVAL = 1
