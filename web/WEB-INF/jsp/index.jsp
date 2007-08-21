<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Front page of the ncWMS server.
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config)
         metadataStore = Store of metadata (uk.ac.rdg.resc.ncwms.metadata.MetadataStore)
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
     --%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>${config.server.title}</title>
    </head>
    <body>

    <h1>${config.server.title}</h1>
    
    <p><a href="wms?SERVICE=WMS&REQUEST=GetCapabilities">Capabilities document</a></p>
    <p><a href="godiva2.html">Godiva2 interface</a></p>
    <p><a href="admin/">Admin interface (requires login)</a></p>
    
    <h2>Datasets:</h2>
    <!-- Print a GetMap link for every dataset we have -->
    <table border="1">
        <tr>
            <th>Dataset</th>
            <c:forEach var="mimeType" items="${supportedImageFormats}">
                <th>${mimeType}</th>
            </c:forEach>
            <c:if test="${config.server.allowFeatureInfo}">
                <th>FeatureInfo</th>
            </c:if>
        </tr>
        <c:forEach var="dataset" items="${config.datasets}">
        <c:if test="${dataset.value.ready}">
        <tr>
            <th>${dataset.value.title}</th>
            <c:set var="layers" value="${dataset.value.layers}"/>
            <c:forEach var="mimeType" items="${supportedImageFormats}">
            <td>
                <c:forEach var="layer" items="${layers}">
                <c:set var="bbox" value="${layer.bbox}"/>
                <a href="wms?REQUEST=GetMap&amp;VERSION=${utils:wmsVersion()}&amp;STYLES=&amp;CRS=CRS:84&amp;WIDTH=256&amp;HEIGHT=256&amp;FORMAT=${mimeType}&amp;TRANSPARENT=true&amp;LAYERS=${layer.layerName}&amp;BBOX=${bbox[0]},${bbox[1]},${bbox[2]},${bbox[3]}">${layer.title}</a><br />
                </c:forEach>
            </td>
            </c:forEach>
            <c:if test="${config.server.allowFeatureInfo}">
            <td>
                <c:forEach var="layer" items="${layers}">
                <c:set var="bbox" value="${layer.bbox}"/>
                <a href="wms?REQUEST=GetFeatureInfo&amp;VERSION=${utils:wmsVersion()}&amp;STYLES=&amp;CRS=CRS:84&amp;WIDTH=256&amp;HEIGHT=256&amp;I=128&amp;J=128&amp;INFO_FORMAT=text/xml&amp;QUERY_LAYERS=${layer.layerName}&amp;BBOX=${bbox[0]},${bbox[1]},${bbox[2]},${bbox[3]}">${layer.title}</a><br />
                </c:forEach>
            </td>
            </c:if>
        </tr>
        </c:if>
        </c:forEach>
    </table>
    
    </body>
</html>
