# routines that produce output (HTML or XML) for web pages
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from xml.utils import iso8601
import time, math
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
        req.content_type = "text/xml"
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
    # TODO: check for tValues == None
    str = StringIO()

    # Find the closest time step to the given dateTime value
    # TODO: binary search would be more efficient
    reqTime = iso8601.parse(dateTime) # Gives seconds since the epoch
    diff = 1e20
    nearestIndex = 0
    for i in xrange(len(tValues)):
        testDiff = math.fabs(tValues[i] - reqTime)
        if testDiff < diff:
            # Axis is monotonic so we should move closer and closer
            # to the nearest value
            diff = testDiff
            nearestIndex = i
        elif i > 0:
            # We've moved past the closest date
            break
    
    str.write("<root>")
    str.write("<nearestValue>%s</nearestValue>" % iso8601.tostring(tValues[nearestIndex]))
    # TODO pretty-printed value for display
    str.write("<prettyNearestValue>%s</prettyNearestValue>" % iso8601.tostring(tValues[nearestIndex]))
    # TODO: do we need this?
    str.write("<nearestIndex>%d</nearestIndex>" % nearestIndex)

    # Now print out the calendar in HTML
    str.write("<calendar>")
    str.write("<table><tbody>")
    # Add the navigation buttons at the top of the month view
    str.write("<tr>")
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&lt;&lt;</a></td>" % (dataset, varID, _getYearBefore(tValues[i])))
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&lt;</a></td>" % (dataset, varID, varID))
    str.write("<td colspan=\"3\">%s</td>" % ("heading")) # TODO
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&gt;</a></td>" % (dataset, varID, varID))
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&gt;&gt;</a></td>" % (dataset, varID, _getYearAfter(tValues[i])))
    str.write("</tr>")
    # Add the day-of-week headings
    str.write("<tr><th>S</th><th>M</th><th>T</th><th>W</th><th>T</th><th>F</th><th>S</th></tr>")
    # TODO: add the calendar body

    str.write("</tbody></table>")
    str.write("</calendar>")
    str.write("</root>")

    s = str.getvalue()
    str.close()
    return s

def _getYearBefore(date):
    """ Returns an ISO8601-formatted date which is exactly one year earlier than
        the given date, expressed in seconds since the epoch """
    # Get the tuple of year, month, day etc
    tup = time.gmtime(date)
    newDate = tuple([tup[0] - 1] + list(tup[1:]))
    return iso8601.tostring(time.mktime(newDate))

def _getYearAfter(date):
    """ Returns an ISO8601-formatted date which is exactly one year later than
        the given date, expressed in seconds since the epoch """
    # Get the tuple of year, month, day etc
    tup = time.gmtime(date)
    newDate = tuple([tup[0] + 1] + list(tup[1:]))
    return iso8601.tostring(time.mktime(newDate))
    