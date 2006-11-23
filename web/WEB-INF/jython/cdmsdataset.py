# Data reader that is connected to NetCDF files via CDMS

import Numeric

class VariableMetadata: pass

def getVariableMetadata(location):
    """ returns a dictionary of VariableMetadata objects.  The keys
        in the dictionary are the unique ids of the variables and
        the values are VariableMetadata objects
        location = location of dataset (full file path, OPeNDAP URL etc) """
    vm = VariableMetadata()
    # TODO: populate the object
    return vm

def readImageData(location, varID, tValue, zValue, grid, fillValue=1e20):
    """ Reads data from this variable, projected on to the given grid.
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        tValue = time stamp in ISO8601
        zValue = value of ELEVATION parameter
        grid = grid onto which data should be projected
        fillvalue = value to use for missing data
        returns an array of floating-point numbers representing the data,
            or None if the variable with id varID does not exist. """
    
    # TODO read data properly
    return Numeric.zeros(grid.size, "f")

def readDataValue(location, varID, tValue, zValue, lat, lon):
    """ Reads an individual data point for GetFeatureInfo
        location = location of dataset (full file path, OPeNDAP URL etc)
        varID = unique ID for a variable
        tValue = time stamp in ISO8601
        zValue = value of ELEVATION parameter
        lat, lon = latitude and longitude of the data value
        i, j = values used in GetFeatureInfo
        returns the value at the given point, or None if there is no data at the point """

    # TODO read properly
    return 0.0
