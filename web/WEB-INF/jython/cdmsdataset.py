# Dataset that is connected to NetCDF files via CDMS

from dataset import AbstractDataset

class CDMSDataset(AbstractDataset):
    """ A Dataset object that reads data from NetCDF files via nj22 """
    
    def __init__(self, title, location):
        """ create an CDMSDataset.
            title = human-readable title for the dataset
            location = full path to the dataset (NetCDF file, NcML file,
            CDML file etc) """
        AbstractDataset.__init__(self, title, location)
    
    
