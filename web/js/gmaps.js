// Functionality of Google Maps widget

// The map widget itself
var map;
// A new map type for displaying ocean data on top of satellite images
var oceanMapType;
// The base URL for the map images from the oceanMapType
var baseURL;
// The opacity of the tiles (1.0 is fully opaque)
var opacity;

// Returns a GMapType for displaying ocean data.  Displays ocean data on top
// of satellite images.  Any number (apparently) of tile layers can be displayed.
// Many thanks to Mike Williams http://www.econym.demon.co.uk/googlemaps/custommap.htm
function getOceanMapType()
{
    // Create the GTileLayer, supports zoom levels 7 to 14
    var tilelayers = [
        G_SATELLITE_MAP.getTileLayers()[0],
        new GTileLayer(new GCopyrightCollection("none"), 7, 14)
    ];
    tilelayers[1].getTileUrl = function(point, zoom) {
        return baseURL + 'x=' + point.x + '&y=' + point.y + '&zoom=' + zoom;
    }
    tilelayers[1].isPng = function() { return true; }
    tilelayers[1].getOpacity = function() { 
        //GLog.write("Returning opacity = " + opacity);
        return opacity;
    }
    
    return new GMapType(tilelayers, G_SATELLITE_MAP.getProjection(), "Ocean data");
}

// Sets up the map in the given document element, with the given server URL
function setup_map(mapEl) {
    
    // When we first start, we just use transparent PNGs until the user selects
    // a dataset.  See index.jsp for definition of serverURL
    baseURL = serverURL + 'images/blank.png?';
    opacity = 1.0;
    
    if (GBrowserIsCompatible())
    {
        // Create a new map object
        map = new GMap2(mapEl);
        // Add a map onto which we'll project ocean data
        oceanMapType = getOceanMapType()
        map.addMapType(oceanMapType);
        // Add the navigation and zoom controls
        map.addControl(new GSmallMapControl());
        // Add Map Type buttons in the upper right corner
        map.addControl(new GMapTypeControl());
        // Add a scale bar
        map.addControl(new GScaleControl());
        // Add an overview map
        //map.addControl(new GOverviewMapControl());
        // Make sure we're showing the ocean data by default
        map.setCenter(new GLatLng(0, 0), 1, oceanMapType);
        
        // When we first start, we just use transparent PNGs until the user selects
        // a dataset.  See index.jsp for definition of serverURL
        baseURL = serverURL + 'images/blank.png?';
    }
    else
    {
        alert("Your browser is not supported by Google Maps");
    }
    
}

// Called when the user does something (e.g. changes the variable or colour scale)
// that causes the map to be redrawn
function set_tile_url(url)
{
    baseURL = url;
    //$('pleaseWait').style.visibility = 'visible';
    // Force refresh of map images
    map.setMapType(oceanMapType);
    //$('pleaseWait').style.visibility = 'hidden';
}

// Changes the opacity of the ocean data overlays
function changeOpacity(value)
{
    opacity = parseFloat(value) / 100.0;
    // Force refresh of map images
    // For some reason, simply calling setMapType(oceanMapType) doesn't force
    // the opacity to be changed.  We need to switch to a different map type
    // first, then switch back again.
    map.setMapType(G_HYBRID_MAP);
    map.setMapType(oceanMapType);
}