<%@page import="java.security.MessageDigest"%>
<%@page import="org.apache.commons.codec.binary.Hex"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.Config"%>
<%
    // Process a logon request and forward to an appropriate page
    String key = (String)session.getAttribute("key");
    String passwordHash = request.getParameter("passwordHash");
    if (key == null || passwordHash == null)
    {
        response.sendRedirect("login.jsp");
    }
    
    // Check that the user is logging on as "admin" with the correct password
    MessageDigest sha1 = MessageDigest.getInstance("SHA");
    sha1.update(key.getBytes());
    Config conf = (Config)application.getAttribute("config");
    String adminPassword = conf.getServer().getAdminPassword();
    boolean logonSucceeded = false;
    String username = request.getParameter("username");
    if (username != null && username.trim().equals("admin"))
    {
        sha1.update(adminPassword.getBytes());
        String realHash = new String(Hex.encodeHex(sha1.digest()));
        logonSucceeded = passwordHash.equals(realHash);
    }
    String destination = request.getParameter("destination");
    
    if (logonSucceeded)
    {
        session.setAttribute("user", username.trim());
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
