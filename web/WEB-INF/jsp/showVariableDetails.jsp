<%@page contentType="application/json"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays the details of a variable as a JSON object
     See MetadataController.showVariableDetails().
     
     Data (models) passed in to this page:
         layer = Layer object --%>

<%--<variableDetails dataset="${layer.dataset.id}" variable="${layer.title}" units="${layer.units}">
    <axes>
        <c:if test="${not empty layer.zvalues}">
        <axis type="z" units="${layer.zunits}" positive="${layer.zpositive}">
            <c:forEach var="z" items="${layer.zvalues}">
            <value>${utils:abs(z)}</value>
            </c:forEach>
        </axis>
        </c:if>
    </axes>
    <bbox>${layer.bbox[0]},${layer.bbox[1]},${layer.bbox[2]},${layer.bbox[3]}</bbox>
</variableDetails>--%>
{
    'title' : '${layer.title}',
    'units' : '${layer.units}',
    'bbox' : [${layer.bbox[0]},${layer.bbox[1]},${layer.bbox[2]},${layer.bbox[3]}],
    'zaxis' : <c:choose>
                  <c:when test="${empty layer.zvalues}">null</c:when>
                  <c:otherwise>
                      {
                          'units' : '${layer.zunits}',
                          'positive' : ${layer.zpositive},
                          'values' : [
                          <c:forEach var="z" items="${layer.zvalues}" varStatus="status">
                              <c:if test="${status.index > 0}">,</c:if>${utils:abs(z)}
                          </c:forEach>
                          ]
                      }
                  </c:otherwise>
              </c:choose>
}

