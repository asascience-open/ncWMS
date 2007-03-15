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
        contact.setName(request.getParameter("contact.name"));
        contact.setOrg(request.getParameter("contact.org"));
        contact.setTel(request.getParameter("contact.tel"));
        contact.setEmail(request.getParameter("contact.email"));

        // Process the server details
        server.setTitle(request.getParameter("server.title"));
        server.setAbstract(request.getParameter("server.abstract"));
        server.setKeywords(request.getParameter("server.keywords"));
        server.setUrl(request.getParameter("server.url"));
        server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
        server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));

        // Save the dataset information, checking for removals
        // First look through the existing datasets for edits.
        for (Dataset ds : conf.getDatasets().values())
        {
            if (request.getParameter("dataset." + ds.getId() + ".remove") != null)
            {
                conf.removeDataset(ds);
            }
            else
            {
                ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
                ds.setLocation(request.getParameter("dataset." + ds.getId() + ".location"));
                ds.setDataReaderClass(request.getParameter("dataset." + ds.getId() + ".reader"));
                ds.setQueryable(request.getParameter("dataset." + ds.getId() + ".queryable") != null);
                ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset." + ds.getId() + ".updateinterval")));
                ds.setId(request.getParameter("dataset." + ds.getId() + ".id"));
            }
        }
        // Now look for the new datasets
        for (int i = 0; i < numBlankDatasets; i++)
        {
            // Look for non-blank ID fields
            if (!request.getParameter("dataset.new" + i + ".id").trim().equals(""))
            {
                Dataset ds = new Dataset();
                ds.setId(request.getParameter("dataset.new" + i + ".id"));
                ds.setTitle(request.getParameter("dataset.new" + i + ".title"));
                ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
                ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
                ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
                ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset.new" + i + ".updateinterval")));
                conf.addDataset(ds);
            }
        }
        
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
    
    <p><a href="./index.jsp">Refresh this page (without saving)</a></p>
    <p><a href="/ncWMS">ncWMS Front page</a></p>
    <p><a href="/ncWMS/WMS.py?SERVICE=WMS&REQUEST=GetCapabilities">Capabilities document</a></p>
    
    <form id="config" action="index.jsp" method="POST">
        
        <input type="submit" value="Save configuration" name="submit1"/>
        
        <h2>Datasets</h2>
        <table border="1">
            <tr><th>ID</th><th>Title</th><th>Location</th><th>State</th><th>Queryable?</th><th>Data reading class</th><th>Refresh frequency</th><th>Reload?</th><th>Remove?</th></tr>
            
            <% for (Dataset ds : conf.getDatasets().values()) { %>
            <tr>
                <td><input type="text" name="dataset.<%=ds.getId()%>.id" value="<%=ds.getId()%>"/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.title" value="<%=ds.getTitle()%>"/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.location" value="<%=ds.getLocation()%>"/></td>
                <td><% if (ds.isError()) { %>
                    <a target="_blank" href="error.jsp?dataset=<%=ds.getId()%>"><%=ds.getState().toString()%></a>
              <%} else {
                    out.print(ds.getState().toString());
                }%></td>
                <td><input type="checkbox" name="dataset.<%=ds.getId()%>.queryable" <%=ds.isQueryable() ? "checked=\"checked\"" : ""%>/></td>
                <td><input type="text" name="dataset.<%=ds.getId()%>.reader" value="<%=ds.getDataReaderClass()%>"/></td>
                <td>
                    <select name="dataset.<%=ds.getId()%>.updateinterval">
                        <option value="-1" <%=ds.getUpdateInterval() < 0 ? "selected=\"selected\"" : ""%>>Never</option>
                        <option value="1" <%=ds.getUpdateInterval() == 1 ? "selected=\"selected\"" : ""%>>Every minute</option>
                        <option value="10" <%=ds.getUpdateInterval() == 10 ? "selected=\"selected\"" : ""%>>Every 10 minutes</option>
                        <option value="30" <%=ds.getUpdateInterval() == 30 ? "selected=\"selected\"" : ""%>>Every half hour</option>
                        <option value="60" <%=ds.getUpdateInterval() == 60 ? "selected=\"selected\"" : ""%>>Hourly</option>
                        <option value="360" <%=ds.getUpdateInterval() == 360 ? "selected=\"selected\"" : ""%>>Every 6 hours</option>
                        <option value="720" <%=ds.getUpdateInterval() == 720 ? "selected=\"selected\"" : ""%>>Every 12 hours</option>
                        <option value="1440" <%=ds.getUpdateInterval() == 1440 ? "selected=\"selected\"" : ""%>>Daily</option>
                    </select>
                </td>
                <td><input type="checkbox" name="dataset.<%=ds.getId()%>.reload"/></td>
                <td><input type="checkbox" name="dataset.<%=ds.getId()%>.remove"/></td>
            </tr>
            <%
            }
            for (int i = 0; i < numBlankDatasets; i++) { %>
            <tr>
                <td><input type="text" name="dataset.new<%=i%>.id" value=""/></td>
                <td><input type="text" name="dataset.new<%=i%>.title" value=""/></td>
                <td><input type="text" name="dataset.new<%=i%>.location" value=""/></td>
                <td>N/A</td>
                <td><input type="checkbox" name="dataset.new<%=i%>.queryable" checked="checked"/></td>
                <td><input type="text" name="dataset.new<%=i%>.reader" value=""/></td>
                <td>
                    <select name="dataset.new<%=i%>.updateinterval">
                        <option value="-1">Never</option>
                        <option value="1">Every minute</option>
                        <option value="10">Every 10 minutes</option>
                        <option value="30">Every half hour</option>
                        <option value="60">Hourly</option>
                        <option value="360">Every 6 hours</option>
                        <option value="720">Every 12 hours</option>
                        <option value="1440">Daily</option>
                    </select>
                </td>
                <td>N/A</td>
                <td>N/A</td>
            </tr>
         <% } %>
        </table>        
        
        <h2>Server metadata</h2>
        <table border="1">
            <tr><th>Title</th><td><input type="text" name="server.title" value="<%=server.getTitle()%>"/></td><td>Title for this WMS</td></tr>
            <!-- TODO: make the abstract field larger -->
            <tr><th>Abstract</th><td><input type="text" name="server.abstract" value="<%=server.getAbstract()%>"/></td><td>More details about this server</td></tr>
            <tr><th>Keywords</th><td><input type="text" name="server.keywords" value="<%=server.getKeywords()%>"/></td><td>Comma-separated list of keywords</td></tr>
            <tr><th>URL</th><td><input type="text" name="server.url" value="<%=server.getUrl()%>"/></td><td>Web site of the service provider</td></tr>
            <!-- TODO: do integer validation on max width and height -->
            <tr><th>Max image width</th><td><input type="text" name="server.maximagewidth" value="<%=server.getMaxImageWidth()%>"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Max image height</th><td><input type="text" name="server.maximageheight" value="<%=server.getMaxImageHeight()%>"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Allow GetFeatureInfo</th><td><input type="checkbox" name="server.allowfeatureinfo" <%=server.isAllowFeatureInfo() ? "checked=\"checked\"" : ""%>/></td><td>Check this box to enable the GetFeatureInfo operation</td></tr>
        </table>
        
        <h2>Contact information</h2>
        <table border="1">
            <tr><th>Name</th><td><input type="text" name="contact.name" value="<%=contact.getName()%>"/></td><td>Name of server administrator</td></tr>
            <tr><th>Organization</th><td><input type="text" name="contact.org" value="<%=contact.getOrg()%>"/></td><td>Organization of server administrator</td></tr>
            <tr><th>Telephone</th><td><input type="text" name="contact.tel" value="<%=contact.getTel()%>"/></td><td>Telephone number of server administrator</td></tr>
            <tr><th>Email</th><td><input type="text" name="contact.email" value="<%=contact.getEmail()%>"/></td><td>Email address of server administrator</td></tr>
        </table>
        
        <br />
        <input type="submit" value="Save configuration" name="submit2"/>
        
    </form>
    
    </body>
</html>
