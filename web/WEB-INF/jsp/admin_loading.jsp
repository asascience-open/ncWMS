<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%-- Display of the progress loading a dataset
     Data (models) passed in to this page:
         dataset     = uk.ac.rdg.resc.ncwms.config.Dataset --%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Progress loading dataset ${dataset.id}</title>
    </head>
    <body>

    <h1>Progress loading dataset ${dataset.id} (for debugging)</h1>
    
    <p>
        <b>Loading state:</b> ${dataset.state}
    </p>
    <p>
        <b>Progress information:</b> ${dataset.loadingProgress}
    </p>
        
    </body>
</html>
