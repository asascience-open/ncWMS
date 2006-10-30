# routines that produce output (HTML or XML) for web pages
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from xml.utils import iso8601
import time
import ncWMS
import config
import nj22dataset

def metadata(req):
    """ Processes a request for metadata from the Godiva2 web interface """
    params = ncWMS.RequestParser(req.args)
    metadataItem = params.getParamValue("item")
    if (metadataItem == "datasets"):
        req.write(getDatasetsDiv())
    elif (metadataItem == "variables"):
        dataset = params.getParamValue("dataset")
        req.write(getVariables(dataset))
    elif (metadataItem == "variableDetails"):
        dataset = params.getParamValue("dataset")
        varID = params.getParamValue("variable")
        req.write(getVariableDetails(dataset, varID))
    elif (metadataItem == "calendar"):
        dataset = params.getParamValue("dataset")
        varID = params.getParamValue("variable")
        dateTime = params.getParamValue("dateTime")
        req.write(getCalendar(dataset, varID, dateTime))

def getDatasetsDiv():
    """ returns a string with a set of divs representing the datasets.
        Quick and dirty. """
    str = StringIO()
    for dataset in config.datasets:
        str.write("<div id=\"%sDiv\">" % dataset[0])
        str.write("<div id=\"%s\">%s</div>" % (dataset[0], dataset[1]))
        str.write("<div id=\"%sContent\">" % dataset[0])
        str.write("Variables in the %s dataset will appear here" % dataset[1])
        str.write("</div>")
        str.write("</div>")
    s = str.getvalue()
    str.close()
    return s

def getVariables(dataset):
    """ returns an HTML table containing a set of variables for the given dataset. """
    datasets = ncWMS.getDatasets()
    str = StringIO()
    str.write("<table cellspacing=\"0\"><tbody>")
    vars = nj22dataset.getVariables(datasets[dataset].location)
    for varID in vars.keys():
        str.write("<tr><td>")
        str.write("<a href=\"#\" onclick=\"javascript:variableSelected('%s', '%s')\">%s</a>" % (dataset, varID, vars[varID]))
        str.write("</td></tr>")
    str.write("</tbody></table>")
    s = str.getvalue()
    str.close()
    return s

def getVariableDetails(dataset, varID):
    """ returns an XML document containing the details of the given variable
        in the given dataset. """
    datasets = ncWMS.getDatasets()
    str = StringIO()
    var = nj22dataset.getVariableDetails(datasets[dataset].location, varID)
    str.write("<variableDetails dataset=\"%s\" variable=\"%s\" units=\"%s\">" % (dataset, var.title, var.units))
    str.write("<axes>")
    if var.zValues is not None:
        str.write("<axis type=\"z\" units=\"%s\">")
        for z in var.zValues:
            str.write("<value>%f</value>" % z)
        str.write("</axis>")
    str.write("</axes>")
    str.write("<range><min>%f</min><max>%f</max></range>" % (var.valid_min, var.valid_max))
    str.write("</variableDetails>")
    s = str.getvalue()
    str.close()
    return s

def getCalendar(dataset, varID, dateTime):
    """ returns an HTML calendar for the given dataset and variable.
        dateTime is a string in ISO 8601 format with the required
        'focus time' """
    datasets = ncWMS.getDatasets()
    # Get an array of time axis values in seconds since the epoch
    tValues = nj22dataset.getTimeAxisValues(datasets[dataset].location, varID)
    str = StringIO()
    str.write("%s\n" % iso8601.tostring(time.time()))
    for t in tValues:
        str.write("<value>%f</value>\n" % t)
    s = str.getvalue()
    str.close()
    return s