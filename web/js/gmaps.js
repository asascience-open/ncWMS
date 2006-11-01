// Functionality of Google Maps widget

// The map widget itself
var map;
// A new map type for displaying ocean data on top of satellite images
var oceanMapType;
// The base URL for the map images from the oceanMapType
var baseURL;
// The opacity of the tiles (1.0 is fully opaque)
var opacity;
// The number of radians per degree
var radians_per_degree = Math.PI / 180;

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
        // Calculate the bounding box for this tile
        var tileSize = 256;
        var zoom_factor = Math.pow(2, zoom);
        var lon_degrees_per_tile = 360.0 / zoom_factor;
        var lon_degrees_per_pixel = lon_degrees_per_tile / tileSize;

        // Get the longitude of the left and right edges of the tile
        var minx = (point.x * lon_degrees_per_tile) - 180;
        var maxx = minx + lon_degrees_per_tile;

        // the y value of the tile whose top edge lies on the equator
        var y_eq = Math.pow(2, zoom - 1);
        // The distance in pixels of the top edge of the tile from the equator
        var top_tile_pixels_from_equator = tileSize * (point.y - y_eq);
       
        // Get the latitude of the bottom and top edges of the tile
        var miny = yToLat(top_tile_pixels_from_equator + tileSize, lon_degrees_per_pixel); 
        var maxy = yToLat(top_tile_pixels_from_equator, lon_degrees_per_pixel);
       
        return baseURL + '&BBOX=' + minx + ',' + miny + ',' + maxx + ',' + maxy;
        //return baseURL + '&BBOX=-90,0,0,80'; //'x=' + point.x + '&y=' + point.y + '&zoom=' + zoom;
    }
    tilelayers[1].isPng = function() { return true; }
    tilelayers[1].getOpacity = function() { 
        //GLog.write("Returning opacity = " + opacity);
        return opacity;
    }
    
    return new GMapType(tilelayers, G_SATELLITE_MAP.getProjection(), "Ocean data");
}

// Converts a y coordinate in pixels from the equator to a latitude in Mercator
// projection, in degrees
function yToLat(y, lon_degrees_per_pixel)
{
    var d = y * lon_degrees_per_pixel * radians_per_degree;
    var r = 2 * Math.atan(Math.exp(d)) - Math.PI / 2;
    return 0.0 - r / radians_per_degree;
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