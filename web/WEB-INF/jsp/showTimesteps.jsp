<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays a JSON object showing the available timesteps for a given date
     See MetadataController.showTimesteps().
     
     Data (models) passed in to this page:
         timesteps = list of times (in milliseconds since the epoch) that fall on this day --%>
<json:object>
    <json:array name="timesteps" items="${timesteps}" var="t">
        ${utils:formatUTCTimeOnly(t)}
    </json:array>
</json:object>