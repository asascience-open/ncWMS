//
// Javascript for GODIVA2 page.
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
var autoLoad = null; // Will contain data for auto-loading data from a permalink
var bbox = null; // The bounding box of the currently-displayed layer

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

    // Detect the browser (IE6 doesn't render PNGs properly so we don't provide
    // the option to have partial overlay opacity)
    isIE = navigator.appVersion.indexOf('MSIE') >= 0;

    // Stop the pink tiles appearing on error
    OpenLayers.Util.onImageLoadError = function() {  this.style.display = ""; this.src="./images/blank.png"; }
    
    // Set up the OpenLayers map widget
    map = new OpenLayers.Map('map');
    var ol_wms = new OpenLayers.Layer.WMS( "OpenLayers WMS", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'basic', format: 'image/png' } );
    var bluemarble_wms = new OpenLayers.Layer.WMS( "Blue Marble", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite' } );
    var osm_wms = new OpenLayers.Layer.WMS( "Openstreetmap", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'osm-map' } );
    var human_wms = new OpenLayers.Layer.WMS( "Human Footprint", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'hfoot' } );
            
    // The SeaZone Web Map server
    var seazone_wms = new OpenLayers.Layer.WMS1_3("SeaZone bathymetry", "http://ws.cadcorp.com/seazone/wms.exe?",
        {layers: 'Bathymetry___Elevation.bds', transparent: 'true'});
    seazone_wms.setVisibility(false);
    
    map.addLayers([bluemarble_wms, ol_wms, osm_wms, human_wms, seazone_wms]);
    
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
    // Add a listener for GetFeatureInfo
    map.events.register('click', map, getFeatureInfo);
    
    if (window.location.search != '') {
        autoLoad = new Object();
        autoLoad.dataset = null;
        autoLoad.variable = null;
        // strip off the leading question mark
        var queryString = window.location.search.split('?')[1];
        var kvps = queryString.split('&');
        // TODO What is "for each" in Javascript?
        for (var i = 0; i < kvps.length; i++) {
            keyAndVal = kvps[i].split('=');
            if (keyAndVal.length > 1) {
                if (keyAndVal[0] == 'dataset') {
                    autoLoad.dataset = keyAndVal[1];
                } else if (keyAndVal[0] == 'variable') {
                    autoLoad.variable = keyAndVal[1];
                }
            }
        }
    }       

    // Load the list of datasets to populate the left-hand menu
    loadDatasets('accordionDiv');
}

// Event handler for when a user clicks on a map
function getFeatureInfo(e)
{
    if (essc_wms != null)
    {
        $('featureInfo').innerHTML = "Getting feature info...";
        var url = essc_wms.getFullRequestString({
            REQUEST: "GetFeatureInfo",
            BBOX: essc_wms.map.getExtent().toBBOX(),
            I: e.xy.x,
            J: e.xy.y,
            INFO_FORMAT: 'text/xml',
            QUERY_LAYERS: essc_wms.params.LAYERS,
            WIDTH: essc_wms.map.size.w,
            HEIGHT: essc_wms.map.size.h
            });
        OpenLayers.loadURL(url, '', this, gotFeatureInfo);
        Event.stop(e);
    }
}

// Called when we have received some feature info
function gotFeatureInfo(response)
{
    var xmldoc = response.responseXML;
    var lon = xmldoc.getElementsByTagName('longitude')[0];
    var lat = xmldoc.getElementsByTagName('latitude')[0];
    var val = xmldoc.getElementsByTagName('value')[0];
    if (lon) {
        $('featureInfo').innerHTML = "<b>Lon:</b> " + toNSigFigs(lon.firstChild.nodeValue, 4) + 
            "&nbsp;&nbsp;<b>Lat:</b> " + toNSigFigs(lat.firstChild.nodeValue, 4) + "&nbsp;&nbsp;<b>Value:</b> " +
            toNSigFigs(val.firstChild.nodeValue, 4);
    } else {
        $('featureInfo').innerHTML = "Can't get feature info data for this layer <a href=\"javascript:popUp('whynot.html')\">(why not?)</a>";
    }
}

function popUp(URL)
{
    day = new Date();
    id = day.getTime();
    eval("page" + id + " = window.open(URL, '" + id + "', 'toolbar=0,scrollbars=0,location=0,statusbar=0,menubar=0,resizable=1,width=200,height=200,left = 300,top = 300');");
}

// Populates the left-hand menu with a set of datasets
function loadDatasets(dsDivId)
{
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=datasets',
        function(req) {
            $(dsDivId).innerHTML = req.responseText;
            var accordion = new Rico.Accordion (
                dsDivId,
                { onShowTab: datasetSelected, panelHeight: 200 }
            );
            if (autoLoad != null && autoLoad.dataset != null) {
                // We are automatically loading a dataset from a permalink
                for (var i = 0; i < accordion.accordionTabs.length; i++) {
                    if (autoLoad.dataset == accordion.accordionTabs[i].titleBar.id) {
                        // TODO: why doesn't showExpanded() work as expected?
                        accordion.accordionTabs[i].showExpanded();
                        datasetSelected(accordion.accordionTabs[i]);
                        break;
                    }
                }
            } else {
                // Make sure that the variables are loaded for the first data set
                datasetSelected( accordion.accordionTabs[0] );
            }
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
    // returns a table of variable names in HTML format
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=variables&dataset=' + dataset,
        function(req) {
            var xmldoc = req.responseXML;
            // set the size of the panel to match the number of variables
            var panel = $(dataset + 'Content');
            var varList = xmldoc.getElementsByTagName('tr');
            panel.style.height = varList.length * 20 + 'px';
            panel.innerHTML = req.responseText;
            if (autoLoad != null && autoLoad.variable != null) {
                variableSelected(dataset, autoLoad.variable);
            }
        }
    );
}

// Called when the user clicks on the name of a variable in the left-hand menu
// Gets the details (units, grid etc) of the given variable. 
function variableSelected(datasetName, variableName)
{
    newVariable = true;
    resetAnimation();
    downloadUrl('WMS.py', 'SERVICE=WMS&REQUEST=GetMetadata&item=variableDetails&dataset=' + datasetName +
        '&variable=' + variableName,
        function(req) {
            var xmldoc = req.responseXML;
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
            
            // Set the auto-zoom box
            bbox = xmldoc.getElementsByTagName('bbox')[0].firstChild.nodeValue;
            $('autoZoom').innerHTML = "<a href=\"#\" onclick=\"javascript:map.zoomToExtent(new OpenLayers.Bounds(" + bbox + "));\">Fit data to window</a>";
            
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
            $('setFrames').style.visibility = 'visible';
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

function resetAnimation()
{
    hideAnimation();
    $('featureInfo').style.visibility = 'visible';
    $('setFrames').style.visibility = 'hidden';
    $('animation').style.visibility = 'hidden';
    $('firstFrame').innerHTML = '';
    $('lastFrame').innerHTML = '';
}
function setFirstAnimationFrame()
{
    $('firstFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
}
function setLastAnimationFrame()
{
    $('lastFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
}
function createAnimation()
{
    if ($('firstFrame').innerHTML == '' || $('lastFrame').innerHTML == '')
    {
        alert("Must select a first and last frame for the animation");
        return;
    }
    // Get a URL for a WMS request that covers the current map extent
    var urlEls = essc_wms.getURL(map.getExtent()).split('&');
    // Replace the parameters as needed.  We generate a map that is half the
    // width and height of the viewport, otherwise it takes too long
    var newURL = urlEls[0];
    for (var i = 1; i < urlEls.length; i++) {
        if (urlEls[i].startsWith('TIME=')) {
            newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
        } else if (urlEls[i].startsWith('FORMAT')) {
            newURL += '&FORMAT=image/gif';
        } else if (urlEls[i].startsWith('WIDTH')) {
            newURL += '&WIDTH=' + $('map').clientWidth / 2;
        } else if (urlEls[i].startsWith('HEIGHT')) {
            newURL += '&HEIGHT=' + $('map').clientHeight / 2;
        } else if (!urlEls[i].startsWith('OPACITY')) {
            // We remove the OPACITY ARGUMENT as GIFs do not support partial transparency
            newURL += '&' + urlEls[i];
        }
    }
    $('featureInfo').style.visibility = 'hidden';
    $('autoZoom').style.visibility = 'hidden';
    $('hideAnimation').style.visibility = 'visible';
    // We show the "please wait" image then immediately load the animation
    $('loadingAnimationDiv').style.visibility = 'visible'; // This will be hidden by animationLoaded()
    $('mapOverlay').src = newURL;
    $('mapOverlay').width = $('map').clientWidth;
    $('mapOverlay').height = $('map').clientHeight;
}
function animationLoaded()
{
    $('loadingAnimationDiv').style.visibility = 'hidden';
    $('mapOverlayDiv').style.visibility = 'visible';
    if (essc_wms != null) {
        essc_wms.setVisibility(false);
    }
}
function hideAnimation()
{
    if (essc_wms != null) {
        essc_wms.setVisibility(true);
    }
    $('autoZoom').style.visibility = 'visible';
    $('hideAnimation').style.visibility = 'hidden';
    $('mapOverlayDiv').style.visibility = 'hidden';
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
    // Buffer is set to 1 to avoid loading a large halo of tiles outside the
    // current viewport
    var baseURL = window.location.href.split("/").slice(0,-1).join("/");
    if (essc_wms == null) {
        essc_wms = new OpenLayers.Layer.WMS1_3("ESSC WMS",
            baseURL + '/WMS.py', {layers: layerName, elevation: zValue, time: tValue,
            transparent: 'true', scale: scaleMinVal + "," + scaleMaxVal,
            opacity: opacity}, {buffer: 1});
        map.addLayers([essc_wms]);
    } else {
        essc_wms.mergeNewParams({layers: layerName, elevation: zValue, time: tValue,
            scale: scaleMinVal + "," + scaleMaxVal, opacity: opacity});
    }
    
    $('featureInfo').innerHTML = "Click on the map to get more information";
    $('featureInfo').style.visibility = 'visible';
    
    var bboxEls = bbox.split(",");
    var bounds = new OpenLayers.Bounds(parseFloat(bboxEls[0]), 
        parseFloat(bboxEls[1]), parseFloat(bboxEls[2]), parseFloat(bboxEls[3]));
    var imageURL = essc_wms.getURL(bounds);
    $('imageURL').innerHTML = '<a href=\'' + imageURL + '\'>link to test image</a>'
        + '&nbsp;&nbsp;<a href=\'WMS.py?SERVICE=WMS&REQUEST=GetKML&LAYERS=' + layerName + 
        '&STYLES=&ELEVATION=' + zValue + '&TIME=' + tValue + 
        '&SCALE=' + scaleMinVal + ',' + scaleMaxVal + '\'>Open in Google Earth</a>';
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