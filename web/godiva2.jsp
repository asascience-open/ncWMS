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
        <script type="text/javascript" src="js/prototype.js"></script>
        <script type="text/javascript" src="js/rico.js"></script>
        <script type="text/javascript">
            var serverURL = '<%=serverURL%>';
        </script>
        <script type="text/javascript" src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=<%
            if (request.getServerPort() == 9080)
            {
                // This is the test server
                out.print("ABQIAAAA7YuB_Hd5LuBiQ3-he19uDxReA-wKwO_5JdwC5myEJFBQQHe_XBRzV2_S1wpru4MBkCzDmuA4Lj8jnA");
            }
            else if (request.getServerPort() == 8080)
            {
                out.print("ABQIAAAA7YuB_Hd5LuBiQ3-he19uDxSwjBCFN6caz8FNNh4puT4ThxF1chRFPKTCfeE2PRcErWgrLTcnmGWlkw");
            }
            else if (request.getServerPort() == 8084)
            {
                // key for http://localhost:8084
                out.print("ABQIAAAA7YuB_Hd5LuBiQ3-he19uDxTFRfqDGOwfXAlOK-54sJyR4NNS5RSdkyh_Ih5CfURmd5umFAKNKx8oJg");
            }%>"></script>
        <script type="text/javascript" src="js/gmaps.js"></script>
        <script type="text/javascript" src="js/godiva2.js"></script>
    </head>
    
    <body onunload="GUnload()">

    <!-- This is the left menu bar that will contain the list of datasets and variables -->
    <div id="accordionDiv" class="leftmenu">
    </div>

    <div id="help" class="help">
    <p><b>Welcome</b> to the <a href="http://www.resc.rdg.ac.uk">Reading e-Science Centre</a>'s GODIVA2 site.  This site
    demonstrates the visualization of ocean forecast and analysis data using
    Google Maps and Google Earth.  This site works best in Mozilla Firefox
    but also works in most modern browsers such as Internet Explorer 6.0 and Opera 8.5.</p>
    <p><b>How to use:</b> Select a dataset and variable (field) from the menu
    above. The data will be extracted and projected onto the Google Map (right).
    Navigate around the map by dragging it and using the zoom controls in the top
    left corner of the map. Change the date and time of the data being displayed
    by using the calendar control (top right).  Change the depth of the data
    being displayed by using the Depth drop-down box (if applicable).  Change
    the colour range by editing the boxes at the top and bottom of the colour 
    scale to the right of the map. (Firefox only: change the opacity of the 
    data overlay by using the control at the bottom right corner of the map.
    This allows you to see the bathymetry through the data.)</p>
    <p>At any time, click "Open in Google Earth" to view the data in <a href="http://earth.google.com">Google Earth</a>.
    You will need to install Google Earth first.</p>
    </div>
    
    <a href="http://www.resc.rdg.ac.uk/"><img id="resclogo" src="http://www.resc.rdg.ac.uk/images/new_logo_72dpi_web.png" alt="ReSC logo"></a>
    
    <div id="mainPanel" class="mainPanel">
        <div class="panelHeader">
            <b>Dataset:</b> <span id="datasetName">Please select from the left panel</span><br />
            <b>Variable:</b> <span id="variableName">Please select from the left panel</span><br />
            <span id="units"></span><br />
            <span id="zAxis"></span><select id="zValues" onchange="javascript:updateMap()"><option value="0">dummy</option></select><br />
            <span id="date"></span>&nbsp;<select id="tValues" onchange="javascript:updateTimestep()"><option value="0">dummy</option></select><br />
            <br />
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
                <select id="opacityValue" onchange="javascript:changeOpacity(this.value)">
                    <option value="100" selected>100%</option>
                    <option value="66">66%</option>
                    <option value="33">33%</option>
                </select>
            </span>
        </div>
    </div>
    
    </body>
</html>
