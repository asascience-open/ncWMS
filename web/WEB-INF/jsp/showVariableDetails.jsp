<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays the details of a variable as an XML document
     See MetadataController.showVariableDetails().
     
     Data (models) passed in to this page:
         variable = VariableMetadata object --%>

<variableDetails dataset="${variable.dataset.id}" variable="${variable.title}" units="${variable.units}">
    <axes>
        <c:if test="${not empty variable.zvalues}">
        <axis type="z" units="${variable.zunits}" positive="${variable.zpositive}">
            <c:forEach var="z" items="${variable.zvalues}">
            <value>${utils:abs(z)}</value>
            </c:forEach>
        </axis>
        </c:if>
    </axes>
    <range><min>${variable.validMin}</min><max>${variable.validMax}</max></range>
    <bbox>${variable.bbox[0]},${variable.bbox[1]},${variable.bbox[2]},${variable.bbox[3]}</bbox>
</variableDetails>