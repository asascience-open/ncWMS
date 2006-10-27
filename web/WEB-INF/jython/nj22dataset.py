# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library
from ucar.nc2.dataset import NetcdfDataset
from ucar.nc2.dataset.grid import GridDataset
from ucar.nc2.units import DateFormatter

from java.lang import Integer
from java.util import Arrays

from uk.ac.rdg.resc.ncwms.datareader import DataReader
from uk.ac.rdg.resc.ncwms.exceptions import *

from wmsExceptions import *
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


def readData(location, varID, tValue, zValue, grid, fillValue=1e20):
    """ Reads data from this variable, projected on to the given grid.
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        grid = grid onto which data should be projected
        fillvalue = value to use for missing data
        returns an array of floating-point numbers representing the data,
            or None if the variable with id varID does not exist. """
    
    if not grid.isLatLon:
        # TODO: relax this limitation:
        raise "Can only read data onto grids in lat-lon projections"
    try:
        return DataReader.read(location, varID, tValue, zValue,
            grid.latValues, grid.lonValues, fillValue)
    except InvalidDimensionValueException, e:
        raise InvalidDimensionValue(e.getDimName(), e.getValue())
    except WMSExceptionInJava, e:
        raise WMSException(e.getMessage())