<%@tag description="Displays a single displayable layer, taking into account elevation and time dimensions" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms2kml" prefix="wms2kml"%>
<%@attribute name="layer" required="true" type="uk.ac.rdg.resc.ncwms.metadata.Layer" description="Layer object"%>
<%@attribute name="baseURL" required="true" description="URL to use as a base for any callbacks to this server, e.g. in NetworkLinks"%>
<c:choose>
    <c:when test="${empty layer.zvalues}">
        <wms2kml:layerTimesteps layer="${layer}" baseURL="${baseURL}"/>
    </c:when>
    <c:otherwise>
        <%-- Create a folder to contain all the elevation values --%>
        <Folder>
            <name>${layer.title} from ${layer.dataset.title}</name>
            <description>${layer.abstract}</description>
            <visibility>0</visibility>
            <open>0</open>
            <c:forEach items="${layer.zvalues}" var="elevation">
                <wms2kml:layerTimesteps layer="${layer}" elevation="${elevation}" baseURL="${baseURL}"/>
            </c:forEach>
        </Folder>
    </c:otherwise>
</c:choose>