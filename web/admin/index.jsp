<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.Collection"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%
    Config conf = (Config)application.getAttribute("config");
    Contact contact = conf.getContact();
    Server server = conf.getServer();
    boolean configSaved = false;
    int numBlankDatasets = 3;
    
    if (request.getParameter("contact.name") != null)
    {
        contact.setName(request.getParameter("contact.name").trim());
        contact.setOrg(request.getParameter("contact.org").trim());
        contact.setTel(request.getParameter("contact.tel").trim());
        contact.setEmail(request.getParameter("contact.email").trim());

        // Process the server details
        server.setTitle(request.getParameter("server.title").trim());
        server.setAbstract(request.getParameter("server.abstract").trim());
        server.setKeywords(request.getParameter("server.keywords").trim());
        server.setUrl(request.getParameter("server.url").trim());
        server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
        server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));

        // TODO: save the dataset information, checking for removals
        
        // Save the config information
        // TODO: trap exceptions and forward to appropriate confirmation page
        conf.saveConfig();
        configSaved = true;
    }
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administrative interface to ncWMS</title>
    </head>
    <body>

    <h1>ncWMS Admin page</h1>
    
    <%
        if (configSaved)
        {%>
        <p>Configuration saved.</p>
      <%}
    %>
    
    <form id="config" action="index.jsp" method="POST">
        <h2>Server metadata</h2>
        <table border="1">
            <tr><th>Title</th><td><input type="text" name="server.title" value="<%=server.getTitle()%>"/></td><td>Title for this WMS</td></tr>
            <!-- TODO: make the abstract field larger -->
            <tr><th>Abstract</th><td><input type="text" name="server.abstract" value="<%=server.getAbstract()%>"/></td><td>More details about this server</td></tr>
            <tr><th>Keywords</th><td><input type="text" name="server.keywords" value="<%=server.getKeywords()%>"/></td><td>Comma-separated list of keywords</td></tr>
            <tr><th>URL</th><td><input type="text" name="server.url" value="<%=server.getUrl()%>"/></td><td><b>How explain this?</b></td></tr>
            <!-- TODO: do integer validation on max width and height -->
            <tr><th>Max image width</th><td><input type="text" name="server.maximagewidth" value="<%=server.getMaxImageWidth()%>"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Max image height</th><td><input type="text" name="server.maximageheight" value="<%=server.getMaxImageHeight()%>"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Allow GetFeatureInfo</th><td><input type="checkbox" name="server.allowfeatureinfo" <%=server.isAllowFeatureInfo() ? "checked=\"checked\"" : ""%>/></td><td>Check this box to enable the GetFeatureInfo operation</td></tr>
            <!-- TODO: allow password to be changed?  Would need good encryption.  Perhaps
                 have server generate a temporary key pair in the session, then encrypt password
                 on client with public key. -->
        </table>
        
        <h2>Contact information</h2>
        <table border="1">
            <tr><th>Name</th><td><input type="text" name="contact.name" value="<%=contact.getName()%>"/></td><td>Name of server administrator</td></tr>
            <tr><th>Organization</th><td><input type="text" name="contact.org" value="<%=contact.getOrg()%>"/></td><td>Organization of server administrator</td></tr>
            <tr><th>Telephone</th><td><input type="text" name="contact.tel" value="<%=contact.getTel()%>"/></td><td>Telephone number of server administrator</td></tr>
            <tr><th>Email</th><td><input type="text" name="contact.email" value="<%=contact.getEmail()%>"/></td><td>Email address of server administrator</td></tr>
        </table>
        
        <h2>Datasets</h2>
        <table border="1">
            <tr><th>ID</th><th>Title</th><th>Location</th><th>State</th><th>Queryable?</th><th>Data reading class</th><th>Remove?</th></tr>
            
            <%
            for (Dataset ds : conf.getDatasets().values())
            {
            %>
            <tr>
                <td><input type="text" name="dataset.<%=ds.getId()%>.id" value="<%=ds.getId()%>"/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.title" value="<%=ds.getTitle()%>"/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.location" value="<%=ds.getLocation()%>"/></td>
                <!-- TODO: turn into hyperlink to problem page -->
                <td><%=ds.getState().toString()%></td>
                <td><input type="checkbox" name="dataset.<%=ds.getId()%>.queryable" <%=ds.isQueryable() ? "checked=\"checked\"" : ""%>/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.reader" value="<%=ds.getDataReaderClass()%>"/></td>
                <td><input type="checkbox" name="remove.<%=ds.getId()%>"/></td>
            </tr>
            <%
            }
            for (int i = 0; i < numBlankDatasets; i++)
            {
            %>
            <%--<tr>
                <td><%=ds.getId()%></td>
                <td><%=ds.getTitle()%></td>
                <td><%=ds.getLocation()%></td>
                <td><%=ds.getState().toString()%></td>
                <td>TODO</td>
                <td><input type="checkbox" name="remove.<%=ds.getId()%>"/></td>
            </tr>--%>
            <%
            }
            %>
            
        </table>
        
        <input type="submit" value="Save configuration" name="submit"/>
        
    </form>
    
    </body>
</html>
