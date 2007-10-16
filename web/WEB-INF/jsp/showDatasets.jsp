<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%-- 
     Displays the datasets that the user has chosen to see as JSON, ready to be
     injected into the Godiva2 site.
     See MetadataController.showDatasets()
     
     Data (models) passed in to this page:
         datasets = List of Datasets to display
         
     returns a a JSON array of simple dataset objects, keyed by their unique ids,
     containing the dataset title
--%>
<json:object>
    <json:array name="datasets" items="${datasets}" var="dataset">
        <json:object>
            <json:property name="id" value="${dataset.id}"/>
            <json:property name="title" value="${dataset.title}"/>
        </json:object>
    </json:array>
</json:object>