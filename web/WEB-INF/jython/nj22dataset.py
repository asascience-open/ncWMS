# Data reader that is connected to NetCDF files via the Java NetCDF (nj22) library
from java.lang import Float
from uk.ac.rdg.resc.ncwms.datareader import DataReader
from wmsExceptions import WMSException

def getAllVariableMetadata(dataset):
    """ returns a dictionary of VariableMetadata objects.  The keys
        in the dictionary are the unique ids of the variables and
        the values are VariableMetadata objects
        dataset = dataset object """
    return DataReader.getAllVariableMetadata(dataset.location, dataset.reader)

def readImageData(dataset, varID, tIndex, zValue, grid, fillValue):
    """ Reads data from this variable, projected on to the given grid.
        dataset = dataset object
        varID = unique ID for a variable
        tIndex = index along the time axis
        zValue = value of ELEVATION parameter
        grid = grid onto which data should be projected
        fillvalue = value to use for missing data
        returns an array of floating-point numbers representing the data,
            or None if the variable with id varID does not exist. """
    
    # All exceptions from Java are caught in WMS.py
    if grid.isLatLon:
        # TODO: relax this limitation:
        return DataReader.read(dataset.location, dataset.reader, varID, tIndex,
            zValue, grid.latValues, grid.lonValues, fillValue)
    else:
        raise WMSException("Internal error: can only read data onto grids in lat-lon projections")

def readDataValue(dataset, varID, tIndex, zValue, lat, lon, fillValue):
    """ Reads an individual data point for GetFeatureInfo
        dataset = dataset object
        varID = unique ID for a variable
        tIndex = index along the time axis
        zValue = value of ELEVATION parameter
        lat, lon = latitude and longitude of the data value
        fillvalue = value to use for missing data
        returns the value at the given point, or None if there is no data at the point """

    # All exceptions from Java are caught in WMS.py
    # We can re-use the read() method
    value = DataReader.read(dataset.location, dataset.reader, varID, tIndex, zValue, [lat], [lon], fillValue)[0]
    # If data is missing, read() will return the fill value, having converted
    # it from a double to a float
    if value == Float(fillValue).floatValue():
        return None
    else:
        return value
