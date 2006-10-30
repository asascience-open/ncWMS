# Configuration of the WMS server

# The list of datasets that are exposed.  Each dataset is an array
# of strings of the form [<unique id>, <title>, <location>]
datasets = [
    ['MRCS', 'POLCOMS MRCS data', 'C:\\data\\POLCOMS_MRCS_NOWCAST_20060731.nc'],
    ['OSTIA', 'OSTIA SST Analysis', 'C:\\data\\20061017-UKMO-L4UHfnd-GLOB-v01.nc'],
    #['FOAM', 'FOAM one degree', 'C:\\data\\FOAM_one.ncml'],
    ['Ifremer_temp', 'CORIOLIS temperature data', 'C:\\data\\temp_OA_20061030.nc'],
    ['Ifremer_sal', 'CORIOLIS salinity data', 'C:\\data\\psal_OA_20061030.nc']
]
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
