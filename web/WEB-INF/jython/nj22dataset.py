# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library
from ucar.nc2.dataset import NetcdfDataset
from ucar.nc2.dataset.grid import GridDataset
from ucar.nc2.units import DateFormatter
from ucar.ma2 import Range

from java.lang import Integer
from java.util import Arrays

import jarray

class VariableMetadata: pass # simple class containing a variable's metadata

def getVariableMetadata(location):
    """ returns a dictionary of Variable objects.  The keys
        in the dictionary are the unique ids of the variables and
        the values are Variable objects
        location = location of dataset (full file path, OPeNDAP URL etc) """
    # TODO: error handling
    nc = NetcdfDataset.openDataset(location)
    gd = GridDataset(nc)
    vars = {}
    for geogrid in gd.getGrids():
        var = VariableMetadata()
        var.title = geogrid.getDescription()
        coordSys = geogrid.getCoordinateSystem()

        # Set the vertical dimension as array of doubles 
        if coordSys.hasVerticalAxis():
            zAxis = coordSys.getVerticalAxis()
            var.zUnits = zAxis.getUnitsString()
            if coordSys.isZPositive():
                var.zValues = zAxis.getCoordValues()
            else:
                var.zValues = []
                for z in zAxis.getCoordValues():
                    var.zValues.append(-z)
        else:
            var.zValues = None
            var.zUnits = None
        
        # Set the time dimension as array of strings
        if coordSys.isDate():
            dateFormatter = DateFormatter()
            var.tValues = []
            for t in coordSys.getTimeDates():
                var.tValues.append(dateFormatter.toDateTimeStringISO(t))
        else:
            var.tValues = None

        # Set the bounding box
        # TODO: should take into account the cell bounds
        latLonRect = coordSys.getLatLonBoundingBox()
        lowerLeft = latLonRect.getLowerLeftPoint()
        minLon, minLat = (lowerLeft.getLongitude(), lowerLeft.getLatitude())
        upperRight = latLonRect.getUpperRightPoint()
        maxLon, maxLat = (upperRight.getLongitude(), upperRight.getLatitude())
        if latLonRect.crossDateline():
            minLon, maxLon = (-180, 180)
        var.bbox = (minLon, minLat, maxLon, maxLat)

        # Add to the dictionary
        vars[geogrid.getName()] = var
        
    nc.close()
    return vars


def readData(location, varID, grid, fillValue=1e20):
    """ Reads data from this variable, projected on to the given grid.
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        grid = grid onto which data should be projected
        fillvalue = value to use for missing data
        returns an array of floating-point numbers representing the data,
            or None if the variable with id varID does not exist. """

    nc = NetcdfDataset.openDataset(location)
    gd = GridDataset(nc)
    geogrid = gd.findGridByName(varID)
    coordSys = geogrid.getCoordinateSystem()
    if geogrid is None:
        nc.close()
        return None
    
    # TODO: relax these limitations:
    if not grid.isLatLon:
        raise "Can only read onto images in lat-lon projections"
    if not coordSys.isLatLon():
        raise "Can only read data from lat-lon coordinate systems"

    xAxis = coordSys.getXHorizAxis() # These should both be instances
    yAxis = coordSys.getYHorizAxis() # of CoordinateAxis1D
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
            subset = geogrid.subset(tRange, zRange, yRange, xRange)
            array = subset.readYXData(0, 0).reduce()
            rawData = array.getStorage()
            # Now copy the scanline's data to the picture array
            for i in xrange(len(xIndices)):
                if xIndices[i] >= 0:
                    picIndex = j * grid.width + i
                    picData[picIndex] = rawData[xIndices[i] - minX]

    # Close the source file and return the data
    nc.close()
    return picData