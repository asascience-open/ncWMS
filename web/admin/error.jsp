<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.*"%>
<%
    // TODO: secure this page
    Config conf = (Config)application.getAttribute("config");
    String dsName = request.getParameter("dataset");
    Dataset ds = conf.getDatasets().get(dsName);
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Error with dataset <%=dsName%></title>
    </head>
    <body>

    <h1>Error stack trace for dataset <%=dsName%> (for debugging)</h1>
    
    <%  if (ds.getException() == null) { %>
        <p>This dataset does not contain any errors</p>
    <%  } else {%>
            <pre><% ds.getException().printStackTrace(new PrintWriter(out)); %></pre>
    <%  }%>
        
        
    </body>
</html>
