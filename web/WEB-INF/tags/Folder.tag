<%@tag description="Creates a folder (container) in the menu structure" pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@attribute name="label" description="Optional label for this folder"%>
<%-- TODO: label attribute is only optional for the top-level folder --%>
<json:object>
    <c:if test="${not empty label}">
        <json:property name="label" value="${label}"/>
    </c:if>
    <json:array name="children">
        <jsp:doBody/>
    </json:array>
</json:object>