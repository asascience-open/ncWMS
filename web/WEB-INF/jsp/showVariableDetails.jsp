<%@page contentType="application/json"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays the details of a variable as a JSON object
     See MetadataController.showVariableDetails().
     
     Data (models) passed in to this page:
         layer = Layer object --%>
{
    'title' : '${layer.title}',
    'units' : '${layer.units}',
    'bbox' : [${layer.bbox[0]},${layer.bbox[1]},${layer.bbox[2]},${layer.bbox[3]}],
    'zaxis' : <c:choose>
                  <c:when test="${layer.zaxisPresent}">
                      {
                          'units' : '${layer.zunits}',
                          'positive' : ${layer.zpositive},
                          'values' : [
                          <c:forEach var="z" items="${layer.zvalues}" varStatus="status">
                              <c:if test="${status.index > 0}">,</c:if>${utils:abs(z)}
                          </c:forEach>
                          ]
                      }
                  </c:when>
                  <c:otherwise>null</c:otherwise>
              </c:choose>
}

