<%@page import="java.security.MessageDigest"%>
<%@page import="org.apache.commons.codec.binary.Hex"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.Config"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.User"%>
<%
    // Process a logon request and forward to an appropriate page
    String key = (String)session.getAttribute("key");
    String passwordHash = request.getParameter("passwordHash");
    if (key == null || passwordHash == null)
    {
        response.sendRedirect("login.jsp");
    }
    
    // Calculate what the password hash should be
    MessageDigest sha1 = MessageDigest.getInstance("SHA");
    sha1.update(key.getBytes());
    Config conf = (Config)application.getAttribute("config");
    User adminUser = conf.getUsers().get("admin");
    boolean logonSucceeded = false;
    // If there is no admin user specified no-one can log on to the admin pages
    if (adminUser != null)
    {
        sha1.update(adminUser.getPassword().getBytes());
        String realHash = new String(Hex.encodeHex(sha1.digest()));
        logonSucceeded = passwordHash.equals(realHash);
    }
    String destination = request.getParameter("destination");
    
    if (logonSucceeded)
    {
        session.setAttribute("user", request.getParameter("username"));
        if (destination != null && !destination.trim().equals(""))
        {
            response.sendRedirect(destination);
        }
        else
        {
            response.sendRedirect("index.jsp");
        }
    }
    else
    {
        String redirectTo = "login.jsp?failed=true";
        if (destination != null && !destination.trim().equals(""))
        {
            redirectTo += "&destination=" + destination;
        }
        response.sendRedirect(redirectTo);
    }
%>
