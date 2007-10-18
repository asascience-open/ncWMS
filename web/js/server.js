/**
 * This class represents a server, i.e. a provider of data and metadata to the
 * Godiva2 site.  Servers can be "plain" WMS servers or ncWMS servers.  ncWMS
 * servers are "enhanced" for scientific data (e.g. each displayable layer
 * represents a variable, which has units and a valid min-max range.  Neither
 * of these things can be represented in a standard WMS capabilities document.)
 */

// Converts ISO8601 string to Date object
// From http://delete.me.uk/2005/03/iso8601.html, copied 16th October 2007
function iso8601ToDate(string)
{
    var regexp = "([0-9]{4})(-([0-9]{2})(-([0-9]{2})" +
        "(T([0-9]{2}):([0-9]{2})(:([0-9]{2})(\.([0-9]+))?)?" +
        "(Z|(([-+])([0-9]{2}):([0-9]{2})))?)?)?)?";
    var d = string.match(new RegExp(regexp));

    var offset = 0;
    var date = new Date(d[1], 0, 1);

    if (d[3]) { date.setMonth(d[3] - 1); }
    if (d[5]) { date.setDate(d[5]); }
    if (d[7]) { date.setHours(d[7]); }
    if (d[8]) { date.setMinutes(d[8]); }
    if (d[10]) { date.setSeconds(d[10]); }
    if (d[12]) { date.setMilliseconds(Number("0." + d[12]) * 1000); }
    if (d[14]) {
        offset = (Number(d[16]) * 60) + Number(d[17]);
        offset *= ((d[15] == '-') ? 1 : -1);
    }

    offset -= date.getTimezoneOffset();
    var time = (Number(date) + (offset * 60 * 1000));
    var ret = new Date();
    ret.setTime(Number(time));
    return ret;
}

// TODO: write a function to get the list of layer providers from the server.
// This will include the "host" ncWMS server as well as any cascaded ncWMS or WMS
// providers that are known to the server.


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
        //this.title = 'ESSC ncWMS server';
        //this.version = 'foo'; // TODO
    },
    
    /**
     * Private function for making an Ajax request to the server.  This will
     * simply throw an alert if the XMLHttpRequest call fails.
     * @param Object containing parameters, which must include:
     *        urlparams Object containing the URL parameters
     *        onSuccess Callback to be called with the JSON object that is
     *                  returned from the server
     *        onServerError Callback to be called when the server reports an error
     *                  (if omitted, a default is provided)
     */
    makeAjaxRequest: function(params) {
        if (typeof params.onServerError == 'undefined') {
            params.onServerError = function(exception) {
                alert("Server exception class: " + exception.class + 
                    ", Message: " + exception.message);
            }
        }
        // Add the common elements to the URL parameters
        if (this.url != null && this.url != '') {
            params.urlparams.url = this.url;
        }
        params.urlparams.request = 'GetMetadata';
        
        new Ajax.Request('wms', {
            method: 'get',
            parameters: params.urlparams,
            onSuccess: function(transport) {
                try {
                    var myobj = transport.responseText.evalJSON();
                } catch(err) {
                    alert("Invalid JSON returned from server");
                    return;
                }
                if (typeof myobj.exception == 'undefined') {
                    params.onSuccess(myobj);
                } else {
                    params.onServerError(myobj.exception);
                }
            },
            onFailure: function() {
                alert('Error getting data from server'); // TODO: get the full URL somehow?
            }
        });
    },
    
    /**
     * Gets the skeleton hierarchy of layers that are offered by this server
     * @param params Object containing a callback that will be called when the result
     * is returned from the server and an optional filter
     * @param filter Optional filter string to allow selective display of layers
     */
    getLayerHierarchy: function(params) {
        this.makeAjaxRequest({
            urlparams: {
                item: 'layers',
                filter: typeof params.filter == 'undefined' ? '' : params.filter
            },
            onSuccess: function(layerHierarchy) {
                alert('this.url=' + this.url);
                params.callback(layerHierarchy, this);
            }
        });
    },
    
    /**
     * Gets the details for the given displayable layer
     * @param Object containing parameters, which must include:
     *        callback the function to be called with the object that is returned
     *            from the call to the server
     *        layerName The unique ID for the displayable layer
     *        time The time that we're currently displaying on the web interface
     *            (the server calculates the nearest point on the t axis to this time).
     */
    getLayerDetails: function(params) {
        this.makeAjaxRequest({
            urlparams: {
                item: 'layerDetails',
                layerName : params.layerName,
                time: params.time
            },
            onSuccess: function(layerDetails) {
                // Convert the nearest-time ISO string to a Javascript date object
                if (typeof layerDetails.nearestTimeIso != 'undefined') {
                    layerDetails.nearestTime = iso8601ToDate(layerDetails.nearestTimeIso);
                }
                params.callback(layerDetails);
            }
        });
    },
    
    /**
     * Gets the timesteps for the given displayable layer and the given day
     * @param Object containing parameters, which must include:
     *        callback the function to be called with the object that is returned
     *            from the call to the server (an array of times as strings)
     *        layerName The unique ID for the displayable layer
     *        day The day for which we will request the timesteps, in "yyyy-mm-dd"
     *            format
     */
    getTimesteps: function(params) {
        this.makeAjaxRequest({
            urlparams: {
                item: 'timesteps',
                layerName: params.layerName,
                day: params.day
            },
            onSuccess: function(timesteps) {
                params.callback(timesteps.timesteps);
            }
        });
    },
    
    /**
     * Gets the min and max values of the layer for the given time, depth and
     * spatial extent (used by the auto-scale function).  ncWMS layers only.
     * @param Object containing parameters, which must include:
     *        callback the function to be called with the object that is returned
     *            from the call to the server (simple object with properties "min" and "max")
     *        layerName The unique ID for the displayable layer
     *        bbox Bounding box *string* (e.g. "-180,-90,180,90")
     *        elevation Elevation value
     *        time Time value
     */
    getMinMax: function(params) {
        this.makeAjaxRequest({
            urlparams: {
                item: 'minmax',
                layers: params.layerName,
                bbox: params.bbox,
                elevation: params.elevation,
                time: params.time,
                crs: 'CRS:84', // TODO: should this be fixed like this?
                width: 50, // Request only a small box to save extracting lots of data
                height: 50
            },
            onSuccess: params.callback
        });
    }
    
 };