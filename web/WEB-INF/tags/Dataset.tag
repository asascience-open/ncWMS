<%@tag description="Displays a dataset as a set of layers in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/MenuMaker" prefix="menu"%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.config.Dataset" description="The dataset object to display"%>
<%@attribute name="server" description="Optional URL to the ncWMS server providing this dataset"%>
<menu:folder label="${dataset.title}">
    <c:forEach items="${dataset.layers}" var="layer">
        <menu:layer id="${layer.layerName}" label="${layer.title}" server="${server}"/>
    </c:forEach>
</menu:folder>