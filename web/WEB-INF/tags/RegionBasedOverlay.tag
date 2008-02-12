<%@tag description="Contains the top level of a region-based overlay" pageEncoding="UTF-8"%>
<%-- Thanks to Jason Birch, http://www.jasonbirch.com/nodes/2006/06/13/21/wms-on-steroids-kml-21-regions-application/ --%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@attribute name="layer" required="true" type="uk.ac.rdg.resc.ncwms.metadata.Layer" description="Layer object"%>
<%@attribute name="baseURL" required="true" description="URL to use as a base for any callbacks to this server, e.g. in NetworkLinks"%>
<%@attribute name="elevation" required="false" description="Elevation value"%>
<%@attribute name="time" required="false" description="Time value in ISO8601 format"%>
<c:set var="href" value="${baseURL}?REQUEST=GetKMLRegion"/>
<c:set var="name" value="${layer.title}"/>
<c:set var="useDescription" value="true"/>
<c:if test="${not empty elevation}">
    <c:set var="href" value="${href}&amp;ELEVATION=${elevation}"/>
    <c:set var="name" value="${elevation} ${layer.zunits}"/>
    <c:set var="useDescription" value="false"/>
</c:if>
<c:if test="${not empty time}">
    <c:set var="href" value="${href}&amp;TIME=${time}"/>
    <c:set var="name" value="${time}"/>
    <c:set var="useDescription" value="false"/>
</c:if>
<Folder>
    <name>${name}</name>
    <c:if test="${useDescription}">
        <description>${layer.abstract}</description>
    </c:if>
    <NetworkLink>
        <visibility>1</visibility> 
        <Region>
            <LatLonAltBox>
                <north>${layer.bbox[3]}</north>
                <south>${layer.bbox[1]}</south>
                <east>${layer.bbox[2]}</east>
                <west>${layer.bbox[0]}</west>
            </LatLonAltBox>
            <Lod>
                <minLodPixels>380</minLodPixels> 
                <maxLodPixels>-1</maxLodPixels> 
            </Lod>
        </Region>
        <Link>
            <viewRefreshMode>onRegion</viewRefreshMode>
            <href>${href}&amp;LAYER=${layer.layerName}&amp;DBOX=${layer.bbox[0]},${layer.bbox[1]},${layer.bbox[2]},${layer.bbox[3]}</href> 
        </Link>
        <c:if test="${not empty time}">
            <TimeStamp>
                <when>${time}</when>
            </TimeStamp>
        </c:if>
    </NetworkLink>
</Folder>
