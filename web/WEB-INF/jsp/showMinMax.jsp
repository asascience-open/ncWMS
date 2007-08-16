<%@include file="xml_header.jsp"%>
<%-- Displays the min and max values of data on a certain image
     
     Data (models) passed in to this page:
          minMax = Array of 2 floats with min and max values --%>
<minmax>
    <min>${minMax[0]}</min>
    <max>${minMax[1]}</max>
</minmax>