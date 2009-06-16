<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Display of the progress loading a dataset
     Data (models) passed in to this page:
         dataset     = uk.ac.rdg.resc.ncwms.config.Dataset --%>
Progress loading dataset ${dataset.id} (for debugging):
Loading state: ${dataset.state}
Progress information (refresh page for updates):
${dataset.loadingProgress}
