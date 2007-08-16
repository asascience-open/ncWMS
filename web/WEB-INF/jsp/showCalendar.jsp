<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays a calendar and supporting metadata for a variable and particular focus time
     See MetadataController.showCalendar().
     
     Data (models) passed in to this page:
          nearestIndex = nearest index along time axis to the focus time (int)
          variable = The variable in question (VariableMetadata) --%>
<root>
    <%-- Calculate the nearest time to the focus time (in milliseconds since epoch) --%>
    <c:set var="nearestTime" value="${variable.tvalues[nearestIndex]}"/>
    <nearestValue>${utils:millisecondsToISO8601(nearestTime)}</nearestValue>
    <prettyNearestValue>${utils:formatPrettyDate(nearestTime)}</prettyNearestValue>
    <nearestIndex>${nearestIndex}</nearestIndex>

    <calendar>
        <table>
            <tbody>
                <%-- Add the navigation buttons at the top of the month view --%>
                <tr>
                    <td><a href="#" onclick="javascript:setCalendar('${variable.dataset.id}','${variable.id}','${utils:getYearBefore(nearestTime)}'); return false">&lt;&lt;</a></td>
                    <td><a href="#" onclick="javascript:setCalendar('${variable.dataset.id}','${variable.id}','${utils:getMonthBefore(nearestTime)}'); return false">&lt;</a></td>
                    <td colspan="3">${utils:getCalendarHeading(nearestTime)}</td>
                    <td><a href="#" onclick="javascript:setCalendar('${variable.dataset.id}','${variable.id}','${utils:getMonthAfter(nearestTime)}'); return false">&gt;</a></td>
                    <td><a href="#" onclick="javascript:setCalendar('${variable.dataset.id}','${variable.id}','${utils:getYearAfter(nearestTime)}'); return false">&gt;&gt;</a></td>
                </tr>
                <%-- Add the day-of-week headings --%>
                <tr><th>M</th><th>T</th><th>W</th><th>T</th><th>F</th><th>S</th><th>S</th></tr>
                <%-- Add the body of the calendar --%>
                <c:forEach var="week" items="${utils:getMonthCalendar(nearestTime, variable.tvalues)}">
                <tr>
                    <c:forEach var="day" items="${week}">
                        <c:choose>
                            <c:when test="${empty day}">
                                <td></td> <%-- This day doesn't appear in the calendar for this month --%>
                            </c:when>
                            <c:otherwise>
                                <c:choose>
                                    <c:when test="${day.tindex >= 0}">
                                        <td id="t${day.tindex}"><a href="#" onclick="javascript:getTimesteps('${variable.dataset.id}','${variable.id}','${day.tindex}','${utils:millisecondsToISO8601(variable.tvalues[day.tindex])}','${utils:formatPrettyDate(variable.tvalues[day.tindex])}'); return false">${day.dayNumber}</a></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>${day.dayNumber}</td> <%-- No data for this day --%>
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </tr>
                </c:forEach>
            </tbody>
        </table>
    </calendar>
</root>