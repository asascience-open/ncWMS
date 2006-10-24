# Dataset that is connected to NetCDF files via the Java NetCDF (nj22) library

from dataset import AbstractDataset

class Nj22Dataset(AbstractDataset):
    """ A Dataset object that reads data from NetCDF files via nj22 """
    
    def __init__(title, location):
        """ create an Nj22Dataset.
            title = human-readable title for the dataset
            location = full path to the dataset (NetCDF file, NcML file,
            CDMS file etc) """
        AbstractDataset.__init__(title, location)
    
    
