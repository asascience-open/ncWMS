/**
 * This class represents a server, i.e. a provider of data and metadata to the
 * Godiva2 site.  Servers can be "plain" WMS servers or ncWMS servers.  ncWMS
 * servers are "enhanced" for scientific data (e.g. each displayable layer
 * represents a variable, which has units and a valid min-max range.  Neither
 * of these things can be represented in a standard WMS capabilities document.)
 */

/**
 * We use the Prototype library to define this class
 */
var Server = Class.create();
Server.prototype = {
    
    /**
     * Initializes the Server object.  This will cause a call to the server
     * to determine its type.
     * @param url URL to the server (TODO: to the Capabilities doc or the server root?)
     */
    initialize: function(url) {
        this.url = url;
        // For browser security reasons, we can only call the server that hosts
        // this script.  Therefore we need to call the hosting server, passing
        // the url of the data/metadata server as an argument.
        // var serverDetails = makeAjaxRequest();
        // TODO: need to get all of these from the server
        this.type = 'ncWMS';
        this.title = 'ESSC ncWMS server';
        this.version = 'foo'; // TODO
    },
    
    /**
     * Gets the skeleton hierarchy of layers that are offered by this server
     * @param callback the function to be called with the object that is returned
     * from the call to the server
     * @param filter Optional filter string to allow selective display of layers
     */
    getLayerHierarchy: function(callback, filter) {
        new Ajax.Request('wms', {
            method: 'get',
            parameters: {
                request: 'GetMetadata',
                item: 'layers',
                filter: filter
            },
            onSuccess: function(transport) {
                callback(transport.responseText.evalJSON());
            },
            onFailure: function() {
                alert('Error getting layer hierarchy');
            }
        });
    },
    
    /**
     * Gets the details for the given displayable layer
     * @param callback the function to be called with the object that is returned
     * from the call to the server
     * @param layerName The unique ID for the displayable layer
     * @param time The time that we're currently displaying on the web interface
     * (the server calculates the nearest point on the t axis to this time).
     */
    getLayerDetails: function(callback, layerName, time) {
        new Ajax.Request('wms', {
            method: 'get',
            parameters: {
                request: 'GetMetadata',
                item: 'layerDetails',
                layerName : layerName,
                time: time
            },
            onSuccess: function(transport) {
                callback(transport.responseText.evalJSON());
            },
            onFailure: function() {
                alert('Error getting layer details');
            }
        });
    },
    
    /**
     * Gets the timesteps for the given displayable layer and the given day
     * @param callback the function to be called with the object that is returned
     * from the call to the server (an array of times as strings)
     * @param layerName The unique ID for the displayable layer
     * @param day The day for which we will request the timesteps, in "yyyy-mm-dd"
     * format
     */
    getTimesteps: function(callback, layerName, day) {
        new Ajax.Request('wms', {
            method: 'get',
            parameters: {
                request: 'GetMetadata',
                item: 'timesteps',
                layerName: layerName,
                day: day
            },
            onSuccess: function(transport) {
                callback(transport.responseText.evalJSON().timesteps);
            },
            onFailure: function() {
                alert('Error getting timesteps');
            }
        });
    },
    
    /**
     * Gets the min and max values of the layer for the given time, depth and
     * spatial extent (used by the auto-scale function).  ncWMS layers only.
     * @param callback the function to be called with the object that is returned
     * from the call to the server (simple object with properties "min" and "max")
     * @param layerName The unique ID for the displayable layer
     * @param bbox Bounding box *string* (e.g. "-180,-90,180,90")
     * @param elevation Elevation value
     * @param time Time value
     */
    getMinMax: function(callback, layerName, bbox, elevation, time) {
        new Ajax.Request('wms', {
            method: 'get',
            parameters: {
                request: 'GetMetadata',
                item: 'minmax',
                layers: layerName,
                bbox: bbox,
                elevation: elevation,
                time: time,
                crs: 'CRS:84', // TODO: should this be fixed like this?
                width: 50, // Request only a small box to save extracting lots of data
                height: 50
            },
            onSuccess: function(transport) {
                callback(transport.responseText.evalJSON());
            },
            onFailure: function() {
                alert('Error getting min and max values for the layer');
            }
        });
    }
    
 };