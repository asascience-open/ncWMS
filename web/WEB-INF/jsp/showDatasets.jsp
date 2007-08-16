<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays the datasets that the user has chosen to see as a set of nested
     divs, ready to be injected into the Godiva2 site.
     See MetadataController.showDatasets()
     
     Data (models) passed in to this page:
         datasets = List of Datasets to display --%>
 
 <c:forEach var="dataset" items="${datasets}">
 <div id="${dataset.id}Div">
     <div id="${dataset.id}">${dataset.title}</div>
     <div id="${dataset.id}Content">
         Variables in the ${dataset.title} dataset will appear here
     </div>
 </div>
 </c:forEach>