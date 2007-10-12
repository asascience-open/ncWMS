<%@page contentType="application/json"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays the variables of a dataset as JSON object.
     See MetadataController.showVariables().
     
     Data (models) passed in to this page:
         dataset = Dataset containing the variables --%>
 
{<c:forEach var="layer" items="${dataset.layers}" varStatus="status">
        <c:if test="${status.index > 0}">,</c:if>'${layer.id}':'${layer.title}'
</c:forEach>}