# Contacts an ncWMS server and requests a number of image tiles at various
# sizes and zoom levels to test performance.  Results are not logged here: they
# are recorded by the server in the ncwms.benchmark log (which must be activated
# in log4j.properties).

# TODO: build a list of all possible requests, then sample randomly therefrom?

from urllib2 import urlopen
import random

serverUrl = "http://localhost:8084/ncWMS/wms"
layerName = "OSTIA/sst_foundation"
sizes = [128, 256] # image widths/heights to use

zoom_levels = [1, 2, 3] # zoom_level 1 = whole earth, 2 = 1/4 earth
maxNumRequests = (2 ** (zoom_levels[-1] - 1)) ** 2

baseUrl  = '%s?REQUEST=GetMap&VERSION=1.3.0&STYLES=' % serverUrl
baseUrl += '&FORMAT=image/png&CRS=CRS:84&EXCEPTIONS=XML&LAYERS=%s' % layerName

for i in xrange(20):
    # Choose a random size and zoom level
    size = random.choice(sizes)
    url2 = baseUrl + '&WIDTH=%d&HEIGHT=%d' % (size, size)
    z = random.choice(zoom_levels)
    # Calculate the number of rows and columns of images tiles at this zoom level
    numrows = 2 ** (z - 1)
    numcols = numrows * 2 # So the tiles will be square in units of degrees
    # Calculate the length of a side of the image in degrees
    side_length = 180.0 / numrows
    # Choose a random row and column
    row = random.randint(0, numrows - 1)
    col = random.randint(0, numcols - 1)
    # Calculate the bounding box at this row and column
    startlat = -90 + (row * side_length)
    endlat = startlat + side_length
    startlon = -180 + (col * side_length)
    endlon = startlon + side_length
    url = url2 + '&BBOX=%f,%f,%f,%f' % (startlon, startlat, endlon, endlat)
    print size, z, row, col, startlon, startlat, endlon, endlat
    #print url
    #urlopen(url).close() # We don't need to do anything with the data
    

# This alternative code searches exhaustively through the zoom levels and image
# sizes, but will lead to many more queries at the higher zoom levels.  Also
# in this version the tiles are not square

#for size in sizes:
#    url2 = baseUrl + '&WIDTH=%d&HEIGHT=%d' % (size, size)
    
#    for z in zoom_levels:
#        n = 2 ** (z-1) # number of rows and columns to cover the globe
#        rowheight = 180.0 / n
#        colwidth = 360.0 / n
        # Make sure we make the same number of requests for each zoom level
#        for i in xrange(maxNumRequests / n**2):
#            for row in range(n):
#                startlat = -90 + (row * rowheight)
#                endlat = startlat + rowheight
#                for col in range(n):
#                    startlon = -180 + (col * colwidth)
#                    endlon = startlon + colwidth
#                    url = url2 + '&BBOX=%f,%f,%f,%f' % (startlon, startlat, endlon, endlat)
                    #print url
#                    urlopen(url).close() # We don't need to do anything with the data
