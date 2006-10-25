# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library
from ucar.nc2.dataset import NetcdfDataset
from ucar.nc2.dataset.grid import GridDataset
from ucar.nc2.units import DateFormatter
from ucar.ma2 import Range

from java.lang import Integer
from java.util import Arrays

import jarray

from dataset import *

class Nj22Dataset(AbstractDataset):
    """ A Dataset object that reads data from NetCDF files via nj22 """
    
    def __init__(self, title, location):
        """ create an Nj22Dataset.
            title = human-readable title for the dataset
            location = full path to the dataset (NetCDF file, NcML file, etc) """
        AbstractDataset.__init__(self, title, location)
        self._variables = None
        
    def getVariables(self):
        """ returns a dictionary of Nj22Variable objects.  The keys
           in the dictionary are the unique ids of the variables and
           the values are Nj22Variable objects """
        # TODO: error handling
        if self._variables is None:
            self._variables = {}
            # Keep a cache of variables so we don't have to create them again
            nc = NetcdfDataset.openDataset(self.location)
            gd = GridDataset(nc)
            for geogrid in gd.getGrids():
                self._variables[geogrid.getName()] = Nj22Variable(geogrid)
            nc.close()
        return self._variables

    def getVariable(self, id):
        """ returns the Nj22Variable with the given id, or None
           if there is no variable with the given id """
        nc = NetcdfDataset.openDataset(self.location)
        gd = GridDataset(nc)
        gg = gd.findGridByName(id)
        if gg is None:
            nc.close()
            return None
        else:
            # We don't close the dataset because we will read data from
            # it soon
            return Nj22Variable(gg, nc)


class Nj22Variable(AbstractVariable):
    """ A Variable that is read from NetCDF files using nj22 """

    def __init__(self, geogrid, ncFile = None):
        """ Create an Nj22Variable from a GeoGrid object. """
        AbstractVariable.__init__(self, geogrid.getDescription())   
        self.geogrid = geogrid
        self.coordSys = geogrid.getCoordinateSystem()
        self._ncFile = ncFile

        # Set the vertical dimension as array of doubles
        if self.coordSys.hasVerticalAxis():
            zAxis = self.coordSys.getVerticalAxis()
            self.zUnits = zAxis.getUnitsString()
            if self.coordSys.isZPositive():
                self.zValues = zAxis.getCoordValues()
            else:
                self.zValues = []
                for z in zAxis.getCoordValues():
                    self.zValues.append(-z)
        
        # Set the time dimension as array of strings
        if self.coordSys.isDate():
            dateFormatter = DateFormatter()
            self.tValues = []
            for t in self.coordSys.getTimeDates():
                self.tValues.append(dateFormatter.toDateTimeStringISO(t))

        # Set the bounding box
        # TODO: should take into account the cell bounds
        latLonRect = self.coordSys.getLatLonBoundingBox()
        lowerLeft = latLonRect.getLowerLeftPoint()
        minLon, minLat = (lowerLeft.getLongitude(), lowerLeft.getLatitude())
        upperRight = latLonRect.getUpperRightPoint()
        maxLon, maxLat = (upperRight.getLongitude(), upperRight.getLatitude())
        if latLonRect.crossDateline():
            minLon, maxLon = (-180, 180)
        self.bbox = (minLon, minLat, maxLon, maxLat)

    def readData(self, grid, fillValue=1e20):
        """ Reads data from this variable, projected on to the given grid.
           Returns an array of floating-point numbers representing the data.
           Missing values are represented by fillValue.  This is called
           after __init__(self, geogrid, ncFile) as part of GetMap """
        # TODO: relax these limitations
        if not grid.isLatLon:
            raise "Can only read onto images in lat-lon projections"
        if not self.coordSys.isLatLon():
            raise "Can only read data from lat-lon coordinate systems"
        xAxis = self.coordSys.getXHorizAxis() # These should both be instances
        yAxis = self.coordSys.getYHorizAxis() # of CoordinateAxis1D
        # TODO: handle t and z properly
        tRange = Range(0, 0)
        zRange = Range(0, 0)
        # Find the range of x indices
        minX = Integer.MAX_VALUE
        maxX = -Integer.MAX_VALUE
        xIndices = []
        for lon in grid.lonValues:
            xIndex = xAxis.findCoordElement(lon) # TODO: findCoordElement() could be more efficient
            xIndices.append(xIndex)
            if xIndex >= 0:
                if xIndex < minX : minX = xIndex
                if xIndex > maxX : maxX = xIndex
        xRange = Range(minX, maxX)
        # Create an array to hold the data
        # TODO: not sure this is the best way to do this
        picData = jarray.zeros(grid.width * grid.height, 'f')
        Arrays.fill(picData, fillValue)
        # Cycle through the latitude values, extracting a scanline of
        # data each time from minX to maxX
        for j in xrange(len(grid.latValues)):
            yIndex = yAxis.findCoordElement(grid.latValues[j])
            if yIndex >= 0:
                yRange = Range(yIndex, yIndex)
                subset = self.geogrid.subset(tRange, zRange, yRange, xRange)
                array = subset.readYXData(0, 0).reduce()
                rawData = array.getStorage()
                # Now copy the scanline's data to the picture array
                for i in xrange(len(xIndices)):
                    if xIndices[i] >= 0:
                        picIndex = j * grid.width + i
                        picData[picIndex] = rawData[xIndices[i] - minX]
                       
        # Close the source file and return the data
        self._ncFile.close()
        return picData
