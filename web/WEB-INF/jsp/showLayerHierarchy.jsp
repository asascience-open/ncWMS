<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%--
     Displays the hierarchy of layers from this server as a JSON object
     See MetadataController.showLayerHierarchy().
     
     Data (models) passed in to this page:
         datasets  = list of datasets containing the layers to be displayed
--%>
<json:object>
    <json:array name="layers" items="${datasets}" var="dataset">
        <json:object>
            <json:property name="label" value="${dataset.title}"/>
            <json:array name="children" items="${dataset.layers}" var="layer">
                <json:object>
                    <json:property name="id" value="${layer.layerName}"/>
                    <json:property name="label" value="${layer.title}"/>
                </json:object>
            </json:array>
        </json:object>
    </json:array>
</json:object>
