<%@page contentType="application/json"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="/WEB-INF/taglib/MenuMaker" prefix="menu"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
response.setHeader("Pragma","no-cache"); //HTTP 1.0
response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
%>
<%--
     Displays the hierarchy of layers from this server as a JSON object
     See MetadataController.showLayerHierarchy().
     
     Data (models) passed in to this page:
         serverTitle = String: title for this server
         datasets = Map<String, Dataset>: all the datasets in this server
         serverInfo  = uk.ac.rdg.resc.ncwms.config.Server object
--%>
<menu:folder label="${serverTitle}">

     <c:set var="dataset" value="${datasets}" />

     <menu:folder label="NCOF Products">
         <menu:dataset dataset="${datasets.MERSEA_NATL}"/>
 	 <menu:dataset dataset="${datasets.NCOF_FOAM_ONE}"/>
 	 <menu:dataset dataset="${datasets.NCOF_AMM}"/>
         <menu:dataset dataset="${datasets.NCOF_IRISH}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
	 <menu:dataset dataset="${datasets.NCOF_WAVES}"/>
     </menu:folder>

     <menu:folder label="EU-MERSEA">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
         <menu:dataset dataset="${datasets.MERSEA_NATL}"/>
         <menu:dataset dataset="${datasets.MERSEA_GLOBAL}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_V}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_U}"/>
         <menu:dataset dataset="${datasets.MERSEA_MED_T}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
     </menu:folder>

     <menu:folder label="EU-ECOOP">
         <menu:dataset dataset="${datasets.BALTIC_BEST_EST}"/>
         <menu:dataset dataset="${datasets.BALTIC_FORECAST}"/>
         <menu:dataset dataset="${datasets.ECOOP_CYCO}"/>
         <menu:dataset dataset="${datasets.ECOOP_ROMS_TEST}"/>
         <menu:dataset dataset="${datasets.NCOF_MRCS}"/>
         <menu:dataset dataset="${datasets.ECOOP_TEST_CYPRUS}"/>
    </menu:folder>

     <menu:folder label="Ocean Syntheses">
         <menu:dataset dataset="${datasets.CLIVAR_NASA_JPL_ECCO}"/>
         <menu:dataset dataset="${datasets.CLIVAR_SODA_POP}"/>
         <menu:folder label="NEMO 1 degree global model control run">
             <menu:dataset dataset="${datasets.ORCA1_R70_MONTHLY}" label="Monthly means"/>
             <menu:dataset dataset="${datasets.ORCA1_R70_SEASONAL}" label="Seasonal means"/>
             <menu:dataset dataset="${datasets.ORCA1_R70_ANNUAL}" label="Annual means"/>
         </menu:folder>
         <menu:folder label="NEMO 1/4 degree global model with T level assimilation">
             <menu:dataset dataset="${datasets.ORCA025_R07_Exp4_ANNUAL}" label="Annual means"/>
         </menu:folder>
     </menu:folder>

     <menu:folder label="Observations">
         <menu:dataset dataset="${datasets.MERSEA_CORIOLIS_SAL}"/>
         <menu:dataset dataset="${datasets.MERSEA_CORIOLIS_TEMP}"/>
         <menu:dataset dataset="${datasets.MERSEA_CNR_SST}"/>
         <menu:dataset dataset="${datasets.OSTIA_OLD}"/>
         <menu:dataset dataset="${datasets.OSTIA}"/>
         <menu:dataset dataset="${datasets.MERSEA_NRTSLA}"/>
         <menu:dataset dataset="${datasets.TEST_OZONE}"/>
     </menu:folder>

     <menu:folder label="Other">
         <menu:folder label="DRAKKAR">
             <menu:dataset dataset="${datasets.ORCA025_G70_ANNUAL}" label="Annual means"/>
             <menu:dataset dataset="${datasets.ORCA025_G70_MONTHLY}" label="Monthly means"/>
         </menu:folder>
         <menu:dataset dataset="${datasets.GENIE}"/>
         <menu:dataset dataset="${datasets.HadCEM}"/>
         <menu:dataset dataset="${datasets.USGS_ADRIATIC_SED038}"/>
         <menu:folder label="MarQuest">
         	<menu:dataset dataset="${datasets.SEAW4}"/>
               	<menu:dataset dataset="${datasets.NEMO_BIO2_ASSIMILATION_GRIDT}"/>
        	<menu:dataset dataset="${datasets.NEMO_BIO2_ASSIMILATION_A_GRID}"/>  
                <menu:dataset dataset="${datasets.NEMO_BIO_FULL_CONT_PHYSICS}"/>
                <menu:dataset dataset="${datasets.NEMO_BIO_FULL_CONT_BIO}"/>
                <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_PHYSICS}"/>
                <menu:dataset dataset="${datasets.NEMO_BIO_FULL_ASSIM_BIO}"/>
 	   </menu:folder>
      </menu:folder>

</menu:folder>
