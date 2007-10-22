// This file defines the menu structure for the MERSEA DQV site.  This file will
// be loaded if someone loads up the godiva2 site with "filter=MERSEA".

// These are the servers that will provide layers to this site
var pmlServer = 'http://ncof.pml.ac.uk/ncWMS/wms';
var esscServer = 'http://lovejoy.nerc-essc.ac.uk:9080/ncWMS/wms';
var servers = [esscServer, pmlServer];

// This object allows for the manual setup of the structure of the left-hand menu,
// so that layers can be added in an arbitrary hierarchy.  Remember a node with
// an id cannot also have children (The id indicates that this is a displayable
// layer and hence a leaf node).
var ecoopMenu = [
    {
        "label" : "UK Met Office",
        "children" : [
            {
                "label" : "POLCOMS MRCS",
                "children" : [
                    {
                        "id" : "NCOF_MRCS/POT",
                        "label" : "Sea Water Potential Temperature",
                        "server" : esscServer
                    },
                    {
                        "id" : "NCOF_MRCS/SALTY",
                        "label" : "Sea Water Salinity",
                        "server" : esscServer
                    }
                ]
            }
        ]
    },
    {
        "label" : "Plymouth Marine Laboratory",
        "children" : [
            {
                "label" : "Ecosystem Variables",
                "children" : [
                    {
                        "id" : "ECOVARSALL/po4",
                        "label" : "Phosphate Concentration",
                        "server" : pmlServer
                    }
                ]
            }
        ]
    },
    {
        "label" : "University of Cyprus",
        "children" : [
            {
                "label" : "Eastern Mediterranean",
                "children" : [
                    {
                        "id" : "ECOOP_CYCO/temp",
                        "label" : "Sea Water Potential Temperature",
                        "server" : esscServer
                    }
                ]
            }
        ]
    },
    {
        "label" : "Marine Institute, Ireland",
        "children" : [
            {
                "label" : "NE Atlantic ROMS",
                "children" : [
                    {
                        "id" : "ECOOP_ROMS_TEST/temp",
                        "label" : "Sea Water Temperature",
                        "server" : esscServer
                    }
                ]
            }
        ]
    }
];