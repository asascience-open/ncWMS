<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%-- 
     Displays the variables of a dataset as an array of JSON objects.
     See MetadataController.showVariables().
     
     Data (models) passed in to this page:
         dataset = Dataset containing the variables
--%>
<json:object>
    <json:array name="variables" items="${dataset.layers}" var="layer">
        <json:object>
            <json:property name="id" value="${layer.id}"/>
            <json:property name="title" value="${layer.title}"/>
        </json:object>
    </json:array>
</json:object>