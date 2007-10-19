// Use this file to define the data sources for the Godiva2 site.
// If this file is not included or blank then defaults will be used

// These are the servers that will provide layers to this site
var homeServer = ''; // The server hosting this Godiva2 site
var esscServer = 'http://lovejoy.nerc-essc.ac.uk:9080/ncWMS/wms'; // A remote server
var servers = [homeServer, esscServer];

// This object allows for the manual setup of the structure of the left-hand menu,
// so that layers can be added in an arbitrary hierarchy.  Remember a node with
// an id cannot also have children (The id indicates that this is a displayable
// layer and hence a leaf node).

// TODO: create complete catalogue from http://www.mersea.eu.org/html/information/catalog/products/catalog.html
// Or look at MIV v2??
var merseaMenu = [
    {
        "label" : "Global TEP",
        "children" : [
            {
                "label" : "Global 1/4 deg PSY3V2",
                "children" : [
                    {
                        "id" : "MERSEA_GLOBAL/temperature",
                        "label" : "Sea Water Potential Temperature",
                        "server" : esscServer
                    },
                    {
                        "id" : "MERSEA_GLOBAL/salinity",
                        "label" : "Sea Water Salinity",
                        "server" : esscServer
                    }
                ]
            }
        ]
    },
    {
        "label" : "North Atlantic TEP",
        "children" : [
            {
                "id" : "MERSEA_MED/temperature",
                "label" : "Sea Water Potential Temperature",
                "server": esscServer
            }
        ]
    },
    {
        "label" : "Mediterranean TEP",
        "children" : [
            {
                "id" : "MERSEA_MED/temperature",
                "label" : "Sea Water Potential Temperature",
                "server": esscServer
            }
        ]
    },
    {
        "label" : "Baltic TEP",
        "children" : [
            {
                "id" : "MERSEA_DMI_FORECAST/wtmp",
                "label" : "Sea Water Temperature",
                "server": esscServer
            },
            {
                "id" : "MERSEA_DMI_FORECAST/salt",
                "label" : "Sea Water Salinity",
                "server": esscServer
            }
        ]
    },
    {
        "label" : "In situ TEP",
        "children" : [
            {
                "label" : "Coriolis global daily in-situ observations",
                "children" : [
                    {
                        "id" : "MERSEA_CORIOLIS/temperature",
                        "label" : "Temperature",
                        "server": esscServer
                    }
                    // TODO: salinity
                ]
            }
        ]
    }
];