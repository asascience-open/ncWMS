<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%-- 
     Displays the datasets that the user has chosen to see as JSON, ready to be
     injected into the Godiva2 site.
     See MetadataController.showDatasets()
     
     Data (models) passed in to this page:
         datasets = List of Datasets to display
         
     returns a a JSON associative array, in which the keys are dataset ids, values are titles
--%>
<json:object>
    <c:forEach var="dataset" items="${datasets}">
        <json:property name="${dataset.id}" value="${dataset.title}"/>
    </c:forEach>
</json:object>