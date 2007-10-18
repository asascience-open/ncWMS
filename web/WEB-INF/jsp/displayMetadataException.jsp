<%@page contentType="text/plain"%> <%-- TODO: replace with json MIME type --%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%-- Displays a MetadataException in JSON format
     Objects passed in to this page:
     exception : A MetadataException object --%>
<json:object>
    <json:object name="exception">
        <json:property name="class" value="${exception.cause.class.name}"/>
        <json:property name="message" value="${exception.cause.message}"/>
    </json:object>
</json:object>