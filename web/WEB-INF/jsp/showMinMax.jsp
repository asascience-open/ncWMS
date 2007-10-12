<%@page contentType="application/json"%>
<%-- Displays the min and max values of data on a certain image
     
     Data (models) passed in to this page:
          minMax = Array of 2 floats with min and max values --%>
[${minMax[0]},${minMax[1]}]