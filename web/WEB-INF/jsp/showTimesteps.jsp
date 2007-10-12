<%@page contentType="application/json"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays a JSON object showing the available timesteps for a given date
     See MetadataController.showTimesteps().
     
     Data (models) passed in to this page:
         timesteps = list of times (in milliseconds since the epoch) that fall on this day --%>
{<c:forEach var="timestep" items="${timesteps}" varStatus="status">
        <c:if test="${status.index > 0}">,</c:if>'${utils:millisecondsToISO8601(timestep)}':'${utils:formatPrettyTime(timestep)}'
</c:forEach>}