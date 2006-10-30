# routines that produce output for web pages (inc JSP pages)
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

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

def getDatasetsDiv():
    """ returns a string with a set of divs representing the datasets.
        Quick and dirty. """
    str = StringIO()
    for dataset in config.datasets:
        str.write("<div id=\"%sDiv\">" % dataset[0])
        str.write("<div id=\"%s\">%s</div>" % (dataset[0], dataset[1]))
        str.write("<div id=\"%sContent\">" % dataset[0])
        str.write("Variables in the %s dataset will appear here" % dataset[1])
        str.write("</div></div>")
    s = str.getvalue()
    str.close()
    return s

def getVariables(dataset):
    """ returns an HTML table containing a set of variables for the given dataset. """
    datasets = ncWMS.getDatasets()
    str = StringIO()
    str.write("<table cellspacing=\"0\"><tbody>")
    for title in nj22dataset.getVariableList(datasets[dataset].location):
        str.write("<tr><td>%s</td></tr>" % title)
    str.write("</tbody></table>")
    s = str.getvalue()
    str.close()
    return s
