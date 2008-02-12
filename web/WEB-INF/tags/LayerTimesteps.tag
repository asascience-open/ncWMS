<%@tag description="Displays all the timesteps for a layer as separate overlays" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wms2kml" prefix="wms2kml"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="wmsUtils"%>
<%@attribute name="layer" required="true" type="uk.ac.rdg.resc.ncwms.metadata.Layer" description="Layer object"%>
<%@attribute name="baseURL" required="true" description="URL to use as a base for any callbacks to this server, e.g. in NetworkLinks"%>
<%@attribute name="elevation" required="false" description="elevation value"%>
<c:choose>
    <c:when test="${empty layer.timesteps}">
        <wms2kml:regionBasedOverlay layer="${layer}" elevation="${elevation}" baseURL="${baseURL}"/>
    </c:when>
    <c:otherwise>
        <%-- Create a folder to contain all the time values --%>
        <Folder>
            <c:choose>
                <c:when test="${empty elevation}">
                    <name>${layer.title} from ${layer.dataset.title}</name>
                    <description>${layer.abstract}</description>
                </c:when>
                <c:otherwise>
                    <%-- This is in a folder of elevation values --%>
                    <name>${elevation} ${layer.zunits}</name>
                </c:otherwise>
            </c:choose>
            <visibility>0</visibility>
            <open>0</open>
            <c:forEach items="${layer.timesteps}" var="timestep">
                <c:set var="isoTime" value="${wmsUtils:dateToISO8601(timestep.date)}"/>
                <wms2kml:regionBasedOverlay layer="${layer}" elevation="${elevation}" time="${isoTime}" baseURL="${baseURL}"/>
            </c:forEach>
        </Folder>
    </c:otherwise>
</c:choose>
