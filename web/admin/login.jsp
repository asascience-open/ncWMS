<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="java.util.Random"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
// Log out the user
session.setAttribute("user", null);

// Generate a random key as a challenge from the server
Random random =  new Random();
long r1 = random.nextLong();
long r2 = random.nextLong();
String hash1 = Long.toHexString(r1);
String hash2 = Long.toHexString(r2);
String key = hash1 + hash2;
// Store the key in the session
session.setAttribute("key", key);

String destination = request.getParameter("destination") == null ? "" :
    request.getParameter("destination");
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administrative login to ncWMS</title>
        <!-- Contains secure hash algorithm from http://pajhome.org.uk/crypt/md5/instructions.html -->
        <script type="text/javascript" src="sha1.js"></script>
        <script type="text/javascript">
        // Fills in the form and encrypts the password before sending it
        function submitForm() {
            var form = document.forms['login'];
            form.elements['username'].value = document.getElementById('userID').value;
            // We concatenate the key from the server with the password, then hash it
            form.elements['passwordHash'].value = hex_sha1('<%=key%>' + document.getElementById('pwd').value);
            form.submit();
        }
        </script>
    </head>
    <body>
        
        <h1>Please log in</h1>
        
        <%
        String failedStr = request.getParameter("failed");
        if (failedStr != null && failedStr.toLowerCase().trim().equals("true"))
        {
        %>
        <p>Login failed. Please try again.</p>
        <%
        }
        %>
        
        <table border="0" cellpadding="0" cellspacing="2" width="200">
            <tr>
                <td width="50%">Username:</td>
                <td width="50"><input type="text" id="userID"/></td>
            </tr>
            <tr>
                <td width="50%">Password:</td>
                <td width="50%"><input type="password" id="pwd"/></td>
            </tr>
            <tr>
                <td width="50%" colspan="2" align="center">
                    <%-- We don't make this a submit button to ensure that the form
                         can only be submitted through the javascript submitForm() method --%>
                    <input type="button" value="submit" onclick="submitForm()"/>
                </td>
            </tr>
        </table>
        
        <%-- We use a hidden form to transmit the username and password information.
             If we make the above inputs part of the form we either end up submitting
             the plaintext password, or we show the password being hashed just before
             the form is submitted. --%>
        <form id="login" name="login" method="POST" action="processLogon.jsp">
            <!-- These will be filled in by the javascript submitForm() method -->
            <input type="hidden" name="username" value=""/>
            <input type="hidden" name="passwordHash" value=""/>
            <!-- This stores the location to where we will be redirected following
                 a successful login -->
            <input type="hidden" name="destination" value="<%=destination%>"/>
        </form>
        
    </body>
</html>
