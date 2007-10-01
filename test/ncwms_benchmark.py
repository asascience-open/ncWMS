# Contacts an ncWMS server and requests a number of image tiles at various
# sizes and zoom levels to test performance.  Results are not logged here: they
# are recorded by the server in the ncwms.benchmark log (which must be activated
# in log4j.properties).

# TODO: build a list of all possible requests, then sample randomly therefrom?

from urllib2 import urlopen

serverUrl = "http://localhost:8084/ncWMS/wms"
layerName = "OSTIA/sst_foundation"
sizes = [256] # image width/height to use

zoom_levels = [1, 2, 3, 4] # zoom_level 1 = whole earth, 2 = 1/4 earth
maxNumRequests = (2 ** (zoom_levels[-1] - 1)) ** 2

for size in sizes:
    baseUrl  = '%s?REQUEST=GetMap&VERSION=1.3.0&STYLES=' % serverUrl
    baseUrl += '&FORMAT=image/png&CRS=CRS:84&EXCEPTIONS=XML'
    baseUrl += '&LAYERS=%s&WIDTH=%d&HEIGHT=%d' % (layerName, size, size)
    
    for z in zoom_levels:
        n = 2 ** (z-1) # number of rows and columns to cover the globe
        rowheight = 180.0 / n
        colwidth = 360.0 / n
        # Make sure we make the same number of requests for each zoom level
        for i in xrange(maxNumRequests / n**2):
            for row in range(n):
                startlat = -90 + (row * rowheight)
                endlat = startlat + rowheight
                for col in range(n):
                    startlon = -180 + (col * colwidth)
                    endlon = startlon + colwidth
                    url = baseUrl + '&BBOX=%f,%f,%f,%f' % (startlon, startlat, endlon, endlat)
                    #print url
                    urlopen(url).close() # We don't need to do anything with the data
