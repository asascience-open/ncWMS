class AbstractDataset:
    """ Abstract superclass for a Dataset.  Subclasses will exist for Jython
        (i.e. nj22) and CDMS """
    
    def __init__(self, title, location):
        """ create an AbstractDataset.
            title = human-readable title for the dataset
            location = full path to the dataset (NetCDF file, NcML file,
            CDML file etc) """
        self.title = title
        self.location = location
        
    def getVariables(self):
        """ returns a dictionary of AbstractVariable objects.  The keys
           in the dictionary are the unique ids of the variables and
           the values are AbstractVariable objects """
        return {} # subclasses must override

    def getVariable(self, id):
        """ returns the AbstractVariable with the given id, or None
           if there is no variable with the given id """
        return None # subclasses must override

class AbstractVariable:
    """ Abstract superclass for a Variable. """

    def __init__(self, title):
        """ Create an AbstractVariable.
            title = human-readable title for the variable """
        self.title = title
        # The properties below should be set by subclasses
        self.zValues = None # List of z values
        self.zUnits = None # z axis units as a string
        self.tValues = None # List of t values as ISO formatted strings
        self.bbox = (-180, -90, 180, 90) # Bounding box

    def readData(self, grid, fillValue=1e20):
        """ Reads data from this variable, projected on to the given grid.
           Returns an array of floating-point numbers representing the data.
           Missing values are represented by fillValue """
        return None
