//
// Javascript for GODIVA2 page.  Requires prototype.js and rico.js to be
// included in any page that uses this script
//

var layerName = '';
var prettyDsName = ''; // The dataset name, formatted for human reading
var zValue = 0; // The currently-selected depth *value* (not the index)
var zPositive = 0; // Will be 1 if the selected z axis is positive
var tValue = null; // The currently-selected time *value* as a string in yyyy-MM-ddThh:mm:ss format
var prettyTValue = null; // The t value, formatted for human reading
var isIE;
var scaleMinVal;
var scaleMaxVal;
var timestep = 0;
var newVariable = true;  // This will be true when we have chosen a new variable

// Called when the page has loaded
window.onload = function()
{
    // reset the scale markers
    $('scaleMax').value = '';
    $('scaleMin').value = '';

    // Make sure 100% opacity is selected
    $('opacityValue').value = '100';

    // Detect the browser (IE doesn't render PNGs properly so we don't provide
    // the option to have partial overlay opacity)
    isIE = navigator.appVersion.indexOf('MSIE') >= 0;

    // Set up the Google Maps widget
    setup_map($('map'));

    // Load the list of datasets to populate the left-hand menu
    loadDatasets('accordionDiv');
}

// Populates the left-hand menu with a set of datasets
function loadDatasets(dsDivId)
{
    GDownloadUrl('Metadata.py?item=datasets',
        function(data, responseCode) {
            $(dsDivId).innerHTML = data;
            var accordion = new Rico.Accordion
            (
                dsDivId,
                { onShowTab: datasetSelected, panelHeight: 200 }
            );
            // Make sure that the variables are loaded for the first data set
            datasetSelected( accordion.accordionTabs[0] );
        }
    );
}
    

// Called when a new tab has been selected in the left-hand menu
// TODO: Cache the results so we don't have to query the server again when the
// same dataset is selected in future?
// Gets the list of variables for a given dataset from the server and populates
// the correct panel in the left-hand menu
function datasetSelected(expandedTab)
{
    var dataset = expandedTab.titleBar.id;
    // Get the pretty-printed name of the dataset
    prettyDsName = expandedTab.titleBar.firstChild.nodeValue;
    // getVariables.jsp returns a table of variable names in HTML format
    GDownloadUrl('Metadata.py?item=variables&dataset=' + dataset,
        function(data, responseCode) {
            var xmldoc = GXml.parse(data);
            // set the size of the panel to match the number of variables
            var panel = $(dataset + 'Content');
            var varList = xmldoc.getElementsByTagName('tr');
            panel.style.height = varList.length * 20 + 'px';
            panel.innerHTML = data;
        }
    );
}

// Called when the user clicks on the name of a variable in the left-hand menu
// Gets the details (units, grid etc) of the given variable. 
function variableSelected(datasetName, variableName)
{
    newVariable = true;
    GDownloadUrl('Metadata.py?item=variableDetails&dataset=' + datasetName +
        '&variable=' + variableName,
        function(data, responseCode) {
            var xmldoc = GXml.parse(data);
            
            // See getVariableDetails.jsp
            var varDetails = xmldoc.getElementsByTagName('variableDetails')[0];
            // Set the global variables for dataset and variable name
            var dataset = varDetails.getAttribute('dataset');
            layerName = dataset + '/' + variableName;
            var units = varDetails.getAttribute('units');
            $('datasetName').innerHTML = prettyDsName;
            $('variableName').innerHTML = varDetails.getAttribute('variable');
            $('units').innerHTML = '<b>Units: </b>' + units;
            
            // clear the list of z values
            $('zValues').options.length = 0; 

            // Set the range selector objects
            var theAxes = xmldoc.getElementsByTagName('axis');
            for (var i = 0; i < theAxes.length; i++)
            {
                var axisType = theAxes[i].getAttribute('type');
                if (axisType == 'z')
                {
                    zPositive = parseInt(theAxes[i].getAttribute('positive'));
                    var zUnits = theAxes[i].getAttribute('units');
                    if (zPositive) {
                        $('zAxis').innerHTML = '<b>Elevation (' + zUnits + '): </b>';
                    } else {
                        $('zAxis').innerHTML = '<b>Depth (' + zUnits + '): </b>';
                    }
                    // Populate the drop-down list of z values
                    var values = theAxes[i].getElementsByTagName('value');
                    // Make z range selector invisible if there are no z values
                    $('zValues').style.visibility = (values.length == 0) ? 'hidden' : 'visible';
                    var zDiff = 1e10; // Set to some ridiculously-high value
                    var nearestIndex = 0;
                    for (var j = 0; j < values.length; j++)
                    {
                        var optionZValue = values[j].firstChild.nodeValue;
                        // Create an item in the drop-down list for this z level
                        $('zValues').options[j] = new Option(optionZValue, j);
                        // Find the nearest value to the currently-selected
                        // depth level
                        var diff = Math.abs(parseFloat(optionZValue) - zValue);
                        if (diff < zDiff)
                        {
                            zDiff = diff;
                            nearestIndex = j;
                        }
                    }
                    $('zValues').selectedIndex = nearestIndex;
                    var zFound = true;
                }
            }

            if (zFound) {
                $('zValues').style.visibility = 'visible';
            } else {
                $('zAxis').innerHTML = ''
                $('zValues').style.visibility = 'hidden';
            }
            
            $('scaleBar').style.visibility = 'visible';
            $('scaleMin').style.visibility = 'visible';
            $('scaleMax').style.visibility = 'visible';
            if (!isIE)
            {
                // Only show this control if we can use PNGs properly (i.e. not on Internet Explorer)
                $('opacityControl').style.visibility = 'visible';
            }

            // update the scale markers
            $('scaleMax').value = xmldoc.getElementsByTagName('max')[0].firstChild.nodeValue;
            $('scaleMin').value = xmldoc.getElementsByTagName('min')[0].firstChild.nodeValue;
            scaleMinVal = $('scaleMin').value;
            scaleMaxVal = $('scaleMax').value;
            
            // Get the currently-selected time and date or the current time if
            // none has been selected
            if (tValue == null)
            {
                var now = new Date();
                // Format the date in ISO8601 format
                tValue = now.getFullYear();
                tValue += '-' + (now.getMonth() < 9 ? '0' : '') + (now.getMonth() + 1);
                tValue += '-' + (now.getDate() < 10 ? '0' : '') + now.getDate();
                tValue += 'T00:00:00Z';
            }
            setCalendar(dataset, variableName, tValue);
        }
    );
}

// This requests a calendar for the given date and time for the given dataset
// and variable.  If there is data for the given date and time, this will
// return a calendar for the given month.  If there is no data for the given
// date and time, this will return a calendar for the nearest month.
function setCalendar(dataset, variable, dateTime)
{
    // Set the calendar. When the calendar arrives the map will be updated
    GDownloadUrl('Metadata.py?item=calendar&dataset=' +  dataset + 
        '&variable=' + variable + '&dateTime=' + dateTime,
        function(data, responseCode) {
            if (data == '') {
                // There is no calendar data.  Just update the map
                $('calendar').innerHTML = '';
                $('date').innerHTML = '';
                updateMap();
            }
            var xmldoc = GXml.parse(data);
            $('calendar').innerHTML =
                RicoUtil.getContentAsString(xmldoc.getElementsByTagName('calendar')[0]);
            
            // If this call has resulted from the selection of a new variable,
            // choose the timestep based on the result from the server
            if (newVariable)
            {
                newVariable = false; // This will be set true when we click on a different variable name
                var tIndex = parseInt(xmldoc.getElementsByTagName('nearestIndex')[0].firstChild.nodeValue);
                var tVal = xmldoc.getElementsByTagName('nearestValue')[0].firstChild.nodeValue;
                var prettyTVal = xmldoc.getElementsByTagName('prettyNearestValue')[0].firstChild.nodeValue;
                // Get the timesteps for this day and update the map
                getTimesteps(tIndex, tVal, prettyTVal);
            }
            else if ($('t' + timestep))
            {
                // Highlight the currently-selected timestep if it happens to
                // exist in this calendar
                $('t' + timestep).style.backgroundColor = '#dadee9';
            }
        }
    );
}

// Updates the time selector control.  Finds all the timesteps that occur on
// the same day as the timestep with the given index.   Called from the calendar
// control (see getCalendar.jsp)
function getTimesteps(tIndex, tVal, prettyTVal)
{
    // Forget about the time control for the moment.  Just set the new timestep
    if ($('t' + timestep))
    {
        $('t' + timestep).style.backgroundColor = 'white';
    }
    timestep = tIndex;
    tValue = tVal;
    $('t' + timestep).style.backgroundColor = '#dadee9';
    $('date').innerHTML = '<b>Date/time: </b>' + prettyTVal;
    updateMap();
    
    
    // Set the calendar. When the calendar arrives the map will be updated
    /*request.open('GET', 'getTimesteps.jsp?dataset=' + dataset + '&variable=' + 
        variable + '&tIndex=' + tIndex, true);
    
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            var xmldoc = GXml.parse(request.responseText);
            $('date').innerHTML = xmldoc.getElementsByTagName('date')[0].firstChild.nodeValue;
            var options = xmldoc.getElementsByTagName('option');
            for (var i = 0; i < options.length; i++)
            {
                $('tValues').options[i] = new Option(options[i].firstChild.nodeValue,
                    options[i].getAttribute('value'));
            }
            timestep = options[0].getAttribute('value');
            updateMap();
        }
    }
    request.send(null);*/
}

// Sets the timestep.  Called from the time selector control (see index.jsp)
/*function updateTimestep()
{
    timestep = $('tValues').value;
    alert('Setting timestep to ' + timestep);
    updateMap();
}*/

// Validates the entries for the scale bar
function validateScale()
{
    var fMin = parseFloat($('scaleMin').value);
    var fMax = parseFloat($('scaleMax').value);
    if (isNaN(fMin))
    {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMin').value = scaleMinVal;
    }
    else if (isNaN(fMax))
    {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMax').value = scaleMaxVal;
    }
    else if (fMin >= fMax)
    {
        alert('Minimum scale value must be less than the maximum');
        // Reset to the old values
        $('scaleMin').value = scaleMinVal;
        $('scaleMax').value = scaleMaxVal;
    }
    else   
    {
        $('scaleMin').value = fMin;
        $('scaleMax').value = fMax;
        scaleMinVal = fMin;
        scaleMaxVal = fMax;
        updateMap();
    }
}

function updateMap()
{    
    // Update the intermediate scale markers
    var scaleOneThird = parseFloat(scaleMinVal) + ((scaleMaxVal - scaleMinVal) / 3);
    var scaleTwoThirds = parseFloat(scaleMinVal) + (2 * (scaleMaxVal - scaleMinVal) / 3);
    $('scaleOneThird').innerHTML = toNSigFigs(scaleOneThird, 4);
    $('scaleTwoThirds').innerHTML = toNSigFigs(scaleTwoThirds, 4);

    // Get the z value
    var zIndex = $('zValues').selectedIndex;
    if ($('zValues').options.length == 0) {
        // If we have no depth information, assume we're at the surface.  This
        // will be ignored by the Google Maps server
        zValue = 0;
    } else {
        zValue = $('zValues').options[zIndex].firstChild.nodeValue;
    }
    if (!zPositive) {
        zValue = -zValue;
    }

    // Get the base url
    var url = serverURL + 'WMS.py?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0' +
        '&LAYERS=' + layerName + '&STYLES=&CRS=EPSG:41001&WIDTH=256&HEIGHT=256' +
        '&ELEVATION=' + zValue + '&TIME=' + tValue +
        '&FORMAT=image/png&SCALE=' + $('scaleMin').value + ',' + $('scaleMax').value;

    // Create the URL of a test image (single image of North Atlantic)
    var testImageURL = url + '&BBOX=-90,0,0,70';

    // Create the URL to launch this dataset in Google Earth
    var gEarthURL = serverURL + 'gearth/genkml1.kml?'
        + 'layer=' + layerName + '&z=' + zIndex +
        '&t=' + timestep + '&scale=' + $('scaleMin').value + ',' + $('scaleMax').value;

    $('imageURL').innerHTML = '<a href=\'' + testImageURL + '\'>link to test image</a>';
        //+ '&nbsp;&nbsp;&nbsp;<a href=\'' + gEarthURL + '\'>Open in Google Earth</a>';

    // Notify the Google Maps widget
    set_tile_url(url);
}

// Formats the given value to numSigFigs significant figures
function toNSigFigs(value, numSigFigs)
{
    var strValue = '' + value;
    var newValue = '';
    var firstSigFigPos = -1;
    var dpSeen = 0; // Will be 1 when we have seen the decimal point

    for (var i = 0; i < strValue.length; i++)
    {
        if (firstSigFigPos < 0)
        {
            // Haven't found the first significant figure yet
            newValue += strValue.charAt(i);
            if (strValue.charAt(i) != '0' && strValue.charAt(i) != '.'
                && strValue.charAt(i) != '-')
            {
                // We've found the first significant figure
                firstSigFigPos = i;
            }
        }
        else
        {
            // We don't want to count the decimal point as a sig fig!
            if (strValue.charAt(i) == '.')
            {
                dpSeen = 1;
            }
            if (i - firstSigFigPos < numSigFigs + dpSeen)
            {
                newValue += strValue.charAt(i);
            }
        }
    }
    return newValue;
}