<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%-- 
     Displays the variables of a dataset as JSON object.
     See MetadataController.showVariables().
     
     Data (models) passed in to this page:
         dataset = Dataset containing the variables
--%>
<json:object>
    <c:forEach var="layer" items="${dataset.layers}">
        <json:property name="${layer.id}" value="${layer.title}"/>
    </c:forEach>
</json:object>