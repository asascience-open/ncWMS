<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays the details of a variable as an XML document
     See MetadataController.showVariableDetails().
     
     Data (models) passed in to this page:
         layer = Layer object --%>

<variableDetails dataset="${layer.dataset.id}" variable="${layer.title}" units="${layer.units}">
    <axes>
        <c:if test="${not empty layer.zvalues}">
        <axis type="z" units="${layer.zunits}" positive="${layer.zpositive}">
            <c:forEach var="z" items="${layer.zvalues}">
            <value>${utils:abs(z)}</value>
            </c:forEach>
        </axis>
        </c:if>
    </axes>
    <range><min>${layer.validMin}</min><max>${varilayerble.validMax}</max></range>
    <bbox>${layer.bbox[0]},${layer.bbox[1]},${layer.bbox[2]},${layer.bbox[3]}</bbox>
</variableDetails>