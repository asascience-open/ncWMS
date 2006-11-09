/*
 * Copyright (c) 2006 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.List;
import java.util.Vector;
import ucar.ma2.Range;

/**
 * Stores the metadata for a {@link GeoGrid}: saves reading in the metadata every
 * time the dataset is opened (a significant performance hit especially for
 * large NcML aggregations.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class VariableMetadata
{
    private String id;
    private String title;
    private String abstr; // "abstract" is a reserved word
    private String units;
    private String zUnits;
    private double[] zValues;
    private boolean zPositive;
    private double[] tValues; // Seconds since the epoch
    private double[] bbox; // Bounding box : minx, miny, maxx, maxy
    private double validMin;
    private double validMax;
    private EnhancedCoordAxis xaxis;
    private EnhancedCoordAxis yaxis;
    private int tPos, zPos, yPos, xPos;
    
    /** Creates a new instance of VariableMetadata */
    VariableMetadata()
    {
        this.title = null;
        this.abstr = null;
        this.zUnits = null;
        this.zValues = null;
        this.tValues = null;
        this.bbox = new double[]{-180.0, -90.0, 180.0, 90.0};
        this.xaxis = null;
        this.yaxis = null;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAbstract()
    {
        return abstr;
    }

    public void setAbstract(String abstr)
    {
        this.abstr = abstr;
    }

    public String getZunits()
    {
        return zUnits;
    }

    public void setZunits(String zUnits)
    {
        this.zUnits = zUnits;
    }

    public double[] getZvalues()
    {
        return zValues;
    }

    public void setZvalues(double[] zValues)
    {
        this.zValues = zValues;
    }

    public double[] getTvalues()
    {
        return tValues;
    }

    public void setTvalues(double[] tValues)
    {
        this.tValues = tValues;
    }

    public double[] getBbox()
    {
        return bbox;
    }

    public void setBbox(double[] bbox)
    {
        this.bbox = bbox;
    }

    public boolean getZpositive()
    {
        return zPositive;
    }

    public void setZpositive(boolean zPositive)
    {
        this.zPositive = zPositive;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public double getValidMin()
    {
        return validMin;
    }

    public void setValidMin(double validMin)
    {
        this.validMin = validMin;
    }

    public double getValidMax()
    {
        return validMax;
    }

    public void setValidMax(double validMax)
    {
        this.validMax = validMax;
    }

    public String getUnits()
    {
        return units;
    }

    public void setUnits(String units)
    {
        this.units = units;
    }

    public EnhancedCoordAxis getXaxis()
    {
        return xaxis;
    }

    public void setXaxis(EnhancedCoordAxis xaxis)
    {
        this.xaxis = xaxis;
    }

    public EnhancedCoordAxis getYaxis()
    {
        return yaxis;
    }

    public void setYaxis(EnhancedCoordAxis yaxis)
    {
        this.yaxis = yaxis;
    }
    
    public void setAxisPositions(int tPos, int zPos, int yPos, int xPos)
    {
        this.tPos = tPos;
        this.zPos = zPos;
        this.yPos = yPos;
        this.xPos = xPos;
    }
    
    public List getRangesList(Range tRange, Range zRange, Range yRange, Range xRange)
    {
        Vector<Range> ranges = new Vector<Range>();
        for (int i = 0; i < 4; i++)
        {
            if (i == this.tPos)
            {
                ranges.add(tRange);
            }
            else if (i == this.zPos)
            {
                ranges.add(zRange);
            }
            else if (i == this.yPos)
            {
                ranges.add(yRange);
            }
            else if (i == this.xPos)
            {
                ranges.add(xRange);
            }
        }
        return ranges;
    }
    
}
