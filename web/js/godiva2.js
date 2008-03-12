//
// Javascript for GODIVA2 page.
//

var map = null;
var zPositive = false; // Will be true if the selected z axis is positive
var calendar = null; // The calendar object
var datesWithData = null; // Will be populated with the dates on which we have data
                          // for the currently-selected variable
var isoTValue = null; // The currently-selected t value (ISO8601)
var isIE;
var scaleMinVal;
var scaleMaxVal;
var gotScaleRange = false;
var scaleLocked = false; // see toggleLockScale()
var autoLoad = new Object(); // Will contain data for auto-loading data from a permalink
var menu = ''; // The menu that is being displayed (e.g. "mersea", "ecoop")
var bbox = null; // The bounding box of the currently-displayed layer
var featureInfoUrl = null; // The last-called URL for getFeatureInfo (following a click on the map)

var layerSwitcher = null;
var ncwms = null; // Points to the currently-active layer that is coming from this ncWMS
                  // Will point to either ncwms_tiled or ncwms_untiled.
var ncwms_tiled = null; // We shall maintain two separate layers, one tiles (for scalar
var ncwms_untiled = null; // quantities) and one untiled (for vector quantities)

var animation_layer = null; // The layer that will be used to display animations

var servers = ['']; // URLs to the servers from which we will display layers
                    // An empty string means the server that is serving this page.
var activeLayer = null; // The currently-selected layer metadata

var tree = null; // The tree control in the left-hand panel

var paletteSelector = null; // Pop-up panel for selecting a new palette
var paletteName = null; // Name of the currently-selected palette

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
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'basic', format: 'image/png' }, { wrapDateLine: true} );
    var bluemarble_wms = new OpenLayers.Layer.WMS( "Blue Marble", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite' }, { wrapDateLine: true} );
    var osm_wms = new OpenLayers.Layer.WMS( "Openstreetmap", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'osm-map' }, { wrapDateLine: true} );
    var human_wms = new OpenLayers.Layer.WMS( "Human Footprint", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'hfoot' }, { wrapDateLine: true} );
    var demis_wms = new OpenLayers.Layer.WMS( "Demis WMS",
        "http://www2.Demis.nl/MapServer/Request.asp?WRAPDATELINE=TRUE", {layers:
        'Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports'},
        {wrapDateLine: true});

    // ESSI WMS (see Stefano Nativi's email to me, Feb 15th)
    /*var essi_wms = new OpenLayers.Layer.WMS.Untiled( "ESSI WMS", 
        "http://athena.pin.unifi.it:8080/ls/servlet/LayerService?",
        {layers: 'sst(time-lat-lon)-T0', transparent: 'true' } );
    essi_wms.setVisibility(false);*/
            
    // The SeaZone Web Map server
    var seazone_wms = new OpenLayers.Layer.WMS1_3("SeaZone bathymetry", "http://ws.cadcorp.com/seazone/wms.exe?",
        {layers: 'Bathymetry___Elevation.bds', transparent: 'true'});
    seazone_wms.setVisibility(false);
    
    map.addLayers([bluemarble_wms, demis_wms, ol_wms, osm_wms, human_wms/*, seazone_wms, essi_wms*/]);
    
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
    
    layerSwitcher = new OpenLayers.Control.LayerSwitcher()
    map.addControl(layerSwitcher);
    //map.addControl(new OpenLayers.Control.MousePosition({prefix: 'Lon: ', separator: ' Lat:'}));
    map.zoomTo(1);
    
    // Add a listener for changing the base map
    //map.events.register("changebaselayer", map, function() { alert(this.projection) });
    // Add a listener for GetFeatureInfo
    map.events.register('click', map, getFeatureInfo);
    
    // Set up the autoload object
    // Note that we must get the query string from the top-level frame
    // strip off the leading question mark
    populateAutoLoad(window.location);
    if (window.top.location != window.location) {
        // We're in an iframe so we must also use the query string from the top frame
        populateAutoLoad(window.top.location);
    }
    
    // Set up the left-hand menu
    setupTreeControl(menu);
    
    // Set up the palette selector pop-up
    paletteSelector = new YAHOO.widget.Panel("paletteSelector", { 
        width:"400px",
        constraintoviewport: true,
        fixedcenter: true,
        underlay:"shadow",
        close:true,
        visible:false,
        draggable:true,
        modal:true
    });
    //paletteSelector.setHeader('Click to choose a colour palette');
}

// Populates the autoLoad object from the given window location object
function populateAutoLoad(windowLocation)
{
    var queryString = windowLocation.search.split('?')[1];
    if (queryString != null) {
        var kvps = queryString.split('&');
        for (var i = 0; i < kvps.length; i++) {
            keyAndVal = kvps[i].split('=');
            if (keyAndVal.length > 1) {
                var key = keyAndVal[0].toLowerCase();
                if (key == 'layer') {
                    autoLoad.layer = keyAndVal[1];
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
                    // we load a specific menu instead of the default
                    menu = keyAndVal[1];
                }
            }
        }
    }
}

function setupTreeControl(menu)
{
    tree = new YAHOO.widget.TreeView('layerSelector');
    // Add an event callback that gets fired when a tree node is clicked
    tree.subscribe('labelClick', treeNodeClicked);
    
    // The servers can be specified using the global "servers" array above
    // but if not, we'll just use the default server
    if (typeof servers == 'undefined' || servers == null) {
        servers = [''];
    }

    // Add a root node in the tree for each server.  If the user has supplied
    // a "menu" option then this will be sent to all the servers.
    for (var i = 0; i < servers.length; i++) {
        var layerRootNode = new YAHOO.widget.TextNode(
            {label: "Loading ...", server: servers[i]},
            tree.getRoot(),
            servers.length == 1 // Only show expanded if this is the only server
        );
        layerRootNode.multiExpand = false;
        // The getMenu() function is asynchronous.  Once we have received
        // the result from the server we shall pass it to the makeLayerMenu() function.
        getMenu(layerRootNode, {
            menu: menu,
            callback : function(layerRootNode, layers) {
                layerRootNode.data.label = layers.label;
                layerRootNode.label = layers.label;
                // Add layers recursively.
                addNodes(layerRootNode, layers.children);
                tree.draw();
                
                // Now look to see if we are auto-loading a certain layer
                if (typeof autoLoad.layer != 'undefined') {
                    var node = tree.getNodeByProperty('id', autoLoad.layer);
                    if (node == null) {
                        alert("Layer " + autoLoad.layer + " not found");
                    } else {
                        if (node.parent != null) node.parent.expand();
                        treeNodeClicked(node); // act as if we have clicked this node
                    }
                }
            }
        });
    }
}

// Called when a node in the tree has been clicked
function treeNodeClicked(node)
{
    // We're only interested if this is a displayable layer, i.e. it has an id.
    if (typeof node.data.id != 'undefined') {
        // Update the breadcrumb trail
        var s = node.data.label;
        var theNode = node;
        while(theNode.parent != tree.getRoot()) {
            theNode = theNode.parent;
            s = theNode.data.label + ' &gt; ' + s;
        }
        $('layerPath').innerHTML = s;

        // See if we're auto-loading a certain time value
        if (typeof autoLoad.isoTValue != 'undefined') {
            isoTValue = autoLoad.isoTValue;
        }
        if (isoTValue == null ) {
            // Set to the present time if we don't already have a time selected
            isoTValue = new Date().print('%Y-%m-%dT%H:%M:%SZ');
        }

        // Get the details of this layer from the server, calling layerSelected()
        // when we have the result
        var layerDetails = getLayerDetails(node.data.server, {
            callback: layerSelected,
            layerName: node.data.id,
            time: isoTValue
        });
    }
}

// Recursive method to add nodes to the layer selector tree control
function addNodes(parentNode, layerArray)
{
    for (var i = 0; i < layerArray.length; i++) {
        var layer = layerArray[i];
        if (layer.server == null) {
            // If the layer does not specify a server explicitly, use the URL of
            // the server that provided this layer
            layer.server = parentNode.data.server;
        }
        // The treeview control uses the layer.label string for display
        var newNode = new YAHOO.widget.TextNode(
            {label: layer.label, id: layer.id, server: layer.server},
            parentNode,
            false
        );
        if (typeof layer.children != 'undefined') {
            newNode.multiExpand = false;
            addNodes(newNode, layer.children);
        }
    }
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
// TODO: how can we detect whether the click is off the map (i.e. |lat| > 90)?
function getFeatureInfo(e)
{
    // Check we haven't clicked off-map
    var lonLat = map.getLonLatFromPixel(e.xy);
    if (ncwms != null && Math.abs(lonLat.lat) <= 90)
    {
        // See if the click was within range of the currently-visible layer
        var layerBounds = new OpenLayers.Bounds(activeLayer.bbox[0],
            activeLayer.bbox[1], activeLayer.bbox[2], activeLayer.bbox[3]);
        if (layerBounds.contains(lonLat.lon, lonLat.lat)) {
            $('featureInfo').innerHTML = "Getting feature info...";
            var params = {
                REQUEST: "GetFeatureInfo",
                BBOX: map.getExtent().toBBOX(),
                I: e.xy.x,
                J: e.xy.y,
                INFO_FORMAT: 'text/xml',
                QUERY_LAYERS: ncwms.params.LAYERS,
                WIDTH: map.size.w,
                HEIGHT: map.size.h
            };
            if (activeLayer.server != '') {
                // This is the signal to the server to load the data from elsewhere
                params.url = activeLayer.server;
            }
            featureInfoUrl = ncwms.getFullRequestString(
                params,
                'wms' // We must always load from the home server
            );
            OpenLayers.loadURL(featureInfoUrl, '', this, gotFeatureInfo);
            Event.stop(e);
        }
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

// Called when the user clicks on the name of a displayable layer in the left-hand menu
// Gets the details (units, grid etc) of the given layer. 
function layerSelected(layerDetails)
{
    activeLayer = layerDetails;
    gotScaleRange = false;
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
    var zValue = typeof autoLoad.zValue == 'undefined'
        ? getZValue()
        : parseFloat(autoLoad.zValue);

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
                diff = Math.abs(parseFloat(zValues[j]) - zValue);
            } else {
                diff = Math.abs(parseFloat(zValues[j]) + zValue);
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
    $('scaleBar').style.visibility = scaleVisibility;
    $('scaleMin').style.visibility = scaleVisibility;
    $('scaleMax').style.visibility = scaleVisibility;
    $('autoScale').style.visibility = scaleLocked ? 'hidden' : scaleVisibility;
    $('lockScale').style.visibility = scaleVisibility;
    
    // Set the scale value if this is present in the metadata
    if (typeof layerDetails.scaleRange != 'undefined' &&
            layerDetails.scaleRange != null &&
            layerDetails.scaleRange.length > 1 &&
            layerDetails.scaleRange[0] != layerDetails.scaleRange[1] &&
            !scaleLocked) {
        scaleMinVal = layerDetails.scaleRange[0];
        scaleMaxVal = layerDetails.scaleRange[1];
        $('scaleMin').value = toNSigFigs(scaleMinVal, 4);
        $('scaleMax').value = toNSigFigs(scaleMaxVal, 4);
        gotScaleRange = true;
    }
    
    if (!isIE) {
        // Only show this control if we can use PNGs properly (i.e. not on Internet Explorer)
        $('opacityControl').style.visibility = 'visible';
    }

    // Set the auto-zoom box
    bbox = layerDetails.bbox;
    $('autoZoom').innerHTML = '<a href="#" onclick="map.zoomToExtent(new OpenLayers.Bounds(' +
        bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3] +
        '));\">Fit layer to window</a>';
    
    // Set up the copyright statement
    $('copyright').innerHTML = layerDetails.copyright;

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
    getTimesteps(activeLayer.server, {
        callback: updateTimesteps,
        layerName: activeLayer.id,
        day: makeIsoDate(calendar.date)
    });
}

// Gets an ISO Date ("yyyy-mm-dd") for the given Javascript date object.
// Does not contain the time.
function makeIsoDate(date)
{
    // Watch out for low-numbered years when generating the ISO string
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

    if (typeof autoLoad.scaleMin != 'undefined' && typeof autoLoad.scaleMax != 'undefined') {
        $('scaleMin').value = autoLoad.scaleMin;
        $('scaleMax').value = autoLoad.scaleMax;
        validateScale(); // this calls updateMap()
    } else if (!gotScaleRange && !scaleLocked) {// We didn't get a scale range from the layerDetails
        autoScale(true);
    } else {
        updateMap(); // Update the map without changing the scale
    }
}

// Calls the WMS to find the min and max data values, then rescales.
// If this is a newly-selected variable the method gets the min and max values
// for the whole layer.  If not, this gets the min and max values for the viewport.
function autoScale(newVariable)
{
    var dataBounds;
    if ($('tValues')) {
        isoTValue = $('tValues').value;
    }
    if (newVariable) {
        // We use the bounding box of the whole layer 
        dataBounds = bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3];
    } else {
        // Use the intersection of the viewport and the layer's bounding box
        dataBounds = getIntersectionBBOX();
    }
    getMinMax(activeLayer.server, {
        callback: gotMinMax,
        layerName: activeLayer.id,
        bbox: dataBounds,
        elevation: getZValue(),
        time: isoTValue
    });
}

// When the scale is locked, the user cannot change the colour scale either
// by editing manually or clicking "auto".  Furthermore the scale will not change
// when a new layer is loaded
function toggleLockScale()
{
    if (scaleLocked) {
        // We need to unlock the scale
        scaleLocked = false;
        // TODO: not very neat!
        $('lockScale').innerHTML = '<a href="#" onclick="javascript:toggleLockScale()">lock</a>';
        $('autoScale').style.visibility = 'visible';
        $('scaleMin').disabled = false;
        $('scaleMax').disabled = false;
    } else {
        // We need to lock the scale
        scaleLocked = true;
        // TODO: not very neat!
        $('lockScale').innerHTML = '<a href="#" onclick="javascript:toggleLockScale()">unlock</a>';
        $('autoScale').style.visibility = 'hidden';
        $('scaleMin').disabled = true;
        $('scaleMax').disabled = true;
    }
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
    if (isNaN(fMin)) {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMin').value = scaleMinVal;
    } else if (isNaN(fMax)) {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMax').value = scaleMaxVal;
    } else if (fMin > fMax) {
        alert('Minimum scale value must be less than the maximum');
        // Reset to the old values
        $('scaleMin').value = scaleMinVal;
        $('scaleMax').value = scaleMaxVal;
    } else {
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
    var urlEls = ncwms.getURL(getMapExtent()).split('&');
    // Replace the parameters as needed.  We generate a map that is half the
    // width and height of the viewport, otherwise it takes too long
    var width = $('map').clientWidth;// / 2;
    var height = $('map').clientHeight;// / 2;
    var newURL = urlEls[0];
    for (var i = 1; i < urlEls.length; i++) {
        if (urlEls[i].startsWith('TIME=')) {
            newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
        } else if (urlEls[i].startsWith('FORMAT')) {
            newURL += '&FORMAT=image/gif';
        } else if (urlEls[i].startsWith('WIDTH')) {
            newURL += '&WIDTH=' + width;
        } else if (urlEls[i].startsWith('HEIGHT')) {
            newURL += '&HEIGHT=' + height;
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
    
    // When the mapOverlay has been loaded we call animationLoaded() and place the image correctly
    // on the map
    $('mapOverlay').src = newURL;
    $('mapOverlay').width = width;
    $('mapOverlay').height = height;
}
// Gets the current map extent, checking for out-of-range latitude values
function getMapExtent()
{
    var bounds = map.getExtent();
    // This assumes a lat-lon projection!
    if (bounds.top > 90.0) bounds.top = 90.0;
    if (bounds.bottom < -90.0) bounds.bottom = -90.0;
    return bounds;
}
function animationLoaded()
{
    $('loadingAnimationDiv').style.visibility = 'hidden';
    //$('mapOverlayDiv').style.visibility = 'visible';
    // Load the image into a new layer on the map
    animation_layer = new OpenLayers.Layer.Image(
        "ncWMS", // Name for the layer
        $('mapOverlay').src, // URL to the image
        getMapExtent(), // Image bounds
        new OpenLayers.Size($('mapOverlay').width, $('mapOverlay').height), // Size of image
        {isBaseLayer : false} // Other options
    );
    setVisibleLayer(true);
    map.addLayers([animation_layer]);
}
function hideAnimation()
{
    setVisibleLayer(false);
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
    if (typeof autoLoad.bbox != 'undefined') {
        map.zoomToExtent(getBounds(autoLoad.bbox));
    }
    
    // Make sure the autoLoad object is cleared
    autoLoad = new Object();
    
    // Get the default style for this layer.  There is some defensive programming here to 
    // take old servers into account that don't advertise the supported styles
    var style = typeof activeLayer.supportedStyles == 'undefined' ? 'boxfill' : activeLayer.supportedStyles[0];
    if (paletteName != null) {
        style += '/' + paletteName;
    }

    // Notify the OpenLayers widget
    // TODO get the map projection from the base layer
    // TODO use a more informative title
    // Buffer is set to 1 to avoid loading a large halo of tiles outside the
    // current viewport
    var params = {
        layers: activeLayer.id,
        elevation: getZValue(),
        time: isoTValue,
        transparent: 'true',
        styles: style,
        colorscalerange: scaleMinVal + ',' + scaleMaxVal,
        opacity: opacity,
        numcolorbands: $('numColorBands').value
    };
    if (ncwms == null) {
        ncwms_tiled = new OpenLayers.Layer.WMS1_3("ncWMS",
            activeLayer.server == '' ? 'wms' : activeLayer.server, 
            params,
            {buffer: 1, wrapDateLine: true}
        );
        ncwms_untiled = new OpenLayers.Layer.WMS1_3("ncWMS",
            activeLayer.server == '' ? 'wms' : activeLayer.server, 
            params,
            {buffer: 1, ratio: 1.5, singleTile: true, wrapDateLine: true}
        );
        setVisibleLayer(false);
        map.addLayers([ncwms_tiled, ncwms_untiled]);
        // Create a layer for coastlines
        // TOOD: only works at low res (zoomed out)
        //var coastline_wms = new OpenLayers.Layer.WMS( "Coastlines", 
        //    "http://labs.metacarta.com/wms/vmap0?", {layers: 'coastline_01', transparent: 'true' } );
        //map.addLayers([ncwms, coastline_wms]);
        //map.addLayers([ncwms_tiled, ncwms_untiled]);
    } else {
        setVisibleLayer(false);
        ncwms.url = activeLayer.server == '' ? 'wms' : activeLayer.server;
        ncwms.mergeNewParams(params);
    }
    
    $('featureInfo').innerHTML = "Click on the map to get more information";
    $('featureInfo').style.visibility = 'visible';
    
    var imageURL = ncwms.getURL(new OpenLayers.Bounds(bbox[0], bbox[1], bbox[2], bbox[3]));
    $('testImage').innerHTML = '<a target="_blank" href="' + imageURL + '">link to test image</a>';
    setGEarthURL();
    setPermalinkURL();
}

// Shows a pop-up window with the available palettes for the user to select
// This is called when the user clicks the colour scale bar
function showPaletteSelector()
{
    updatePaletteSelector();
    paletteSelector.render(document.body);
    paletteSelector.show();
}

// Updates the contents of the palette selection table
function updatePaletteSelector()
{
    // Populate the palette selector dialog box
    // TODO: revert to default palette if layer doesn't support this one
    var palettes = activeLayer.palettes;
    if (palettes == null || palettes.length == 0) {
        $('paletteDiv').innerHTML = 'There are no alternative palettes for this layer';
        return;
    }
    
    // TODO test if coming from a different server
    var width = 50;
    var height = 200;
    var paletteUrl = activeLayer.server + 'wms?REQUEST=GetLegendGraphic' +
        '&LAYER=' + activeLayer.id +
        '&COLORBARONLY=true' +
        '&WIDTH=1' +
        '&HEIGHT=' + height +
        '&NUMCOLORBANDS=' + $('numColorBands').value;
    var palStr = '<div style="overflow: auto">'; // ensures scroll bars appear if necessary
    palStr += '<table border="1"><tr>';
    for (var i = 0; i < palettes.length; i++) {
        palStr += '<td><img src="' + paletteUrl + '&PALETTE=' + palettes[i] +
            '" width="' + width + '" height="' + height + '" title="' + palettes[i] +
            '" onclick="paletteSelected(\'' + palettes[i] + '\')"' +
            '/></td>';
    }
    palStr += '</tr></table></div>';
    $('paletteDiv').innerHTML = palStr;
}

// Called when the user selects a new palette in the palette selector
function paletteSelected(thePalette)
{
    paletteName = thePalette;
    paletteSelector.hide();
    // Change the colour scale bar on the main page
    $('scaleBar').src = 'wms?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=398'
        + '&PALETTE=' + thePalette + '&NUMCOLORBANDS=' + $('numColorBands').value;
    updateMap();
}

// Decides whether to display the animation, or the tiled or untiled
// version of the ncwms layer
function setVisibleLayer(animation)
{
    // TODO: repeats code above
    var style = typeof activeLayer.supportedStyles == 'undefined' ? 'boxfill' : activeLayer.supportedStyles[0];
    if (animation) {
        setLayerVisibility(animation_layer, true);
        setLayerVisibility(ncwms_tiled, false);
        setLayerVisibility(ncwms_untiled, false);
    } else if (style.toLowerCase() == 'vector') {
        setLayerVisibility(animation_layer, false);
        setLayerVisibility(ncwms_tiled, false);
        setLayerVisibility(ncwms_untiled, true);
        ncwms = ncwms_untiled;
    } else {
        setLayerVisibility(animation_layer, false);
        setLayerVisibility(ncwms_tiled, true);
        setLayerVisibility(ncwms_untiled, false);
        ncwms = ncwms_tiled;
    }
    layerSwitcher.layerStates = []; // forces redraw
    layerSwitcher.redraw();
}

function setLayerVisibility(layer, visible)
{
    if (layer != null) {
        layer.setVisibility(visible);
        layer.displayInLayerSwitcher = visible;
    }
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

// Sets the permalink, i.e. the link back to this view of the page
function setPermalinkURL()
{
    if (activeLayer != null) {
        // Note that we must use window.top to get the containing page, in case
        // the Godiva2 page is embedded in an iframe
        // Watch out for trailing hashes, which screw the permalink up
        var url = window.top.location.toString().replace('#', '');
        url +=
            '?menu=' + menu +
            '&layer=' + activeLayer.id +
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

// Sets the URL for "Open in Google Earth"
function setGEarthURL()
{
    if (ncwms != null) {
        // Get a URL for a WMS request that covers the current map extent
        var mapBounds = map.getExtent();
        var urlEls = ncwms.getURL(mapBounds).split('&');
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
            } else if (urlEls[i].startsWith('WIDTH')) {
                gEarthURL += '&WIDTH=' + map.size.w;
            } else if (urlEls[i].startsWith('HEIGHT')) {
                gEarthURL += '&HEIGHT=' + map.size.h;
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