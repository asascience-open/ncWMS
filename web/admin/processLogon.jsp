<%@page import="java.security.MessageDigest"%>
<%@page import="org.apache.commons.codec.binary.Hex"%>
<%
    String key = (String)session.getAttribute("key");
    String password = request.getParameter("pwd");
    if (key == null || password == null)
    {
        response.sendRedirect("login.jsp");
    }
    MessageDigest sha1 = MessageDigest.getInstance("SHA");
    sha1.update(key.getBytes());
    // TODO: read the password from a database
    sha1.update("password".getBytes());
    String hex = new String(Hex.encodeHex(sha1.digest()));
    boolean logonSucceeded = password.equals(hex);
    String destination = request.getParameter("destination");
    
    if (logonSucceeded)
    {
        session.setAttribute("user", request.getParameter("userID"));
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
        // TODO: forward a message and the destination
        String redirectTo = "login.jsp?failed=true";
        if (destination != null && !destination.trim().equals(""))
        {
            redirectTo += "&destination=" + destination;
        }
        response.sendRedirect(redirectTo);
    }
%>
