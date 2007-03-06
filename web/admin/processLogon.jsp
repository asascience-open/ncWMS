<%@page import="java.security.MessageDigest"%>
<%@page import="org.apache.commons.codec.binary.Hex"%>
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
    // TODO: read the password from a database
    sha1.update("password".getBytes());
    String realHash = new String(Hex.encodeHex(sha1.digest()));
    
    boolean logonSucceeded = passwordHash.equals(realHash);
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
