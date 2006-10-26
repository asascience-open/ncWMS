<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
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
    <a href="./WMS.py?SERVICE=WMS&REQUEST=GetCapabilities">GetCapabilities</a><br />
    <a href="./WMS.py?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=FOAM/TMP&STYLES=&CRS=CRS:84&BBOX=-90,0,0,90&WIDTH=256&HEIGHT=256&FORMAT=image/png">GetMap</a><br />
    
    </p>
    
    </body>
</html>
