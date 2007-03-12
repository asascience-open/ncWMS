<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="uk.ac.rdg.resc.ncwms.config.*"%>
<%
    // TODO: protect this page under the login filter
    
    // Saves the configuration information as posted from index.jsp
    // Get the config object from the session
    Config conf = (Config)application.getAttribute("config");
    
    // Process the contact details
    Contact contact = conf.getContact();
    contact.setName(request.getParameter("contact.name").trim());
    contact.setOrg(request.getParameter("contact.org").trim());
    contact.setTel(request.getParameter("contact.tel").trim());
    contact.setEmail(request.getParameter("contact.email").trim());
    
    // Process the server details
    Server server = conf.getServer();
    server.setTitle(request.getParameter("server.title").trim());
    server.setAbstract(request.getParameter("server.abstract").trim());
    server.setKeywords(request.getParameter("server.keywords").trim());
    server.setUrl(request.getParameter("server.url").trim());
    server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
    server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));
    
    // Save the config information
    // TODO: trap exceptions and forward to appropriate confirmation page
    conf.saveConfig();
%>