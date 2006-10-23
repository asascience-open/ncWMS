# Configuration of the WMS server

# The root of the file hierarchy
# TODO: replace root with list of Dataset objects
root = '/var/www/cdat'
title = "Web Map Service for marine data"
url = "http://www.nerc-essc.ac.uk"

### End of configuration: do not edit anything below this point

WMS_VERSION = "1.3.0"
XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"

# The supported map projections (coordinate reference systems)
CRS_LONLAT = "CRS:84"
CRS_EPSG4326 = "EPSG:4326" # Same as CRS:84 but with lat and lon reversed. I think.
CRS_MERCATOR = "EPSG:41001"
SUPPORTED_CRSS = [CRS_LONLAT, CRS_EPSG4326, CRS_MERCATOR]
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
