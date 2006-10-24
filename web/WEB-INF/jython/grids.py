# Code relevant to grids, i.e. the array of points that make up the
# image in a GetMap request

class AbstractGrid:
    """ Abstract superclass for all grids.  All subclasses must provide
       an __init__ function like so: __init__(self, bbox, width, height) """
    
    def __init__(self, width, height):
        self.isLatLon = 0 # True if axes are latitude and longitude
        self.lonValues = None # lonValues and latValues will be populated
        self.latValues = None # by subclasses if isLatLon == 1
        self.width = width
        self.height = height
        # TODO: create iterator over all lon-lat values


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
        self.latValues = [(minLat + (height - j - 0.5) * dy) for j in xrange(height)]
        self.size = width * height