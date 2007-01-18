# Data reader that is connected to NetCDF files via the Java NetCDF (nj22) library
from java.lang import Float

from uk.ac.rdg.resc.ncwms.datareader import DatasetCache, DataReader
from uk.ac.rdg.resc.ncwms.exceptions import *

from wmsExceptions import *

def getVariableMetadata(location):
    """ returns a dictionary of VariableMetadata objects.  The keys
        in the dictionary are the unique ids of the variables and
        the values are VariableMetadata objects
        location = location of dataset (full file path, OPeNDAP URL etc) """
    # Get the dataset from the cache
    return DatasetCache.getVariableMetadata(location)

def findTIndex(tValues, target):
    """ returns the index of the given target t value in the given
        array of t values, raising an InvalidDimensionValue exception
        if the target value does not exist """
    # TODO: make this a function of the VariableMetadata object?
    try:
        return DataReader.findTIndex(tValues, target)
    except InvalidDimensionValueException, e:
        raise InvalidDimensionValue(e.getDimName(), e.getValue())

def readImageData(location, varID, tValue, zValue, grid, fillValue):
    """ Reads data from this variable, projected on to the given grid.
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        tValue = time stamp in ISO8601
        zValue = value of ELEVATION parameter
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
    except MissingDimensionValueException, e:
        raise MissingDimensionValue(e.getDimName())
    except WMSExceptionInJava, e:
        raise WMSException(e.getMessage())


def readDataValue(location, varID, tValue, zValue, lat, lon, fillValue):
    """ Reads an individual data point for GetFeatureInfo
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        tValue = time stamp in ISO8601
        zValue = value of ELEVATION parameter
        lat, lon = latitude and longitude of the data value
        i, j = values used in GetFeatureInfo
        returns the value at the given point, or None if there is no data at the point """
    try:
        # We can re-use the read() method
        value = DataReader.read(location, varID, tValue, zValue, [lat], [lon], fillValue)[0]
        # If data is missing, read() will return the fill value, having converted
        # it from a double to a float
        if value == Float(fillValue).floatValue():
            return None
        else:
            return value
    except InvalidDimensionValueException, e:
        raise InvalidDimensionValue(e.getDimName(), e.getValue())
    except MissingDimensionValueException, e:
        raise MissingDimensionValue(e.getDimName())
    except WMSExceptionInJava, e:
        raise WMSException(e.getMessage())
