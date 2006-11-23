# Code for generating the Capabilities document
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from xml.utils import iso8601

import config, time
import sys
if sys.platform.startswith("java"):
    # We're running on Jython
    import nj22dataset as datareader
else:
    # TODO: check for presence of CDAT
    import cdmsdataset as datareader
from wmsExceptions import *

def getCapabilities(req, params, datasets, lastUpdateTime):
    """ Returns the Capabilities document.
        req = mod_python request object or WMS.FakeModPythonRequest object
        params = ncWMS.RequestParser object containing the request parameters
        datasets = dictionary of dataset.AbstractDatasets, indexed by unique id 
        lastUpdateTime = time at which cache of data and metadata was last updated """

    version = params.getParamValue("version", "")
    format = params.getParamValue("format", "")
    # TODO: deal with version and format

    updatesequence = params.getParamValue("updatesequence", "")
    if updatesequence != "":
        # Client has requested a specific update sequence
        try:
            us = iso8601.parse(updatesequence)
            if round(us) == round(lastUpdateTime):
                # Equal to the nearest second
                raise CurrentUpdateSequence(updatesequence)
            elif us > lastUpdateTime:
                raise InvalidUpdateSequence(updatesequence)
        except ValueError:
            # Client didn't supply a valid ISO8601 date
            # According to the spec, InvalidUpdateSequence is not the
            # right error code here so we use the generic one
            raise WMSException("UPDATESEQUENCE must be a valid ISO8601 date")
    
    output = StringIO()
    output.write(config.XML_HEADER)
    output.write("<WMS_Capabilities version=\"" + config.WMS_VERSION + "\"")
    # UpdateSequence is accurate to the nearest second
    output.write(" updateSequence=\"%s\"" % iso8601.tostring(round(lastUpdateTime)))
    output.write(" xmlns=\"http://www.opengis.net/wms\"")
    output.write(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
    # The next two lines should be commented out if you wish to load this document
    # in Cadcorp SIS from behind the University of Reading firewall
    output.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
    output.write(" xsi:schemaLocation=\"http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd\"")
    output.write(">")
    
    output.write("<Service>")
    output.write("<Name>WMS</Name>")
    output.write("<Title>%s</Title>" % config.title)
    output.write("<OnlineResource xlink:type=\"simple\" xlink:href=\"%s\"/>" % config.url)
    output.write("<Fees>none</Fees>")
    output.write("<AccessConstraints>none</AccessConstraints>")
    output.write("<LayerLimit>%d</LayerLimit>" % config.LAYER_LIMIT)
    output.write("<MaxWidth>%d</MaxWidth>" % config.MAX_IMAGE_WIDTH)
    output.write("<MaxHeight>%d</MaxHeight>" % config.MAX_IMAGE_HEIGHT)
    output.write("</Service>")
    
    output.write("<Capability>")
    output.write("<Request>")
    output.write("<GetCapabilities>")
    output.write("<Format>text/xml</Format>")
    url = "http://%s%s?" % (req.server.server_hostname, req.unparsed_uri.split("?")[0])
    output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"" +
        url + "\"/></Get></HTTP></DCPType>")
    output.write("</GetCapabilities>")
    output.write("<GetMap>")
    for format in config.SUPPORTED_IMAGE_FORMATS:
        output.write("<Format>%s</Format>" % format)
    output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"" +
        url + "\"/></Get></HTTP></DCPType>")
    output.write("</GetMap>")
    if config.ALLOW_GET_FEATURE_INFO:
        output.write("<GetFeatureInfo>")
        for format in config.FEATURE_INFO_FORMATS:
            output.write("<Format>%s</Format>" % format)
        output.write("<DCPType><HTTP><Get><OnlineResource xlink:type=\"simple\" xlink:href=\"" +
            url + "\"/></Get></HTTP></DCPType>")
        output.write("</GetFeatureInfo>")
    output.write("</Request>")
    # TODO: support more exception types
    output.write("<Exception>")
    for ex_format in config.SUPPORTED_EXCEPTION_FORMATS:
        output.write("<Format>%s</Format>" % ex_format)
    output.write("</Exception>")

    # Write the top-level container layer
    output.write("<Layer>")
    output.write("<Title>%s</Title>" % config.title)
    # TODO: add styles
    for crs in config.SUPPORTED_CRSS.keys():
        output.write("<CRS>" + crs + "</CRS>")
    
    # Now for the dataset layers
    for dsid in datasets.keys():
        # Write a container layer for this dataset. Container layers
        # do not have a Name
        output.write("<Layer>")
        output.write("<Title>%s</Title>" % datasets[dsid].title)
        # Now write the displayable data layers
        vars = datareader.getVariableMetadata(datasets[dsid].location)
        for vid in vars.keys():
            output.write("<Layer")
            if config.ALLOW_GET_FEATURE_INFO and datasets[dsid].queryable:
                output.write(" queryable=\"1\"")
            output.write(">")
            output.write("<Name>%s%s%s</Name>" % (dsid, config.LAYER_SEPARATOR, vid))
            output.write("<Title>%s</Title>" % vars[vid].title)
            output.write("<Abstract>%s</Abstract>" % vars[vid].abstract)

            # Set the bounding box
            minLon, minLat, maxLon, maxLat = vars[vid].bbox
            output.write("<EX_GeographicBoundingBox>")
            output.write("<westBoundLongitude>%s</westBoundLongitude>" % str(minLon))
            output.write("<eastBoundLongitude>%s</eastBoundLongitude>" % str(maxLon))
            output.write("<southBoundLatitude>%s</southBoundLatitude>" % str(minLat))
            output.write("<northBoundLatitude>%s</northBoundLatitude>" % str(maxLat))
            output.write("</EX_GeographicBoundingBox>")
            output.write("<BoundingBox CRS=\"CRS:84\" ")
            output.write("minx=\"%f\" maxx=\"%f\" miny=\"%f\" maxy=\"%f\"/>"
                % (minLon, maxLon, minLat, maxLat))

            # Set the level dimension
            if vars[vid].zvalues is not None:
                output.write("<Dimension name=\"elevation\" units=\"%s\"" 
                    % vars[vid].zunits)
                # Use the first value in the array as the default
                # If the default value is removed, you also need to edit
                # the data reading code (e.g. DataReader.java) to
                # disallow default z values
                output.write(" default=\"%s\">" % vars[vid].zvalues[0])
                firstTime = 1
                for z in vars[vid].zvalues:
                    if firstTime:
                        firstTime = 0
                    else:
                        output.write(",")
                    output.write(str(z))
                output.write("</Dimension>")

            # Set the time dimension
            if vars[vid].tvalues is not None:
                output.write("<Dimension name=\"time\" units=\"ISO8601\">")
                # If we change this to support the "current" attribute
                # we must also change the data reading code
                firstTime = 1
                for t in vars[vid].tvalues:
                    if firstTime:
                        firstTime = 0
                    else:
                        output.write(",")
                    output.write(iso8601.tostring(t))
                output.write("</Dimension>")

            output.write("</Layer>") # end of variable Layer
        output.write("</Layer>") # end of dataset layer
    
    output.write("</Layer>") # end of top-level container layer
    
    output.write("</Capability>")
    output.write("</WMS_Capabilities>")

    req.content_type="text/xml"
    req.write(output.getvalue())
    output.close() # Free the buffer
    return
