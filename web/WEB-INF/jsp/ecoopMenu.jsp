<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the ECOOP site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=ECOOP" --%>
<c:set var="pmlServer" value="http://ncof.pml.ac.uk/ncWMS/wms"/>
<c:set var="esscServer" value="http://lovejoy.nerc-essc.ac.uk:8080/ncWMS/wms"/>
{
layers: [
    {
        "label" : "UK Met Office",
        "children" : [
            {
                "label" : "POLCOMS MRCS",
                "children" : [
                    {
                        "id" : "NCOF_MRCS/POT",
                        "label" : "Sea Water Potential Temperature",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "NCOF_MRCS/SALTY",
                        "label" : "Sea Water Salinity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "NCOF_MRCS/sea_water_velocity",
                        "label" : "Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "NCOF_MRCS/VOGRD",
                        "label" : "Northward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "NCOF_MRCS/UOGRD",
                        "label" : "Eastward Sea Water Velocity",
                        "server" : "${esscServer}"
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
                        "server" : "${pmlServer}"
                    },
                    {
                        "id" : "ECOVARSALL/si",
                        "label" : "Silicate Concentration",
                        "server" : "${pmlServer}"
                    },
                    {
                        "id" : "ECOVARSALL/no3",
                        "label" : "Nitrate Concentration",
                        "server" : "${pmlServer}"
                    },
                    {
                        "id" : "ECOVARSALL/o2o",
                        "label" : "Dissolved Oxygen Concentration",
                        "server" : "${pmlServer}"
                    },
                    {
                        "id" : "ECOVARSALL/p3c",
                        "label" : "Picoplankton biomass",
                        "server" : "${pmlServer}"
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
                        "id" : "ECOOP_CYCO/HF",
                        "label" : "Surface Downward Heat Flux In Air",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/surface_downward_stress",
                        "label" : "Surface Downward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/h",
                        "label" : "Sea Floor Depth Below Sea Level",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/eta",
                        "label" : "Sea Surface Height",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/EMP",
                        "label" : "Water Flux Into Ocean",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/Ty",
                        "label" : "Surface Downward Northward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/temp",
                        "label" : "Sea Water Potential Temperature",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/v",
                        "label" : "Northward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/Tx",
                        "label" : "Surface Downward Eastward Stress",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/sea_water_velocity",
                        "label" : "Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/SW",
                        "label" : "Surface Net Downward Shortwave Flux",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/u",
                        "label" : "Eastward Sea Water Velocity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_CYCO/sal",
                        "label" : "Sea Water Salinity",
                        "server" : "${esscServer}"
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
                        "id" : "ECOOP_ROMS_TEST/sensible",
                        "label" : "Net Sensible Heat Flux",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_ROMS_TEST/salt",
                        "label" : "Salinity",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_ROMS_TEST/lwrad",
                        "label" : "Net Longwave Radiation Flux",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_ROMS_TEST/temp",
                        "label" : "Potential Temperature",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_ROMS_TEST/evaporation",
                        "label" : "Evaporation Rate",
                        "server" : "${esscServer}"
                    },
                    {
                        "id" : "ECOOP_ROMS_TEST/latent",
                        "label" : "Net Latent Heat Flux",
                        "server" : "${esscServer}"
                    }
                ]
            }
        ]
    }
]
}
