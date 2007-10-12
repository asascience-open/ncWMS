<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays the datasets that the user has chosen to see as JSON, ready to be
     injected into the Godiva2 site.
     See MetadataController.showDatasets()
     
     Data (models) passed in to this page:
         datasets = List of Datasets to display
         
     returns a a JSON Hashtable, in which the keys are dataset ids, values are titles --%>
 
{<c:forEach var="dataset" items="${datasets}" varStatus="status">
        <c:if test="${status.index > 0}">,</c:if>'${dataset.id}':'${dataset.title}'
</c:forEach>}