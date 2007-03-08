<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.Collection"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.Config"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.Dataset"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">   
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administrative interface to ncWMS</title>
    </head>
    <body>

    <h1>ncWMS Admin page</h1>
    
    <h2>Server metadata</h2>
    <p>Title, contact details etc go here</p>
    
    <h2>Datasets</h2>
    <table border="1">
        <tr><th>ID</th><th>Title</th><th>Location</th><th>State</th><th>Data reading class</th></tr>
    
    <%
        Config conf = (Config)application.getAttribute("config");
        for (Dataset ds : conf.getDatasets().values())
        {
    %>
        <tr><td><%=ds.getId()%></td><td><%=ds.getTitle()%></td><td><%=ds.getLocation()%></td>
            <td><%=ds.getState().toString()%></td><td>TODO</td></tr>
    <%
        }
    %>
    </table>
    
    </body>
</html>
