<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Usage Monitor</title>
    </head>
    <body>
    
    <h1>ncWMS Usage Monitor</h1>
    <p>This page contains summary usage information.  If you need more detail you can
    download the whole usage log in CSV format (e.g. for Microsoft Excel) by clicking
    <a href="downloadUsageLog">here</a>.</p>
    
    <p>Number of GetMap requests received by this server = ${usageLogger.numGetMapRequests},
    of which ${usageLogger.numCachedGetMapRequests} were served by the cache of data arrays.</p>
    
    </body>
</html>
