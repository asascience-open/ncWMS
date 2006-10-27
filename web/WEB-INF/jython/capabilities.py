# Code for generating the Capabilities document
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

import config, time
import nj22dataset # TODO import other modules for CDMS server

def getCapabilities(req, params, datasets):
    """ Returns the Capabilities document.
        req = mod_python request object or WMS.FakeModPythonRequest object
        params = ncWMS.RequestParser object containing the request parameters
        datasets = dictionary of dataset.AbstractDatasets, indexed by unique id """

    version = params.getParamValue("version", "")
    format = params.getParamValue("format", "")
    updatesequence = params.getParamValue("updatesequence", "")    
    # TODO: deal with version, format and updatesequence
    
    output = StringIO()
    output.write(config.XML_HEADER)
    output.write("<WMS_Capabilities version=\"" + config.WMS_VERSION + "\"")
    # UpdateSequence is always the current time, representing the fact
    # that the capabilities doc is always generated dynamically
    # TODO: change this to help caches
    output.write(" updateSequence=\"%04d-%02d-%02dT%02d:%02d:%02dZ\"" % time.gmtime()[:-3])
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
    output.write("<LayerLimit>%s</LayerLimit>" % str(config.LAYER_LIMIT))
    output.write("<MaxWidth>%s</MaxWidth>" % str(config.MAX_IMAGE_WIDTH))
    output.write("<MaxHeight>%s</MaxHeight>" % str(config.MAX_IMAGE_HEIGHT))
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
        vars = nj22dataset.getVariableMetadata(datasets[dsid].location)
        for vid in vars.keys():
            output.write("<Layer>")
            output.write("<Name>%s/%s</Name>" % (dsid, vid))
            output.write("<Title>%s</Title>" % vars[vid].title)

            # Set the bounding box
            minLon, minLat, maxLon, maxLat = vars[vid].bbox
            output.write("<EX_GeographicBoundingBox>")
            output.write("<westBoundLongitude>%f</westBoundLongitude>" % minLon)
            output.write("<eastBoundLongitude>%f</eastBoundLongitude>" % maxLon)
            output.write("<southBoundLatitude>%f</southBoundLatitude>" % minLat)
            output.write("<northBoundLatitude>%f</northBoundLatitude>" % maxLat)
            output.write("</EX_GeographicBoundingBox>")
            output.write("<BoundingBox CRS=\"CRS:84\" ")
            output.write("minx=\"%f\" maxx=\"%f\" miny=\"%f\" maxy=\"%f\"/>"
                % (minLon, maxLon, minLat, maxLat))

            # Set the level dimension
            if vars[vid].zValues is not None:
                output.write("<Dimension name=\"elevation\" units=\"%s\"" 
                    % vars[vid].zUnits)
                # Use the first value in the array as the default
                # If the default value is removed, you also need to edit
                # the data reading code (e.g. DataReader.java) to
                # disallow default z values
                output.write(" default=\"%s\">" % vars[vid].zValues[0])
                firstTime = 1
                for z in vars[vid].zValues:
                    if firstTime:
                        firstTime = 0
                    else:
                        output.write(",")
                    output.write(str(z))
                output.write("</Dimension>")

            # Set the time dimension
            if vars[vid].tValues is not None:
                output.write("<Dimension name=\"time\" units=\"ISO8601\">")
                firstTime = 1
                for t in vars[vid].tValues:
                    if firstTime:
                        firstTime = 0
                    else:
                        output.write(",")
                    output.write(t)
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
