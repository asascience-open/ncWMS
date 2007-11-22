<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the MERSEA site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=MERSEA"
     TODO: create complete catalogue from http://www.mersea.eu.org/html/information/catalog/products/catalog.html
     Or look at MIV v2??--%>
<c:set var="esscServer" value="http://lovejoy.nerc-essc.ac.uk:8080/ncWMS/wms"/>
{
    layers : [
    {
        "label" : "Global TEP",
        "children" : [
            {
                "label" : "Global 1/4 deg PSY3V2",
                "children" : [
                    {
                        "id" : "MERSEA_GLOBAL/surface_downward_stress",
                        "label" : "Surface Downward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/taux",
                        "label" : "Surface Downward Eastward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/mlp",
                        "label" : "Ocean Mixed Layer Thickness",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/qtot",
                        "label" : "Surface Downward Heat Flux In Air",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/tauy",
                        "label" : "Surface Downward Northward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/v",
                        "label" : "Northward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/hice",
                        "label" : "Sea Ice Thickness",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/vice",
                        "label" : "Sea Ice Y Velocity",
                        "server" : "${esscServer}"
                    },
                   {
                        "id" : "MERSEA_GLOBAL/uice",
                        "label" : "Sea Ice X Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/salinity",
                        "label" : "Sea Water Salinity",
                        "server" : "${esscServer}"
                    },
                     {
                        "id" : "MERSEA_GLOBAL/temperature",
                        "label" : "Sea Water Potential Temperature",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/sea_water_velocity",
                        "label" : "Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/emp",
                        "label" : "Water Flux Into Ocean",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/u",
                        "label" : "Eastward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/ssh",
                        "label" : "Sea Surface Elevation",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/kz",
                        "label" : "Vertical Eddy Diffucivity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/qsr",
                        "label" : "Surface Downward Solar Heat Flux",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/sea_ice_velocity",
                        "label" : "Sea Ice Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_GLOBAL/fice",
                        "label" : "Sea Ice Area Fraction",
                        "server" : "${esscServer}"
                    }
                ]
            }
        ]
    },
    {
        "label" : "North Atlantic TEP",
        "children" : [
 	     {
                    "id" : "MERSEA_ESSC_NATL/surface_downward_stress",
                        "label" : "Surface Downward Stress",
                        "server" : "${esscServer}"
            },
                    {
                        "id" : "MERSEA_ESSC_NATL/taux",
                        "label" : "Surface Downward Eastward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/mlp",
                        "label" : "Ocean Mixed Layer Thickness",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/qtot",
                        "label" : "Surface Downward Heat Flux In Air",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/tauy",
                        "label" : "Surface Downward Northward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/v",
                        "label" : "Northward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/salinity",
                        "label" : "Sea Water Salinity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/temperature",
                        "label" : "Sea Water Potential Temperature",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/sea_water_velocity",
                        "label" : "Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/u",
                        "label" : "Eastward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/emp",
                        "label" : "Water Flux Into Ocean",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/ssh",
                        "label" : "Sea Surface Elevation",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/bsfd",
                        "label" : "Ocean Barotropic Streamfunction",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_ESSC_NATL/mld",
                        "label" : "Ocean Mixed Layer Thicknes",
                        "server" : "${esscServer}"
                    }

        ]
    },
    {
        "label" : "Mediterranean TEP",
        "children" : [
            {
                "id" : "MERSEA_MED/salinity",
                "label" : "Sea Water Salinity",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_MED/temperature",
                "label" : "Sea Water Potential Temperature",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_MED/ssh",
                "label" : "Sea Surface Height Above Sea Level",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_MED/mld",
                "label" : "Ocean Mixed Layer Thickness",
                "server": "${esscServer}"
            }
        ]
    },
    {
        "label" : "Baltic TEP",
        "children" : [
            {
                "id" : "MERSEA_DMI_FORECAST/ithk",
                "label" : "Sea Ice Thickness",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/vvel",
                "label" : "Northward Sea Water Velocity",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/wvel",
                "label" : "Upward Sea Water Velocity",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/salt",
                "label" : "Sea Water Salinity",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/sea_water_velocity",
                "label" : "Sea Water Velocity",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/wtmp",
                "label" : "Sea Water Temperature",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/icov",
                "label" : "Sea Ice Area Fraction",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/elev",
                "label" : "Sea Surface Height Above Sea Level",
                "server": "${esscServer}"
            },
            {
                "id" : "MERSEA_DMI_FORECAST/uvel",
                "label" : "Eastward Sea Water Velocity",
                "server": "${esscServer}"
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
                        "server": "${esscServer}"
                    },
                    {
                        "id" : "MERSEA_CORIOLIS/pct_variance",
                        "label" : "Estimate variance / a priori variance (percent)",
                        "server": "${esscServer}"
                    }
                ]
            }
        ]
    }
]
}