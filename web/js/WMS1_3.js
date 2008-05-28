/* Copyright (c) 2006 University of Reading, UK, published under the BSD license. */
// @require: OpenLayers/Layer/WMS.js
/**
* A WMS 1.3.0 layer, created by subclassing the WMS (1.1.1) layer type
* @class
*/
OpenLayers.Layer.WMS1_3 = OpenLayers.Class.create();
OpenLayers.Layer.WMS1_3.prototype = OpenLayers.Class.inherit( OpenLayers.Layer.WMS, {

    /** @final @type hash */
    DEFAULT_PARAMS: { service: "WMS",
                      version: "1.3.0",
                      request: "GetMap",
                      styles: "",
                      exceptions: "XML",  // todo, should be INIMAGE
                      format: "image/png",
                      crs: "CRS:84"
                     },
                     
    projection: 'none',
    
    /*
     * Overrides function in superclass, converting bounds in lat-lon to
     * equivalents in polar stereographic.
     */
    /*getURL: function(bounds) {
        bounds = this.adjustBounds(bounds);
        // Dirty hack to make polar stereographic work properly
        if (this.params.SRS == 'EPSG:32661' ||
            this.params.SRS == 'EPSG:32761') {
            bounds = this.lonLatToPolar(bounds);
        }
        var imageSize = this.getImageSize();
        var newParams = {
            'BBOX': this.encodeBBOX ?  bounds.toBBOX() : bounds.toArray(),
            'WIDTH': imageSize.w,
            'HEIGHT': imageSize.h
        };
        var requestString = this.getFullRequestString(newParams);
        return requestString;
    },*/
    
    /*
     * Converts a bounding-box in lon-lat coordinates to polar stereographic
     * such that the full extent of the earth in lat-lon corresponds with the
     * full extent of a stereographic projection.
     */
    lonLatToPolar: function(bounds) {
        var bbox = bounds.toArray();
        return new OpenLayers.Bounds(this.ll2p(bbox[0]), this.ll2p(bbox[1]),
            this.ll2p(bbox[2]), this.ll2p(bbox[3]));
    },
    
    /*
     * Linearly converts a number in the range [-180,180] to [-10700000, 14700000]
     */
    ll2p: function(x) {
        var y1 = -10700000;
        var y2 = 14700000;
        var x1 = -180;
        var x2 = 180;
        var m = (y2 - y1) / (x2 - x1);
        return m * (x - x1) + y1;
    },
                     
    /** @final @type String */
    CLASS_NAME: "OpenLayers.Layer.WMS1_3"
});
