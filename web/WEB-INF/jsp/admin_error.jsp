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
<%-- Display of the error associated with a dataset
     Data (models) passed in to this page:
         dataset     = uk.ac.rdg.resc.ncwms.config.Dataset --%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Error with dataset ${dataset.id}</title>
    </head>
    <body>

    <h1>Error report for dataset ${dataset.id} (for debugging)</h1>
    
    <c:set var="err" value="${dataset.exception}"/>
    <c:choose>
        <c:when test="${empty err}">
            This dataset does not contain any errors
        </c:when>
        <c:otherwise>
            <b>Stack trace:</b><br />
            ${err.class.name}: ${err.message}<br />
            <c:forEach var="stacktraceelement" items="${err.stackTrace}">
            ${stacktraceelement}<br />
            </c:forEach>
        </c:otherwise>
    </c:choose>
        
    </body>
</html>
