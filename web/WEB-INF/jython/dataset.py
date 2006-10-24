class AbstractDataset:
    """ Abstract superclass for a Dataset.  Subclasses will exist for Jython
        (i.e. nj22) and CDMS """
    
    def __init__(title, location):
        """ create an AbstractDataset.
            title = human-readable title for the dataset
            location = full path to the dataset (NetCDF file, NcML file,
            CDMS file etc) """
        self.title = title
        self.location = location
        
    
