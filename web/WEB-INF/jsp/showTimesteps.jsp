<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays a drop-down box showing the available timesteps for a given date
     See MetadataController.showTimesteps().
     
     Data (models) passed in to this page:
         timesteps = list of times (in milliseconds since the epoch) that fall on this day --%>
<select id="tValues" onchange="javascript:updateMap()">
    <c:forEach var="timestep" items="${timesteps}">
    <option value="${utils:millisecondsToISO8601(timestep)}">${utils:formatPrettyTime(timestep)}</option>
    </c:forEach>
</select>