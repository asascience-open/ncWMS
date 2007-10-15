<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%--
     Displays the details of a variable as a JSON object
     See MetadataController.showVariableDetails().
     
     Data (models) passed in to this page:
         layer = Layer object
         datesWithData = Map<Integer, Map<Integer, List<Integer>>>.  Contains
                information about which days contain data for the Layer.  Maps
                years to a map of months to an array of day numbers.
--%>
<json:object>
    <json:property name="title" value="${layer.title}"/>
    <json:property name="units" value="${layer.units}"/>
    <json:array name="bbox" items="${layer.bbox}"/>
    <c:if test="${layer.zaxisPresent}">
        <json:object name="zaxis">
            <json:property name="units" value="${layer.zunits}"/>
            <json:property name="positive" value="${layer.zpositive}"/>
            <json:array name="values" items="${layer.zvalues}" var="z">
                ${utils:abs(z)}
            </json:array>
        </json:object>
    </c:if>
    <c:if test="${layer.taxisPresent}">
        <json:object name="datesWithData">
            <c:forEach var="year" items="${datesWithData}">
                <json:object name="${year.key}">
                    <c:forEach var="month" items="${year.value}">
                        <json:array name="${month.key}" items="${month.value}"/>
                    </c:forEach>
                </json:object>
            </c:forEach>
        </json:object>
    </c:if>
</json:object>
