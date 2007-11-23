<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
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
<menu:folder label="mylabel">
    <menu:folder label="wally">
        <menu:dataset dataset="${datasets.NCOF_POLCOMS}"/>
    </menu:folder>
    <menu:dataset dataset="${datasets.OSTIA}"/>
    <menu:folder label="individual layers">
        <menu:layer id="OSTIA/analysed_sst" label="OSTIA SST"/>
        <menu:layer id="NCOF_POLCOMS/POT" label="POLCOMS SST"/>
    </menu:folder>
</menu:folder>