<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="uk.ac.rdg.resc.ncwms.WMS"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%--
    TODO: get the titles etc from a configuration file
--%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>NetCDF Web Map Server</title>
    </head>
    <body>

    <h1>NetCDF Web Map Server</h1>
    <p>
    <%-- TODO: how do we get the context path of the WMS servlet automatically? --%>
    <a href="./WMS?SERVICE=WMS&REQUEST=GetCapabilities">GetCapabilities</a><br />
    <a href="./WMS?SERVICE=WMS&REQUEST=GetMap&VERSION=<%=WMS.VERSION%>&LAYERS=FOAM/TMP&STYLES=&CRS=<%=WMS.CRS_84%>&BBOX=0,0,90,90&WIDTH=100&HEIGHT=100&FORMAT=image/png">GetMap</a><br />
    
    </p>
    
    </body>
</html>
