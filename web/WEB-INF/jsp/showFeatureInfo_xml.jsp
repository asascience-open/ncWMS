<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- Shows data from a GetFeatureInfo request as XML
     
     Data (models) passed in to this page:
          longitude = longitude of the point of interest (float)
          latitude = latitude of the point of interest (float)
          data = Map of times and data values (Map<Date, Float>) --%>
<FeatureInfoResponse>
    <longitude>${longitude}</longitude>
    <latitude>${latitude}</latitude>
    <c:forEach var="datapoint" items="${data}">
    <FeatureInfo>
        <c:if test="${not empty datapoint.key}">
            <time>${utils:dateToISO8601(datapoint.key)}</time>
        </c:if>
        <c:choose>
            <c:when test="${empty datapoint.value}">
                <value>none</value>
            </c:when>
            <c:otherwise>
                <value>${datapoint.value}</value>
            </c:otherwise>
        </c:choose>
    </FeatureInfo>
    </c:forEach>
</FeatureInfoResponse>