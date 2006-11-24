# Code relevant to grids, i.e. the array of points that make up the
# image in a GetMap request
import math

# The supported map projections (coordinate reference systems). We map
# the codes to implementations of AbstractGrid that generate the grid
# objects themselves
def getSupportedCRSs():
    return { "CRS:84" : PlateCarreeGrid, "EPSG:41001" : MercatorGrid }

class AbstractGrid:
    """ Abstract superclass for all grids.  All subclasses must provide
       an __init__ function like so: __init__(self, bbox, width, height) """
    
    def __init__(self, width, height):
        self.isLatLon = 0 # True if axes are latitude and longitude
        self.lonValues = None # lonValues and latValues will be populated
        self.latValues = None # by subclasses if isLatLon == 1
        self.width = width
        self.height = height
        
    def getLonLat(self, i, j):
        """ Returns the longitude and latitude of the point at the given
            i and j indices in the grid """
        if self.isLatLon:
            return self.lonValues[i], self.latValues[j]
        else:
            # TODO: relax this
            raise "getLonLat() only supported for latlon grids"


class PlateCarreeGrid(AbstractGrid):
    """ A grid in Plate Carree projection, i.e. regularly-spaced latitude
        and longitude axes """

    def __init__(self, bbox, width, height):
        """ Create a grid in Plate Carree projection
            bbox = bounding box in form (minLon, minLat, maxLon, maxLat)
            width = width of requested image in pixels
            height = height of requested image in pixels """
        AbstractGrid.__init__(self, width, height)
        self.isLatLon = 1
        minLon, minLat, maxLon, maxLat = bbox
        dx = (maxLon - minLon) / width
        self.lonValues = [(minLon + (i + 0.5) * dx) for i in xrange(width)]
        dy = (maxLat - minLat) / height
        # The latitude axis is flipped
        self.latValues = [(minLat + (height - j - 0.5) * dy) for j in xrange(height)]
        self.size = width * height


class MercatorGrid(AbstractGrid):
    """ A grid in Mercator projection """

    def __init__(self, bbox, width, height):
        """ Create a grid in Mercator projection
            bbox = bounding box in form (minLon, minLat, maxLon, maxLat)
            width = width of requested image in pixels
            height = height of requested image in pixels """
        AbstractGrid.__init__(self, width, height)
        self.isLatLon = 1
        minLon, minLat, maxLon, maxLat = bbox
        dx = (maxLon - minLon) / width
        self.lonValues = [(minLon + (i + 0.5) * dx) for i in xrange(width)]
        # TODO: lat axes not correct!
        dy = (maxLat - minLat) / height
        # The latitude axis is flipped
        self.latValues = _getMercatorLatitudeArray(minLat, maxLat, height)
        self.size = width * height

def _getMercatorLatitudeArray(minlat, maxlat, n):
    """ Returns an array (list) of latitudes in Mercator projection where
        the bounding box has the given minimum and maximum latitudes, and n
        is the number of points in the array. """
    # Get the minimum and maximum y coordinates, being careful to avoid
    # the singularities at lat = +- 90
    miny = _latToY((minlat, -89.9999)[minlat < -89.9999])
    maxy = _latToY((maxlat, 89.9999)[maxlat > 89.9999])
    dy = (maxy - miny) / n
    # Remember that the latitude axis is flipped
    return [_yToLat(miny + ((n - j - 1) + 0.5) * dy) for j in xrange(n)]

def _yToLat(y):
    """ Calculates the latitude (in degrees) of a given y coordinate.
        lat = arctan(sinh(y)) """
    return _toDegrees(math.atan(math.sinh(y)))

def _latToY(lat):
    """ Calculates the y coordinate of a given latitude value in degrees.
        y = ln(tan(pi/4 + lat/2)) """
    return math.log(math.tan(math.pi / 4 +_toRadians(lat) / 2))

def _toDegrees(rad):
    """ Converts the given radians value to degrees (math.degrees is not present
        in Jython 2.1 """
    return rad * (180 / math.pi)

def _toRadians(deg):
    """ Converts the given degrees value to radians (math.radians is not present
        in Jython 2.1 """
    return deg * (math.pi / 180)
    