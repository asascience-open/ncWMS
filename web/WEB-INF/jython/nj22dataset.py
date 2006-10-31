# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library
from ucar.nc2.dataset import NetcdfDataset
from ucar.nc2.dataset.grid import GridDataset
from ucar.nc2.units import DateFormatter

from java.lang import Integer
from java.util import Arrays

from uk.ac.rdg.resc.ncwms.datareader import DataReader
from uk.ac.rdg.resc.ncwms.exceptions import *

from wmsExceptions import *
import ncWMS

class VariableMetadata: pass # simple class containing a variable's metadata

def getVariableMetadata(location):
    """ returns a dictionary of VariableMetadata objects.  The keys
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

def getVariables(location):
    """ Returns a dictionary of the titles of the variables in the given
        dataset (not the full variable metadata), keyed by the ID of
        the variable, for the web interface """
    nc = NetcdfDataset.openDataset(location)
    gd = GridDataset(nc)
    vars = {}
    for geogrid in gd.getGrids():
        vars[geogrid.getName()] = geogrid.getDescription()
    nc.close()
    return vars

class VariableDetails: pass
def getVariableDetails(location, varID):
    """ Returns the details of the given variable in the given
        NetCDF file, for the web interface """
    nc = NetcdfDataset.openDataset(location)
    gd = GridDataset(nc)
    geogrid = gd.findGridByName(varID)
    coordSys = geogrid.getCoordinateSystem()
    var = VariableDetails()
    var.title = geogrid.getDescription()
    var.units = geogrid.getUnitsString()
    # Set the vertical dimension as array of doubles 
    # TODO: repeats code from above: refactor
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
    # TODO: set valid_min and valid_max properly
    var.valid_min = 0.0
    var.valid_max = 50.0
    nc.close()
    return var

def getTimeAxisValues(location, varID):
    """ Returns the values along the time axis as an array of floats
        in *seconds* since the epoch, or None if the given variable
        does not have a time axis """
    nc = NetcdfDataset.openDataset(location)
    gd = GridDataset(nc)
    geogrid = gd.findGridByName(varID)
    coordSys = geogrid.getCoordinateSystem()
    tValues = None
    if coordSys.isDate():
        dateArr = coordSys.getTimeDates() # Array of java.util.Dates
        if dateArr is not None:
            # date.getTime() gives milliseconds since the epoch
            tValues = [(d.getTime() / 1000.0) for d in dateArr]
    nc.close()
    return tValues

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