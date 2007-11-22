<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the ECOOP site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=ECOOP"
     TODO: create complete catalogue from http://www.mersea.eu.org/html/information/catalog/products/catalog.html
     Or look at MIV v2??
     
     datasets = Map<String, Dataset>: all the datasets in this server
     --%>
<c:set var="esscServer" value="http://lovejoy.nerc-essc.ac.uk:8080/ncWMS/wms"/>
<menu:menu>
    <menu:dataset dataset="${datasets.OSTA}"/>
    <menu:layer/>
    <menu:layer/>
</menu:menu>
<%--{
    layers : [
    {
        "label" : "Global TEP",
        "children" : [
            {
                "label" : "Global 1/4 deg PSY3V2",
                "children" : [
                    <menu:layer server="${esscServer}"/>
                ]
            }
        ]
    }
    ]
}--%>