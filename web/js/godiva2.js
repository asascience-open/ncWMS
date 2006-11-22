//
// Javascript for GODIVA2 page.  Requires prototype.js and rico.js to be
// included in any page that uses this script
//

var map = null;
var layerName = '';
var prettyDsName = ''; // The dataset name, formatted for human reading
var zValue = 0; // The currently-selected depth *value* (not the index)
var zPositive = 0; // Will be 1 if the selected z axis is positive
var tValue = null;
var prettyTValue = null; // The t value, formatted for human reading
var isIE;
var scaleMinVal;
var scaleMaxVal;
var timestep = 0;
var newVariable = true;  // This will be true when we have chosen a new variable
var essc_wms = null; // The WMS layer for the ocean data

// Ajax call using the Prototype library
// url: The URL of the data source
// params: The parameters to append to the URL
// onsuccess: A function that will be called with the original request object
function downloadUrl(url, params, onsuccess)
{
    var myAjax = new Ajax.Request(
        url, 
        {
            method: 'get', 
            parameters: params, 
            onComplete: onsuccess
        });
}

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

    // Set up the OpenLayers map widget
    map = new OpenLayers.Map('map');
    var ol_wms = new OpenLayers.Layer.WMS( "OpenLayers WMS", 
        "http://labs.metacarta.com/wms/vmap0?", {layers: 'basic'} );
    var jpl_wms = new OpenLayers.Layer.WMS( "NASA Global Mosaic",
        "http://wms.jpl.nasa.gov/wms.cgi?", {layers: "modis,global_mosaic"});
    var seazone_wms = new OpenLayers.Layer.WMS1_3("SeaZone", "http://ws.cadcorp.com/seazone/wms.exe?",
        {layers: 'Barts_50km', transparent: 'true'});
    seazone_wms.setVisibility(false);
    map.addLayers([ol_wms, jpl_wms]); //, seazone_wms]);
    
    // If we have loaded Google Maps and the browser is compatible, add it as a base layer
    if (typeof GBrowserIsCompatible == 'function' && GBrowserIsCompatible()) {
        var gmapLayer = new OpenLayers.Layer.Google("Google Maps (satellite)", {type: G_SATELLITE_MAP});
        var gmapLayer2 = new OpenLayers.Layer.Google("Google Maps (political)", {type: G_NORMAL_MAP});
        map.addLayers([gmapLayer, gmapLayer2]);
    }
        
    map.addControl(new OpenLayers.Control.LayerSwitcher());
    // For some reason we have to call zoomToMaxExtent() before calling zoomTo()
    map.zoomToMaxExtent();
    map.zoomTo(1);
    
    // Add a listener for changing the base map
    //map.events.register("changebaselayer", map, function() { alert(this.projection) });

    // Load the list of datasets to populate the left-hand menu
    loadDatasets('accordionDiv');
}

// Populates the left-hand menu with a set of datasets
function loadDatasets(dsDivId)
{
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=datasets',
        function(req) {
            $(dsDivId).innerHTML = req.responseText;
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
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=variables&dataset=' + dataset,
        function(req) {
            var xmldoc = req.responseXML;
            // set the size of the panel to match the number of variables
            var panel = $(dataset + 'Content');
            var varList = xmldoc.getElementsByTagName('tr');
            panel.style.height = varList.length * 20 + 'px';
            panel.innerHTML = req.responseText;
        }
    );
}

// Called when the user clicks on the name of a variable in the left-hand menu
// Gets the details (units, grid etc) of the given variable. 
function variableSelected(datasetName, variableName)
{
    newVariable = true;
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=variableDetails&dataset=' + datasetName +
        '&variable=' + variableName,
        function(req) {
            var xmldoc = req.responseXML;
            
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
                        var diff;
                        // This is nasty: improve!
                        if (zPositive) {
                            diff = Math.abs(parseFloat(optionZValue) - zValue);
                        } else {
                            diff = Math.abs(parseFloat(optionZValue) + zValue);
                        }
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
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=calendar&dataset=' +  dataset + 
        '&variable=' + variable + '&dateTime=' + dateTime,
        function(req) {
            if (req.responseText == '') {
                // There is no calendar data.  Just update the map
                $('calendar').innerHTML = '';
                $('date').innerHTML = '';
                $('time').innerHTML = '';
                $('utc').style.visibility = 'hidden';
                updateMap();
                return;
            }
            var xmldoc = req.responseXML;
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
                getTimesteps(dataset, variable, tIndex, tVal, prettyTVal);
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
function getTimesteps(dataset, variable, tIndex, tVal, prettyTVal)
{
    $('date').innerHTML = '<b>Date/time: </b>' + prettyTVal;
    $('utc').style.visibility = 'visible';
    
    // Get the timesteps
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=timesteps&dataset=' +  dataset + 
        '&variable=' + variable + '&tIndex=' + tIndex,
        function(req) {
            $('time').innerHTML = req.responseText; // the data will be a selection box
            // Make sure the correct day is highlighted in the calendar
            // TODO: doesn't work if there are many timesteps on the same day!
            if ($('t' + timestep))
            {
                $('t' + timestep).style.backgroundColor = 'white';
            }
            timestep = tIndex;
            if ($('t' + timestep))
            {
                $('t' + timestep).style.backgroundColor = '#dadee9';
            }
            updateMap();
        }
    );
}

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
        // will be ignored by the map server
        zValue = 0;
    } else {
        zValue = $('zValues').options[zIndex].firstChild.nodeValue;
    }
    if (!zPositive) {
        zValue = -zValue;
    }
    
    if ($('tValues')) {
        tValue = $('tValues').value;
    }
    
    var opacity = $('opacityValue').value;

    // Notify the OpenLayers widget
    // SCALE=minval,maxval is a non-standard extension to WMS, describing how
    // the map is to be coloured.
    // OPACITY=[0,100] is another non-standard extension to WMS, giving the opacity
    // of the data pixels
    // TODO get the map projection from the base layer
    // TODO use a more informative title
    if (essc_wms == null) {
        essc_wms = new OpenLayers.Layer.WMS1_3("ESSC WMS",
            serverURL + 'WMS.py', {layers: layerName, elevation: zValue, time: tValue,
            transparent: 'true', scale: scaleMinVal + "," + scaleMaxVal,
            opacity: opacity});
        map.addLayers([essc_wms]);
    } else {
        essc_wms.mergeNewParams({layers: layerName, elevation: zValue, time: tValue,
            scale: scaleMinVal + "," + scaleMaxVal, opacity: opacity});
    }
    var imageURL = essc_wms.getURL(new OpenLayers.Bounds(-90,0,0,70));
    $('imageURL').innerHTML = '<a href=\'' + imageURL + '\'>link to test image</a>';
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