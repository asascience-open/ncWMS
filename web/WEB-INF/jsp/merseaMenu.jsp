<%@page contentType="text/plain"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/taglib/MenuMaker" prefix="menu"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%-- This file defines the menu structure for the MERSEA site.  This file will
     be loaded if someone loads up the godiva2 site with "menu=MERSEA"--%>
 <menu:folder label="MERSEA Dynamic Quick View">
     <menu:folder label="Global TEP">
         <menu:dataset dataset="${datasets.MERSEA_GLOBAL}"/>
         <menu:dataset dataset="${datasets.MERSEA_NRTSLA}"/>
         <%-- The CORIOLIS Temp and Sal come from two different datasets so we
              manually aggregate them in a single folder --%>
         <menu:folder label="CORIOLIS global in-situ daily">
             <menu:layer id="MERSEA_CORIOLIS_TEMP/temperature" label="temperature"/>
             <menu:layer id="MERSEA_CORIOLIS_SAL/salinity" label="salinity"/>
         </menu:folder>
     </menu:folder>
     <menu:folder label="Baltic TEP">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
     </menu:folder>
     <menu:folder label="North-East Atlantic TEP">
         <menu:dataset dataset="${datasets.MERSEA_NATL}"/>
     </menu:folder>
     <menu:folder label="Mediterranean TEP">
         <%-- The Mediterranean data come from three different datasets so we
              manually aggregate them in a single folder --%>
          <menu:folder label="Mediterranean ocean analyses/forecasts">
              <menu:layer id="MERSEA_MED_T/temperature" label="sea_water_potential_temperature"/>
              <menu:layer id="MERSEA_MED_T/salinity" label="sea_water_salinity"/>
              <menu:layer id="MERSEA_MED_T/ssh" label="sea_surface_height_above_sea_level"/>
              <menu:layer id="MERSEA_MED_T/mld" label="ocean_mixed_layer_thickness"/>
              <menu:layer id="MERSEA_MED_U/u" label="eastward_sea_water_velocity"/>
              <menu:layer id="MERSEA_MED_V/v" label="northward_sea_water_velocity"/>
          </menu:folder>
     </menu:folder>
 </menu:folder>