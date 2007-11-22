<%@tag description="Displays a single Layer in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="server" description="URL to the ncWMS server providing this layer"%>
<json:object>
    <json:property name="id" value="MERSEA_GLOBAL/surface_downward_stress"/>
    <json:property name="label" value="Surface Downward Stress"/>
    <json:property name="server" value="${server}"/>
</json:object>