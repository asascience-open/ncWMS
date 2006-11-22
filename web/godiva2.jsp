<%@page contentType="text/html"%><%@page pageEncoding="UTF-8"%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
   java.net.URL reqURL = new java.net.URL(request.getRequestURL().toString());
   String serverURL = "http://" + reqURL.getHost() + ":" + reqURL.getPort() + 
       request.getContextPath() + "/";
%>
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>GODIVA2 Data Access and Analysis demo page</title>
        <link rel="stylesheet" type="text/css" href="css/godiva2.css">
        <script type="text/javascript" src="js/OpenLayers.js"></script>
        <script type="text/javascript" src="js/WMS1_3.js"></script>
        <script type="text/javascript" src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA7YuB_Hd5LuBiQ3-he19uDxTFRfqDGOwfXAlOK-54sJyR4NNS5RSdkyh_Ih5CfURmd5umFAKNKx8oJg"></script>
        <script type="text/javascript">
            var serverURL = '<%=serverURL%>';
        </script>
        <script type="text/javascript" src="js/godiva2.js"></script>
    </head>
    
    <body>

    <!-- This is the left menu bar that will contain the list of datasets and variables -->
    <div id="accordionDiv" class="leftmenu">
    </div>

    <div id="help" class="help">
    <p><a href="info.html">More info</a></p>
    </div>
    
    <a href="http://www.resc.rdg.ac.uk/"><img id="resclogo" src="http://www.resc.rdg.ac.uk/images/new_logo_72dpi_web.png" alt="ReSC logo"></a>
    <a href="http://www.jcomm-services.org/"><img id="jcommlogo" width="187" src="images/jcomm_logo.png" alt="JCOMM logo"></a>
    
    <div id="mainPanel" class="mainPanel">
        <div class="panelHeader">
            <b>Dataset:</b> <span id="datasetName">Please select from the left panel</span><br />
            <b>Variable:</b> <span id="variableName">Please select from the left panel</span><br />
            <span id="units"></span><br />
            <span id="zAxis"></span><select id="zValues" onchange="javascript:updateMap()"><option value="0">dummy</option></select><br />
            <span id="date"></span>&nbsp;<span id="time"></span> <span id="utc">UTC</span><br />
            <br />
            Powered by <a href="http://www.openlayers.org">OpenLayers</a> and <a href="http://www.opengeospatial.org">OGC</a> standards<br />
            <!--<select id="otherGEarthDatasets" onchange="javascript:if(this.value != '') { window.open(this.value) }">
                <option value="" selected>Other useful datasets...</option>
                <option value="http://w3.jcommops.org/cgi-bin/WebObjects/Argo.woa/482/wo/Ej1NgzFtN3S024S2meG1733WzA9/0.0.56.9.2.1">ARGO float locations</option>
                <option value="http://www.seaice.dk/damocles/google/DAMOCLES.kmz">DAMOCLES Arctic sea-ice</option>
            </select>-->
            <div id="calendar"></div>
        </div>
        
        <div id="imagePanel" class="imagePanel">
            <div id="map" class="map"></div>
            <!--<span id="pleaseWait">Loading, please wait...</span>-->
            <img id="scaleBar" class="scaleBar" src="images/rainbowScaleBar.png" alt="scale bar"/>
            <div class="scaleMarkers">
                <input id="scaleMax" class="scaleMax" type="text" size="4" onblur="javascript:validateScale()"/>
                <span id="scaleTwoThirds"></span>
                <span id="scaleOneThird"></span>
                <input id="scaleMin" class="scaleMin" type="text" size="4" onblur="javascript:validateScale()"/>
            </div>
            <span id="imageURL" class="imageURL"></span>
            <span id="opacityControl">Overlay opacity:
                <select id="opacityValue" onchange="javascript:updateMap()">
                    <option value="100" selected>100%</option>
                    <option value="66">66%</option>
                    <option value="33">33%</option>
                </select>
            </span>
        </div>
    </div>
    
    </body>
</html>
