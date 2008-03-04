<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/sql"  prefix="sql"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt"  prefix="fmt"%>

<%-- prepare the SQL queries that we will use on this page --%>
<sql:query var="numEntries" dataSource="${usageLogger.dataSource}">
select count(*) as count from usage_log
</sql:query>
<sql:query var="getMapRequests" dataSource="${usageLogger.dataSource}">
select count(*) as count from usage_log where wms_operation = 'GetMap'
</sql:query>
<sql:query var="getMapRequestsUsedCache" dataSource="${usageLogger.dataSource}">
select count(*) as count from usage_log where wms_operation = 'GetMap' and used_cache = true
</sql:query>
<sql:query var="getMapRequestsByClient" dataSource="${usageLogger.dataSource}">
select client_hostname, count(*) as count from usage_log where wms_operation = 'GetMap' group by client_hostname
</sql:query>
<sql:query var="getMapRequestsByUserAgent" dataSource="${usageLogger.dataSource}">
select client_user_agent, count(*) as count from usage_log where wms_operation = 'GetMap' group by client_user_agent
</sql:query>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Usage Monitor</title>
    </head>
    <body>
    
    <h1>ncWMS Usage Monitor</h1>
    <p>This page contains summary usage information.  If you need more detail you can
    download the whole usage log (containing ${numEntries.rows[0].count} entries)
    in CSV format (e.g. for Microsoft Excel) by clicking <a href="downloadUsageLog">here</a>.</p>
    
    <h2>Use of cache of image arrays</h2>
    <c:set var="getMapRequests" value="${getMapRequests.rows[0].count}"/>
    <c:set var="getMapRequestsUsedCache" value="${getMapRequestsUsedCache.rows[0].count}"/>
    <p>The cache was used to serve <b>${getMapRequestsUsedCache}</b>
        GetMap requests out of a total of <b>${getMapRequests}</b>
        (=<fmt:formatNumber value="${getMapRequestsUsedCache / getMapRequests}" type="percent" minFractionDigits="2"/>).
    </p>
    
    <h2>GetMap requests by client</h2>
    <table border="1">
        <tr>
            <th>Client hostname/IP</th>
            <th>Number of GetMap requests</th>
        </tr>
        <c:set var="total" value="0"/>
        <c:forEach var="row" items="${getMapRequestsByClient.rows}">
            <tr>
                <td><c:out value="${row.client_hostname}"/></td>
                <td><c:out value="${row.count}"/></td>
                <c:set var="total" value="${total + row.count}"/>
            </tr>
        </c:forEach>
        <tr>
            <th>TOTAL (check)</th>
            <th>${total}</th>
        </tr>
    </table>
    
    <h2>GetMap requests by user agent</h2>
    <table border="1">
        <tr>
            <th>Client user agent (browser)</th>
            <th>Number of GetMap requests</th>
        </tr>
        <c:set var="total" value="0"/>
        <c:forEach var="row" items="${getMapRequestsByUserAgent.rows}">
            <tr>
                <td><c:out value="${row.client_user_agent}"/></td>
                <td><c:out value="${row.count}"/></td>
                <c:set var="total" value="${total + row.count}"/>
            </tr>
        </c:forEach>
        <tr>
            <th>TOTAL (check)</th>
            <th>${total}</th>
        </tr>
    </table>
    
    </body>
</html>
