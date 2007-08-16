<%-- This page is called when we have updated the configuration information via
     the admin interface.  We simply redirect back to the admin page.  We do this
     to prevent the user from accidentally resending the config data by
     pressing refresh --%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:redirect url="index.jsp"/>
