<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
<%-- Administrative page
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config) --%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Administrative interface to ncWMS</title>
    </head>
    <body>

    <h1>ncWMS Admin page</h1>

    <p><a href="../godiva2.html">Godiva2 interface</a></p>
    <c:choose>
        <c:when test="${config.server.allowGlobalCapabilities}">
            <p><a href="../wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.3.0">WMS 1.3.0 Capabilities</a></p>
            <p><a href="../wms?SERVICE=WMS&amp;REQUEST=GetCapabilities&amp;VERSION=1.1.1">WMS 1.1.1 Capabilities</a></p>
        </c:when>
        <c:otherwise>
            <em>The generation of Capabilities documents that include all datasets
            on this server is disabled: see Server Settings below.</em>
        </c:otherwise>
    </c:choose>
    <p><a href="usage.jsp">Usage monitor</a></p>
    <p><a href="../">ncWMS Front page</a></p>
    
    <form id="config" action="updateConfig" method="POST">
        
        <input type="submit" value="Save configuration" name="submit1"/>
        
        <h2>Datasets</h2>
        <table border="1">
        <tr><th>Unique ID</th><th>Title</th><th>Location</th><th>State</th><th>Last update</th><th>Auto refresh frequency</th><th>Force refresh?</th><th>Disabled?</th><th>Queryable?</th><th>Remove?</th><th>Data reading class</th><th>Link to more info</th><th>Copyright statement</th></tr>
            
            <c:forEach var="dataset" items="${config.datasets}">
                <tr<c:if test="${dataset.value.disabled}"> bgcolor="lightgrey"</c:if>>
                    <td><input type="text" name="dataset.${dataset.value.id}.id" value="${dataset.value.id}"/></td>
                    <td><input type="text" name="dataset.${dataset.value.id}.title" value="${dataset.value.title}"/></td>
                    <td><input type="text" name="dataset.${dataset.value.id}.location" value="${dataset.value.location}"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${dataset.value.error}">
                                <a target="_blank" href="error.jsp?dataset=${dataset.value.id}">${dataset.value.state}</a>
                            </c:when>
                            <c:otherwise>
                                ${dataset.value.state}
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${empty dataset.value.lastUpdate}">never</c:when>
                            <c:otherwise>${utils:dateToISO8601(dataset.value.lastUpdate)}</c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <select name="dataset.${dataset.value.id}.updateinterval">
                            <option value="-1"<c:if test="${dataset.value.updateInterval < 0}"> selected="selected"</c:if>>Never</option>
                            <option value="1"<c:if test="${dataset.value.updateInterval == 1}"> selected="selected"</c:if>>Every minute</option>
                            <option value="10"<c:if test="${dataset.value.updateInterval == 10}"> selected="selected"</c:if>>Every 10 minutes</option>
                            <option value="30"<c:if test="${dataset.value.updateInterval == 30}"> selected="selected"</c:if>>Every half hour</option>
                            <option value="60"<c:if test="${dataset.value.updateInterval == 60}"> selected="selected"</c:if>>Hourly</option>
                            <option value="360"<c:if test="${dataset.value.updateInterval == 360}"> selected="selected"</c:if>>Every 6 hours</option>
                            <option value="720"<c:if test="${dataset.value.updateInterval == 720}"> selected="selected"</c:if>>Every 12 hours</option>
                            <option value="1440"<c:if test="${dataset.value.updateInterval == 1440}"> selected="selected"</c:if>>Daily</option>
                        </select>
                    </td>
                    <td><input type="checkbox" name="dataset.${dataset.value.id}.refresh"/></td>
                    <td><input type="checkbox" name="dataset.${dataset.value.id}.disabled"<c:if test="${dataset.value.disabled}"> checked="checked"</c:if>/></td>
                    <td><input type="checkbox" name="dataset.${dataset.value.id}.queryable"<c:if test="${dataset.value.queryable}"> checked="checked"</c:if>/></td>
                    <td><input type="checkbox" name="dataset.${dataset.value.id}.remove"/></td>
                    <td><input type="text" name="dataset.${dataset.value.id}.reader" value="${dataset.value.dataReaderClass}"/></td>
                    <td><input type="text" name="dataset.${dataset.value.id}.moreinfo" value="${dataset.value.moreInfo}"/></td>
                    <td><input type="text" name="dataset.${dataset.value.id}.copyright" value="${dataset.value.copyrightStatement}"/></td>
                </tr>
            </c:forEach>
            <%-- Now add lines for the user to add new datasets --%>
            <c:forEach var="i" begin="0" end="2">
                <tr>
                    <td><input type="text" name="dataset.new${i}.id" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.title" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.location" value=""/></td>
                    <td>N/A</td>
                    <td>N/A</td>
                    <td>
                        <select name="dataset.new${i}.updateinterval">
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
                    <td><input type="checkbox" name="dataset.new${i}.disabled"/></td>
                    <td><input type="checkbox" name="dataset.new${i}.queryable" checked="checked"/></td>
                    <td>N/A</td>
                    <td><input type="text" name="dataset.new${i}.reader" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.moreinfo" value=""/></td>
                    <td><input type="text" name="dataset.new${i}.copyright" value=""/></td>
                </tr>
            </c:forEach>
        </table>
        
        <!--<h2>THREDDS (experimental!)</h2>
        THREDDS catalog location: <input type="text" name="thredds.catalog.location" value="${config.threddsCatalogLocation}" size="60"/>-->
        
        <h2>Cache settings</h2>
        <p>This ncWMS server uses a cache of recently-extracted data arrays to increase
        performance and reduce the load on the server.  Note that the cache will use up
        some memory and some disk space (configurable below).</p>
        <table border="1">
            <tr>
                <th>Enable cache?</th>
                <td><input type="checkbox" name="cache.enable"<c:if test="${config.cache.enabled}"> checked="checked"</c:if>/></td>
                <td>Check this box to enable the cache.  <font color="red">This can be changed while the server is running.</font></td>
            </tr>
            <tr>
                <th>Lifetime of each cached array (minutes)</th>
                <td><input type="text" name="cache.elementLifetime" value="${config.cache.elementLifetimeMinutes}"/></td>
                <td>Data arrays will expire automatically from the cache after this number of minutes.
                <font color="red">If you change this value you must restart the server for your change to take effect.</font></td>
            </tr>
            <tr>
                <c:set var="memoryFootprintMB" value="${256*256*4*config.cache.maxNumItemsInMemory / (1024*1024)}"/>
                <th>Maximum number of items to hold in memory</th>
                <td><input type="text" name="cache.maxNumItemsInMemory" value="${config.cache.maxNumItemsInMemory}"/></td>
                <td>If each item in the cache is a 256x256 array of 4-byte floating point data then 
                    this value gives a memory footprint for the cache of <b>${memoryFootprintMB} megabytes</b>.
                <font color="red">If you change this value you must restart the server for your change to take effect.</font></td>
            </tr>
            <tr>
                <th>Enable disk store?</th>
                <td><input type="checkbox" name="cache.enableDiskStore"<c:if test="${config.cache.enableDiskStore}"> checked="checked"</c:if>/></td>
                <td>Check this box to enable the disk store (recommended).  If enabled, items that
                    do not fit into the memory footprint will overflow to disk.  All
                    items in the cache will automatically be written to disk when the
                    server is shut down or restarted.
                <font color="red">If you change this value you must restart the server for your change to take effect.</font></td>
            </tr>
            <tr>
                <c:set var="diskFootprintMB" value="${256*256*4*config.cache.maxNumItemsOnDisk / (1024*1024)}"/>
                <th>Maximum number of items to hold on disk</th>
                <td><input type="text" name="cache.maxNumItemsOnDisk" value="${config.cache.maxNumItemsOnDisk}"/></td>
                <td>If each item in the cache is a 256x256 array of 4-byte floating point data then 
                    this value gives a disk footprint for the cache of <b>${diskFootprintMB} megabytes</b>.
                <font color="red">If you change this value you must restart the server for your change to take effect.</font></td>
            </tr>
        </table>
        
        <h2>Server settings</h2>
        <table border="1">
            <tr><th>Title</th><td><input type="text" name="server.title" value="${config.server.title}"/></td><td>Title for this WMS</td></tr>
            <!-- TODO: make the abstract field larger -->
            <tr><th>Abstract</th><td><input type="text" name="server.abstract" value="${config.server.abstract}"/></td><td>More details about this server</td></tr>
            <tr><th>Keywords</th><td><input type="text" name="server.keywords" value="${config.server.keywords}"/></td><td>Comma-separated list of keywords</td></tr>
            <tr><th>URL</th><td><input type="text" name="server.url" value="${config.server.url}"/></td><td>Web site of the service provider</td></tr>
            <!-- TODO: do integer validation on max width and height -->
            <tr><th>Max image width</th><td><input type="text" name="server.maximagewidth" value="${config.server.maxImageWidth}"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Max image height</th><td><input type="text" name="server.maximageheight" value="${config.server.maxImageHeight}"/></td><td>Maximum width of image that can be requested</td></tr>
            <tr><th>Allow GetFeatureInfo</th><td><input type="checkbox" name="server.allowfeatureinfo"<c:if test="${config.server.allowFeatureInfo}"> checked="checked"</c:if>/></td><td>Check this box to enable the GetFeatureInfo operation</td></tr>
            <tr><th>Allow global Capabilities</th><td><input type="checkbox" name="server.allowglobalcapabilities"<c:if test="${config.server.allowGlobalCapabilities}"> checked="checked"</c:if>/></td><td>Check this box to allow clients to request a
            WMS Capabilities document including all datasets on this server (not recommended if this server hosts a large number of datasets)</td></tr>
        </table>
        
        <h2>Contact information</h2>
        <table border="1">
            <tr><th>Name</th><td><input type="text" name="contact.name" value="${config.contact.name}"/></td><td>Name of server administrator</td></tr>
            <tr><th>Organization</th><td><input type="text" name="contact.org" value="${config.contact.org}"/></td><td>Organization of server administrator</td></tr>
            <tr><th>Telephone</th><td><input type="text" name="contact.tel" value="${config.contact.tel}"/></td><td>Telephone number of server administrator</td></tr>
            <tr><th>Email</th><td><input type="text" name="contact.email" value="${config.contact.email}"/></td><td>Email address of server administrator</td></tr>
        </table>
        
        <br />
        <input type="submit" value="Save configuration" name="submit2"/>
        
    </form>
    
    </body>
</html>
