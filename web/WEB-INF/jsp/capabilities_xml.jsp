<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%> <%-- tag library for useful utility functions --%>
<%-- Displays the Capabilities document in XML.
     Data (models) passed in to this page:
         config     = Configuration of this server (uk.ac.rdg.resc.ncwms.config.Config)
         wmsBaseUrl = Base URL of this server (java.lang.String)
         supportedImageFormats = Set of Strings representing MIME types of supported image formats
         layerLimit = Maximum number of layers that can be requested simultaneously from this server (int)
     --%>
<WMS_Capabilities
        version="${utils:wmsVersion()}"
        <%-- TODO: do UpdateSequence properly --%>
        xmlns="http://www.opengis.net/wms"
        xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd">
        
    <Service>
        <Name>WMS</Name>
        <Title>${config.server.title}</Title>
        <Abstract>${config.server.abstract}</Abstract>
        <KeywordList>
            <%-- forEach recognizes that keywords is a comma-delimited String --%>
            <c:forEach var="keyword" items="${config.server.keywords}">
            <Keyword>${keyword}</Keyword>
            </c:forEach>
        </KeywordList>
        <OnlineResource xlink:type="simple" xlink:href="${config.server.url}"/>
        <ContactInformation>
            <ContactPersonPrimary>
                <ContactPerson>${config.contact.name}</ContactPerson>
                <ContactOrganization>${config.contact.org}</ContactOrganization>
            </ContactPersonPrimary>
            <ContactVoiceTelephone>${config.contact.tel}</ContactVoiceTelephone>
            <ContactElectronicMailAddress>${config.contact.email}</ContactElectronicMailAddress>
        </ContactInformation>
        <Fees>none</Fees>
        <AccessConstraints>none</AccessConstraints>
        <LayerLimit>${layerLimit}</LayerLimit>
        <MaxWidth>${config.server.maxImageWidth}</MaxWidth>
        <MaxHeight>${config.server.maxImageHeight}</MaxHeight>
    </Service>
    
    <Capability>
        <Request>
            <GetCapabilities>
                <Format>text/xml</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}"/>
                        </Get>
                    </HTTP>
                </DCPType>
            </GetCapabilities>
            <GetMap>
                <c:forEach var="mimeType" items="${supportedImageFormats}">
                <Format>${mimeType}</Format>
                </c:forEach>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}"/>
                        </Get>
                    </HTTP>
                </DCPType>
            </GetMap>
            <c:if test="${config.server.allowFeatureInfo}">
            <GetFeatureInfo>
                <Format>text/xml</Format> <%-- TODO: get these formats properly --%>
                <Format>image/png</Format>
                <DCPType>
                    <HTTP>
                        <Get>
                            <OnlineResource xlink:type="simple" xlink:href="${wmsBaseUrl}"/>
                        </Get>
                    </HTTP>
                </DCPType>
            </GetFeatureInfo>
            </c:if>
        </Request>
        <Exception>
            <Format>XML</Format>
        </Exception>
        <Layer>
            <Title>${config.server.title}</Title>
            <CRS>CRS:84</CRS> <%-- TODO: do this properly --%>
            <c:forEach var="dataset" items="${config.datasets}">
            <c:if test="${dataset.value.ready}">
            <Layer>
                <Title>${dataset.value.title}</Title>
                <c:forEach var="layer" items="${dataset.value.layers}">
                <Layer<c:if test="${config.server.allowFeatureInfo} and ${layer.queryable}"> queryable="1"</c:if>>
                    <Name>${layer.layerName}</Name>
                    <Title>${layer.title}</Title>
                    <Abstract>${layer.abstract}</Abstract>
                    <c:set var="bbox" value="${layer.bbox}"/>
                    <EX_GeographicBoundingBox>
                        <westBoundLongitude>${bbox[0]}</westBoundLongitude>
                        <eastBoundLongitude>${bbox[2]}</eastBoundLongitude>
                        <southBoundLatitude>${bbox[1]}</southBoundLatitude>
                        <northBoundLatitude>${bbox[3]}</northBoundLatitude>
                    </EX_GeographicBoundingBox>
                    <BoundingBox CRS="CRS:84" minx="${bbox[0]}" maxx="${bbox[2]}" miny="${bbox[1]}" maxy="${bbox[3]}"/>
                    <c:if test="${layer.zaxisPresent}">
                    <Dimension name="elevation" units="${layer.zunits}" default="${layer.defaultZValue}">
                        <%-- Print out the dimension values, comma separated, making sure
                             that there is no comma at the start or end of the list.  Note that
                             we can't use ${fn:join} because the z values are an array of doubles,
                             not strings. --%>
                        <c:forEach var="zval" items="${layer.zvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${zval}</c:forEach>
                    </Dimension>
                    </c:if>
                    <c:set var="tvalues" value="${layer.tvalues}"/>
                    <c:if test="${layer.taxisPresent}">
                    <Dimension name="time" units="ISO8601" multipleValues="true" current="true" default="${utils:millisecondsToISO8601(layer.defaultTValue)}">
                        <c:forEach var="tval" items="${tvalues}" varStatus="status"><c:if test="${status.index > 0}">,</c:if>${utils:millisecondsToISO8601(tval)}</c:forEach>
                    </Dimension>
                    </c:if>
                    <c:forEach var="style" items="${layer.supportedStyleKeys}">
                    <Style><Name>${style}</Name><Title>${style}</Title></Style>
                    </c:forEach>
                </Layer>
                </c:forEach> <%-- End loop through variables --%>
            </Layer>
            </c:if> <%-- End if dataset is ready for display --%>
            </c:forEach> <%-- End loop through datasets --%>
        </Layer>
    </Capability>
    
</WMS_Capabilities>