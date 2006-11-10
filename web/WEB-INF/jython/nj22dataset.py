# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library
from ucar.nc2.dataset import NetcdfDatasetCache, EnhanceScaleMissingImpl
from ucar.nc2.dataset.grid import GridDataset

from uk.ac.rdg.resc.ncwms.datareader import DataReader, DatasetFactory
from uk.ac.rdg.resc.ncwms.exceptions import *

from wmsExceptions import *
import ncWMS

class VariableMetadata: pass

def getVariableMetadata(location):
    """ returns a dictionary of VariableMetadata objects.  The keys
        in the dictionary are the unique ids of the variables and
        the values are VariableMetadata objects
        location = location of dataset (full file path, OPeNDAP URL etc) """
    # TODO: error handling: wrap in try-finally
    # Get the dataset from the cache
    nc = NetcdfDatasetCache.acquire(location, None, DatasetFactory.get())
    gd = GridDataset(nc)
    vars = {}
    for gg in gd.getGrids():
        vm = VariableMetadata()
        vm.id = gg.getName()
        vm.title = _getStandardName(gg)
        vm.abstract = gg.getDescription()
        vm.units = gg.getUnitsString()
        coordSys = gg.getCoordinateSystem()

        vm.zvalues = None
        if coordSys.hasVerticalAxis():
            zAxis = coordSys.getVerticalAxis()
            vm.zunits = zAxis.getUnitsString()
            vm.zpositive = coordSys.isZPositive()
            if coordSys.isZPositive():
                vm.zvalues = zAxis.getCoordValues()
            else:
                vm.zvalues = [(0.0 - z) for z in zAxis.getCoordValues()]

        vm.tvalues = None
        if coordSys.isDate():
            tVals = coordSys.getTimeDates()
            vm.tvalues = [(t.getTime() / 1000.0) for t in tVals] # Seconds since the epoch

        # Set the bounding box
        # TODO: should take into account the cell bounds
        latLonRect = coordSys.getLatLonBoundingBox();
        ll = latLonRect.getLowerLeftPoint();
        ur = latLonRect.getUpperRightPoint();
        minLat, maxLat = ll.getLatitude(), ur.getLatitude()
        if latLonRect.crossDateline():
            minLon, maxLon = -180.0, 180.0
        else:
            minLon, maxLon = ll.getLongitude(), ur.getLongitude()            
        vm.bbox = [minLon, minLat, maxLon, maxLat]
        
        enhanced = DataReader.getEnhanced(gg)
        vm.validMin = enhanced.validMin
        vm.validMax = enhanced.validMax

        vars[gg.getName()] = vm

    nc.close()
    return vars

def _getStandardName(geogrid):
    """ Returns the standard_name attribute or the unique name if the
        standard_name is not provided """
    stdNameAtt = geogrid.findAttributeIgnoreCase("standard_name")
    if stdNameAtt is None:
        return geogrid.getName()
    else:
        return stdNameAtt.getStringValue()

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
    except MissingDimensionValueException, e:
        raise MissingDimensionValue(e.getDimName())
    except WMSExceptionInJava, e:
        raise WMSException(e.getMessage())