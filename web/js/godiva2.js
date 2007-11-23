//
// Javascript for GODIVA2 page.
//

var map = null;
var layerName = ''; // The unique name of the currently-displayed WMS layer
var zPositive = false; // Will be true if the selected z axis is positive
var calendar = null; // The calendar object
var datesWithData = null; // Will be populated with the dates on which we have data
                          // for the currently-selected variable
var isoTValue = null; // The currently-selected t value (ISO8601)
var isIE;
var scaleMinVal;
var scaleMaxVal;
var newVariable = true;  // This will be true when we have chosen a new variable
var essc_wms = null; // The WMS layer for the ocean data
var autoLoad = null; // Will contain data for auto-loading data from a permalink
var bbox = null; // The bounding box of the currently-displayed layer
var featureInfoUrl = null; // The last-called URL for getFeatureInfo (following a click on the map)

var servers = ['']; // Means that data will only be loaded from this server,
                    // unless changed later (e.g. by loading in external javascript)
var activeServer; // The server (url) that is serving the currently-selected layer (see dataSources.js)

var tree = null; // The tree control in the left-hand panel

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
        
    // ESSI WMS (see Stefano Nativi's email to me, Feb 15th)
    /*var essi_wms = new OpenLayers.Layer.WMS.Untiled( "ESSI WMS", 
        "http://athena.pin.unifi.it:8080/ls/servlet/LayerService?",
        {layers: 'sst(time-lat-lon)-T0', transparent: 'true' } );
    essi_wms.setVisibility(false);*/
            
    // The SeaZone Web Map server
    var seazone_wms = new OpenLayers.Layer.WMS1_3("SeaZone bathymetry", "http://ws.cadcorp.com/seazone/wms.exe?",
        {layers: 'Bathymetry___Elevation.bds', transparent: 'true'});
    seazone_wms.setVisibility(false);
    
    map.addLayers([bluemarble_wms, ol_wms, osm_wms, human_wms, seazone_wms/*, essi_wms*/]);
    
    // Make sure the Google Earth and Permalink links are kept up to date when
    // the map is moved or zoomed
    map.events.register('moveend', map, setGEarthURL);
    map.events.register('moveend', map, setPermalinkURL);
    
    // If we have loaded Google Maps and the browser is compatible, add it as a base layer
    if (typeof GBrowserIsCompatible == 'function' && GBrowserIsCompatible()) {
        var gmapLayer = new OpenLayers.Layer.Google("Google Maps (satellite)", {type: G_SATELLITE_MAP});
        var gmapLayer2 = new OpenLayers.Layer.Google("Google Maps (political)", {type: G_NORMAL_MAP});
        map.addLayers([gmapLayer, gmapLayer2]);
    }
        
    map.addControl(new OpenLayers.Control.LayerSwitcher());
    //map.addControl(new OpenLayers.Control.MousePosition({prefix: 'Lon: ', separator: ' Lat:'}));
    map.zoomTo(1);
    
    // Add a listener for changing the base map
    //map.events.register("changebaselayer", map, function() { alert(this.projection) });
    // Add a listener for GetFeatureInfo
    map.events.register('click', map, getFeatureInfo);
    
    var menu = ''; // This will be set if we're requesting a specific menu
    // see if we are recreating a view from a permalink
    if (window.location.search != '') {
        autoLoad = new Object();
        autoLoad.dataset = null;
        autoLoad.variable = null;
        autoLoad.zValue = null;
        autoLoad.isoTValue = null;
        autoLoad.bbox = null;
        autoLoad.scaleMin = null;
        autoLoad.scaleMax = null;
        // strip off the leading question mark
        var queryString = window.location.search.split('?')[1];
        var kvps = queryString.split('&');
        for (var i = 0; i < kvps.length; i++) {
            keyAndVal = kvps[i].split('=');
            if (keyAndVal.length > 1) {
                var key = keyAndVal[0].toLowerCase();
                if (key == 'dataset') {
                    autoLoad.dataset = keyAndVal[1];
                } else if (key == 'variable') {
                    autoLoad.variable = keyAndVal[1];
                } else if (key == 'elevation') {
                    autoLoad.zValue = keyAndVal[1];
                } else if (key == 'time') {
                    autoLoad.isoTValue = keyAndVal[1];
                } else if (key == 'bbox') {
                    autoLoad.bbox = keyAndVal[1];
                } else if (key == 'scale') {
                    autoLoad.scaleMin = keyAndVal[1].split(',')[0];
                    autoLoad.scaleMax = keyAndVal[1].split(',')[1];
                } else if (key == 'menu') {
                    // we must adapt the site for this brand (e.g. by showing only
                    // certain datasets)
                    menu = keyAndVal[1];
                }
            }
        }
    }
    // We haven't loaded the menu from elsewhere
    setupTreeControl(menu);
}

function setupTreeControl(menu)
{
    tree = new YAHOO.widget.TreeView('layerSelector');
    
    if (menu == '') {
        // We're populating the menus automatically based on the hierarchy
        // returned by the server
        // The servers can be specified in dataSources.js but if not, we'll just
        // use the default server
        if (typeof servers == 'undefined' || servers == null) {
            servers = [''];
        }
        
        // Add a root node in the tree for each server
        for (var i = 0; i < servers.length; i++) {
            var layerRootNode = new YAHOO.widget.TextNode(
                {label: "Loading ...", server: servers[i]},
                tree.getRoot(),
                servers.length == 1 // Only show expanded if this is the only server
            );
            layerRootNode.multiExpand = false;
            // The getLayerHierarchy() function is asynchronous.  Once we have received
            // the result from the server we shall pass it to the makeLayerMenu() function.
            getLayerHierarchy(servers[i], {
                callback : makeLayerMenu // Takes two arguments: the returned layers and the server object
            });
        }
    } else {
        // We're requesting a specific menu hierarchy
        getLayerHierarchy('', { // We are loading from the host server
            callback: function(layerHierarchy, url) {
                addNodes(tree.getRoot(), null, layerHierarchy.children);
                tree.draw();
            },
            menu: menu
        });
    }
        
    // Add an event callback that gets fired when a tree node is clicked
    tree.subscribe('labelClick', function(node) {
        if (typeof node.data.id != 'undefined') {
            // Set the currently-active server
            activeServer = node.data.server;
            // We're only interested if this is a displayable layer, i.e. it has an id.
            
            // Update the breadcrumb trail
            var s = node.data.label;
            var theNode = node;
            while(theNode.parent != tree.getRoot()) {
                theNode = theNode.parent;
                s = theNode.data.label + ' &gt; ' + s;
            }
            $('layerPath').innerHTML = s;
            
            // Store the layer name for later use
            layerName = node.data.id;
            
            // See if we're auto-loading a certain time value
            if (autoLoad != null && autoLoad.isoTValue != null) {
                isoTValue = autoLoad.isoTValue;
            } else if (isoTValue == null ) {
                // Set to the present time if we don't already have a time selected
                isoTValue = new Date().print('%Y-%m-%dT%H:%M:%SZ');
            }
            
            // Get the details of this layer from the server, calling layerSelected()
            // when we have the result
            var layerDetails = getLayerDetails(activeServer, {
                callback: layerSelected,
                layerName: node.data.id,
                time: isoTValue
            });
        }
    });
    tree.draw();
}

// Function that is used by the calendar to see whether a date should be disabled
function isDateDisabled(date, year, month, day)
{
    // datesWithData is a hash of year numbers mapped to a hash of month numbers
    // to an array of day numbers, i.e. {2007 : {0 : [3,4,5]}}.
    // Month numbers are zero-based.
    if (datesWithData == null ||
        datesWithData[year] == null || 
        datesWithData[year][month] == null) {
        // No data for this year or month
        return true;
    }
    // Cycle through the array of days for this month, looking for the one we want
    var numDays = datesWithData[year][month].length;
    for (var d = 0; d < numDays; d++) {
        if (datesWithData[year][month][d] == day) return false; // We have data for this day
    }
    // If we've got this far, we've found no data
    return true;
}

// Event handler for when a user clicks on a map
function getFeatureInfo(e)
{
    if (essc_wms != null)
    {
        $('featureInfo').innerHTML = "Getting feature info...";
        var params = {
            REQUEST: "GetFeatureInfo",
            BBOX: essc_wms.map.getExtent().toBBOX(),
            I: e.xy.x,
            J: e.xy.y,
            INFO_FORMAT: 'text/xml',
            QUERY_LAYERS: essc_wms.params.LAYERS,
            WIDTH: essc_wms.map.size.w,
            HEIGHT: essc_wms.map.size.h
        };
        if (activeServer != '') {
            // This is the signal to the server to load the data from elsewhere
            params.url = activeServer;
        }
        featureInfoUrl = essc_wms.getFullRequestString(
            params,
            'wms' // We must always load from the home server
        );
        OpenLayers.loadURL(featureInfoUrl, '', this, gotFeatureInfo);
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
        $('featureInfo').innerHTML = "<b>Lon:</b> " + lon.firstChild.nodeValue + 
            "&nbsp;&nbsp;<b>Lat:</b> " + lat.firstChild.nodeValue + "&nbsp;&nbsp;<b>Value:</b> " +
            toNSigFigs(parseFloat(val.firstChild.nodeValue), 4);
        if (timeSeriesSelected()) {
            // Construct a GetFeatureInfo request for the timeseries plot
            // Get a URL for a WMS request that covers the current map extent
            var urlEls = featureInfoUrl.split('&');
            // Replace the parameters as needed.  We generate a map that is half the
            // width and height of the viewport, otherwise it takes too long
            var newURL = urlEls[0];
            for (var i = 1; i < urlEls.length; i++) {
                if (urlEls[i].startsWith('TIME=')) {
                    newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
                } else if (urlEls[i].startsWith('INFO_FORMAT')) {
                    newURL += '&INFO_FORMAT=image/png';
                } else {
                    newURL += '&' + urlEls[i];
                }
            }
            // Image will be 400x300, need to allow a little elbow room
            $('featureInfo').innerHTML += "&nbsp;&nbsp;<a href='#' onclick=popUp('"
                + newURL + "',450,350)>Create timeseries plot</a>";
        }
    } else {
        $('featureInfo').innerHTML = "Can't get feature info data for this layer <a href=\"javascript:popUp('whynot.html', 200, 200)\">(why not?)</a>";
    }
}

function popUp(url, width, height)
{
    var day = new Date();
    var id = day.getTime();
    window.open(url, id, 'toolbar=0,scrollbars=0,location=0,statusbar=0,menubar=0,resizable=1,width='
        + width + ',height=' + height + ',left = 300,top = 300');
}

// Populates the left-hand menu with a hierarchy of layers
function makeLayerMenu(layerHierarchy, server)
{
    // Find the root node for this server
    var node = tree.getNodeByProperty('server', server);
    if (node == null) {
        alert("Internal error: can't find server node");
        return;
    }
    node.data.label = layerHierarchy.title;
    node.label = layerHierarchy.title;
    // Add layers recursively.
    addNodes(node, server, layerHierarchy.layers);
    tree.draw();
}

// Recursive method to add nodes to the layer selector tree control
function addNodes(parentNode, server, layerArray)
{
    for (var i = 0; i < layerArray.length; i++) {
        var layer = layerArray[i];
        if (server != null) {
            // If we're adding nodes from a manually-specified hierarchy
            // (set in the "menu" variable) then server will be null because
            // the server has already been set on the layer object.
            layer.server = server;
        }
        // The treeview control uses the layer.label string for display
        var newNode = new YAHOO.widget.TextNode(layer, parentNode, false);
        if (typeof layer.children != 'undefined') {
            newNode.multiExpand = false;
            addNodes(newNode, server, layer.children);
        }
    }
}

// Called when the user clicks on the name of a displayable layer in the left-hand menu
// Gets the details (units, grid etc) of the given layer. 
function layerSelected(layerDetails)
{
    // TODO: what do we do with these two?
    newVariable = true;
    resetAnimation();
    
    // Units are ncWMS-specific
    var isNcWMS = false;
    if (typeof layerDetails.units != 'undefined') {
        $('units').innerHTML = '<b>Units: </b>' + layerDetails.units;
        isNcWMS = true;
    } else {
        $('units').innerHTML = '';
    }

    // clear the list of z values
    $('zValues').options.length = 0; 

    // Set the range selector objects
    if (autoLoad == null || autoLoad.zValue == null) {
        var zValue = getZValue();
    } else {
        var zValue = parseFloat(autoLoad.zValue);
    }

    var zAxis = layerDetails.zaxis;
    if (zAxis == null) {
        $('zAxis').innerHTML = ''
        $('zValues').style.visibility = 'hidden';
    } else {
        if (zAxis.positive) {
            $('zAxis').innerHTML = '<b>Elevation (' + zAxis.units + '): </b>';
        } else {
            $('zAxis').innerHTML = '<b>Depth (' + zAxis.units + '): </b>';
        }
        // Populate the drop-down list of z values
        // Make z range selector invisible if there are no z values
        var zValues = zAxis.values;
        zPositive = zAxis.positive;
        $('zValues').style.visibility = (zValues.length == 0) ? 'hidden' : 'visible';
        var zDiff = 1e10; // Set to some ridiculously-high value
        var nearestIndex = 0;
        for (var j = 0; j < zValues.length; j++) {
            // Create an item in the drop-down list for this z level
            $('zValues').options[j] = new Option(zValues[j], j);
            // Find the nearest value to the currently-selected
            // depth level
            var diff;
            // This is nasty: improve!
            if (zPositive) {
                diff = Math.abs(parseFloat(zValues) - zValue);
            } else {
                diff = Math.abs(parseFloat(zValues) + zValue);
            }
            if (diff < zDiff) {
                zDiff = diff;
                nearestIndex = j;
            }
        }
        $('zValues').selectedIndex = nearestIndex;
        $('zValues').style.visibility = 'visible';
    }
    
    // Only show the scale bar if the data are coming from an ncWMS server
    var scaleVisibility = isNcWMS ? 'visible' : 'hidden';
    // TODO: could put these in a container and make all (in)visible at the same time
    $('scaleBar').style.visibility = scaleVisibility;
    $('scaleMin').style.visibility = scaleVisibility;
    $('scaleMax').style.visibility = scaleVisibility;
    $('autoScale').style.visibility = scaleVisibility;
    // Set the scale value
    scaleMinVal = layerDetails.scaleRange[0];
    scaleMaxVal = layerDetails.scaleRange[1];
    $('scaleMin').value = toNSigFigs(scaleMinVal, 4);
    $('scaleMax').value = toNSigFigs(scaleMaxVal, 4);
    
    if (!isIE) {
        // Only show this control if we can use PNGs properly (i.e. not on Internet Explorer)
        $('opacityControl').style.visibility = 'visible';
    }

    // Set the auto-zoom box
    bbox = layerDetails.bbox;
    $('autoZoom').innerHTML = '<a href="#" onclick="map.zoomToExtent(new OpenLayers.Bounds(' +
        bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3] +
        '));\">Fit layer to window</a>';

    // Now set up the calendar control
    if (layerDetails.datesWithData == null) {
        // There is no calendar data.  Just update the map
        if (calendar != null) calendar.hide();
        $('date').innerHTML = '';
        $('time').innerHTML = '';
        $('utc').style.visibility = 'hidden';
        updateMap();
    } else {
        datesWithData = layerDetails.datesWithData; // Tells the calendar which dates to disable
        if (calendar == null) {
            // Set up the calendar
            calendar = Calendar.setup({
                flat : 'calendar', // ID of the parent element
                align : 'bl', // Aligned to top-left of parent element
                weekNumbers : false,
                flatCallback : dateSelected
            });
            // For some reason, if we add this to setup() things don't work
            // as expected (dates not selectable on web page when first loaded).
            calendar.setDateStatusHandler(isDateDisabled);
        }
        // Set the range of valid years in the calendar.  Look through
        // the years for which we have data, finding the min and max
        var minYear = 100000000;
        var maxYear = -100000000;
        for (var year in datesWithData) {
            if (typeof datesWithData[year] != 'function') { // avoid built-in functions
                if (year < minYear) minYear = year;
                if (year > maxYear) maxYear = year;
            }
        }
        calendar.setRange(minYear, maxYear);
        // Get the time on the t axis that is nearest to the currently-selected
        // time, as calculated on the server
        calendar.setDate(layerDetails.nearestTime);
        calendar.refresh();
        calendar.show();
        // Load the timesteps for this date
        loadTimesteps();
    }
}

// Function that is called when a user clicks on a date in the calendar
function dateSelected(cal)
{
    if (cal.dateClicked) {
        loadTimesteps();
    }
}

// Updates the time selector control.  Finds all the timesteps that occur on
// the same day as the currently-selected date.  Called from the calendar
// control when the user selects a new date
function loadTimesteps()
{
    // Print out date, e.g. "15 Oct 2007"
    $('date').innerHTML = '<b>Date/time: </b>' + calendar.date.print('%d %b %Y');

    // Get the timesteps for this day
    getTimesteps(activeServer, {
        callback: updateTimesteps,
        layerName: layerName,
        day: makeIsoDate(calendar.date)
    });
}

// Gets an ISO Date ("yyyy-mm-dd") for the given Javascript date object.
// Does not contain the time.
function makeIsoDate(date)
{
    // Watch out for low-numbered years when calculating the ISO string
    var prefix = '';
    var year = date.getFullYear();
    if (year < 10) prefix = '000';
    else if (year < 100) prefix = '00';
    else if (year < 1000) prefix = '0';
    return prefix + date.print('%Y-%m-%d'); // Date only (no time) in ISO format
}

// Called when we have received the timesteps from the server
function updateTimesteps(times)
{
    // We'll get back a JSON array of ISO8601 times ("hh:mm:ss", UTC, no date information)
    // Build the select box
    var s = '<select id="tValues" onchange="javascript:updateMap()">';
    for (var i = 0; i < times.length; i++) {
        // Construct the full ISO Date-time
        var isoDateTime = makeIsoDate(calendar.date) + 'T' + times[i] + 'Z';
        s += '<option value="' + isoDateTime + '">' + times[i] + '</option>';
    }
    s += '</select>';

    $('time').innerHTML = s;
    $('utc').style.visibility = 'visible';

    // If we're autoloading, set the right time in the selection box
    if (autoLoad != null && autoLoad.isoTValue != null) {
        var timeSelect = $('tValues');
        for (var i = 0; i < timeSelect.options.length; i++) {
            if (timeSelect.options[i].value == autoLoad.isoTValue) {
                timeSelect.selectedIndex = i;
                break;
            }
        }
    }
    $('setFrames').style.visibility = 'visible';

    if (autoLoad != null && autoLoad.scaleMin != null && autoLoad.scaleMax != null) {
        $('scaleMin').value = autoLoad.scaleMin;
        $('scaleMax').value = autoLoad.scaleMax;
        validateScale(); // this calls updateMap()
    } else {
        updateMap(); // Update the map without changing the scale
    }
}

// Calls the WMS to find the min and max data values, then rescales.
// If this is a newly-selected variable the method gets the min and max values
// for the whole layer.  If not, this gets the min and max values for the viewport.
function autoScale()
{
    var dataBounds = bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3];
    if ($('tValues')) {
        isoTValue = $('tValues').value;
    }
    if (newVariable) {
        newVariable = false; // This will be set true when we click on a different variable name
    } else {
        // Use the intersection of the viewport and the layer's bounding box
        dataBounds = getIntersectionBBOX();
    }
    getMinMax(activeServer, {
        callback: gotMinMax,
        layerName: layerName,
        bbox: dataBounds,
        elevation: getZValue(),
        time: isoTValue
    });
}

// This function is called when we have received the min and max values from the server
function gotMinMax(minmax)
{
    $('scaleMin').value = toNSigFigs(minmax.min, 4);
    $('scaleMax').value = toNSigFigs(minmax.max, 4);
    validateScale(); // This calls updateMap()
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
    $('setFrames').style.visibility = 'hidden';
    $('animation').style.visibility = 'hidden';
    $('firstFrame').innerHTML = '';
    $('lastFrame').innerHTML = '';
}
function setFirstAnimationFrame()
{
    $('firstFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
    setGEarthURL();
}
function setLastAnimationFrame()
{
    $('lastFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
    setGEarthURL();
}
function createAnimation()
{
    if (!timeSeriesSelected()) {
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
    $('featureInfo').style.visibility = 'visible';
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
    
    if ($('tValues')) {
        isoTValue = $('tValues').value;
    }
    
    var opacity = $('opacityValue').value;
    
    // Set the map bounds automatically
    if (autoLoad != null && autoLoad.bbox != null) {
        map.zoomToExtent(getBounds(autoLoad.bbox));
    }
    
    // Make sure the autoLoad object is cleared
    autoLoad = null;

    // Notify the OpenLayers widget
    // SCALE=minval,maxval is a non-standard extension to WMS, describing how
    // the map is to be coloured.
    // OPACITY=[0,100] is another non-standard extension to WMS, giving the opacity
    // of the data pixels
    // TODO get the map projection from the base layer
    // TODO use a more informative title
    // Buffer is set to 1 to avoid loading a large halo of tiles outside the
    // current viewport
    if (essc_wms == null) {
        // If this were an Untiled layer we could control the ratio of image
        // size to viewport size with "{buffer: 1, ratio: 1.5}"
        essc_wms = new OpenLayers.Layer.WMS1_3("ESSC WMS",
            activeServer == '' ? 'wms' : activeServer, {
            layers: layerName,
            elevation: getZValue(),
            time: isoTValue,
            transparent: 'true',
            // TODO: provide option to choose STYLE on web interface.
            styles: 'boxfill;scale:' + scaleMinVal + ':' + scaleMaxVal + ';opacity:' + opacity},
            {buffer: 1, ratio: 1.5}
        );
        map.addLayers([essc_wms]);
    } else {
        essc_wms.url = activeServer == '' ? 'wms' : activeServer;
        essc_wms.mergeNewParams({
            layers: layerName,
            elevation: getZValue(),
            time: isoTValue,
            styles: 'boxfill;scale:' + scaleMinVal + ':' + scaleMaxVal + ';opacity:' + opacity
        });
    }
    
    $('featureInfo').innerHTML = "Click on the map to get more information";
    $('featureInfo').style.visibility = 'visible';
    
    var imageURL = essc_wms.getURL(new OpenLayers.Bounds(bbox[0], bbox[1], bbox[2], bbox[3]));
    $('testImage').innerHTML = '<a href=\'' + imageURL + '\'>link to test image</a>';
    setGEarthURL();
    setPermalinkURL();
}

// Gets the Z value set by the user
function getZValue()
{
    // If we have no depth information, assume we're at the surface.  This
    // will be ignored by the map server
    var zIndex = $('zValues').selectedIndex;
    var zValue = $('zValues').options.length == 0 ? 0 : $('zValues').options[zIndex].firstChild.nodeValue;
    return zPositive ? zValue : -zValue;
}

// Sets the permalink
function setPermalinkURL()
{
    if (layerName != '') {
        var url = window.location.protocol + '//' +
            window.location.host +
            window.location.pathname +
            '?dataset=' + layerName.split('/')[0] +
            '&variable=' + layerName.split('/')[1] +
            '&elevation=' + getZValue() +
            '&time=' + isoTValue +
            '&scale=' + scaleMinVal + ',' + scaleMaxVal +
            '&bbox=' + map.getExtent().toBBOX();
        $('permalink').innerHTML = '<a target="_blank" href="' + url +
            '">Permalink</a>&nbsp;|&nbsp;<a href="mailto:?subject=Godiva2%20link&body='
            + escape(url) + '">email</a>';
        $('permalink').style.visibility = 'visible';
    }
}

// Sets the URL for "Open in Google Earth" and the permalink
function setGEarthURL()
{
    if (essc_wms != null) {
        // Get a URL for a WMS request that covers the current map extent
        var mapBounds = map.getExtent();
        var urlEls = essc_wms.getURL(mapBounds).split('&');
        var gEarthURL = urlEls[0];
        for (var i = 1; i < urlEls.length; i++) {
            if (urlEls[i].startsWith('FORMAT')) {
                // Make sure the FORMAT is set correctly
                gEarthURL += '&FORMAT=application/vnd.google-earth.kmz';
            } else if (urlEls[i].startsWith('TIME') && timeSeriesSelected()) {
                // If we can make an animation, do so
                gEarthURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
            } else if (urlEls[i].startsWith('BBOX')) {
                // Set the bounding box so that there are no transparent pixels around
                // the edge of the image: i.e. find the intersection of the layer BBOX
                // and the viewport BBOX
                gEarthURL += '&BBOX=' + getIntersectionBBOX();
            } else if (!urlEls[i].startsWith('OPACITY')) {
                // We remove the OPACITY argument as Google Earth allows opacity
                // to be controlled in the client
                gEarthURL += '&' + urlEls[i];
            }
        }
        if (timeSeriesSelected()) {
            $('googleEarth').innerHTML = '<a href=\'' + gEarthURL + '\'>Open animation in Google Earth</a>';
        } else {
            $('googleEarth').innerHTML = '<a href=\'' + gEarthURL + '\'>Open in Google Earth</a>';
        }
    }
}

// Returns a bounding box as a string in format "minlon,minlat,maxlon,maxlat"
// that represents the intersection of the currently-visible map layer's 
// bounding box and the viewport's bounding box.
function getIntersectionBBOX()
{
    var mapBounds = map.getExtent();
    var mapBboxEls = mapBounds.toBBOX().split(',');
    // bbox is the bounding box of the currently-visible layer
    var newBBOX = Math.max(parseFloat(mapBboxEls[0]), bbox[0]) + ',';
    newBBOX += Math.max(parseFloat(mapBboxEls[1]), bbox[1]) + ',';
    newBBOX += Math.min(parseFloat(mapBboxEls[2]), bbox[2]) + ',';
    newBBOX += Math.min(parseFloat(mapBboxEls[3]), bbox[3]);
    return newBBOX;
}

// Formats the given value to numSigFigs significant figures
// WARNING: Javascript 1.5 only!
function toNSigFigs(value, numSigFigs)
{
    if (!value.toPrecision) {
        // TODO: do this somewhere more useful
        alert("Your browser doesn't support Javascript 1.5");
        return value;
    } else {
        return value.toPrecision(numSigFigs);
    }
}

// Returns true if the user has selected a time series
function timeSeriesSelected()
{
    return $('firstFrame').innerHTML != '' && $('lastFrame').innerHTML != '';
}

// Takes a BBOX string of the form "minlon,minlat,maxlon,maxlat" and returns
// the corresponding OpenLayers.Bounds object
// TODO: error checking
function getBounds(bboxStr)
{
    var bboxEls = bboxStr.split(",");
    return new OpenLayers.Bounds(parseFloat(bboxEls[0]), parseFloat(bboxEls[1]),
        parseFloat(bboxEls[2]), parseFloat(bboxEls[3]));
}