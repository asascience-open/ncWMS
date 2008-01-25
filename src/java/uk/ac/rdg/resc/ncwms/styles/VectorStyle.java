/*
 * Copyright (c) 2007 The University of Reading
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

package uk.ac.rdg.resc.ncwms.styles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.List;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;

/**
 * Style for plotting vector data as arrows
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class VectorStyle extends BoxFillStyle
{
    private static final Logger logger = Logger.getLogger(VectorStyle.class);
    
    /**
     * Defines the names of styles that this class supports: see Factory.setClasses()
     */
    public static final String[] KEYS = new String[]{"vector"};
    
    /**
     * The length of arrows in pixels
     */
    private float arrowLength = 10.0f;
    
    /**
     * Creates a new instance of VectorStyle
     */
    public VectorStyle()
    {
        super();
        this.name = KEYS[0];
    }

    public void setAttribute(String attName, String[] values) throws StyleNotDefinedException
    {
        if (attName.trim().equalsIgnoreCase("arrowLength"))
        {
            if (values.length != 1)
            {
                throw new StyleNotDefinedException("Format error for \"arrowLength\" attribute of "
                    + this.name + " style");
            }
            try
            {
                this.arrowLength = Float.parseFloat(values[0]);
            }
            catch (NumberFormatException nfe)
            {
                throw new StyleNotDefinedException("Format error for \"arrowLength\" attribute of "
                    + this.name + " style");
            }
        }
        else
        {
            // Give the BoxFillStyle class a chance to handle this attribute
            super.setAttribute(attName, values);
        }
    }

    protected BufferedImage createImage(List<float[]> data, String label)
    {
        if (data.size() != 2)
        {
            // Shouldn't happen: defensive programming
            throw new IllegalStateException("A vector style is only appropriate "
                + "for fields with two components");
        }
        
        // We're going to create a boxfill image to use as a background
        BufferedImage image = super.createImage(data, label);
        
        Graphics2D g = image.createGraphics();
        // TODO: control the colour of the arrows with an attribute
        // Must be part of the colour palette (here we use the colour
        // for out-of-range values
        g.setColor(Color.BLACK);
        
        logger.debug("Drawing vectors, length = {} pixels", this.arrowLength);
        float[] comp1 = data.get(0);
        float[] comp2 = data.get(1);
        for (int i = 0; i < this.picWidth; i += Math.ceil(this.arrowLength * 1.2))
        {
            for (int j = 0; j < this.picHeight; j += Math.ceil(this.arrowLength * 1.2))
            {
                int dataIndex = j * this.picWidth + i;
                if (!Float.isNaN(comp1[dataIndex]) && !Float.isNaN(comp2[dataIndex]))
                {
                    double angle = Math.atan2(comp2[dataIndex], comp1[dataIndex]);
                    // Calculate the end point of the arrow
                    double iEnd = i + this.arrowLength * Math.cos(angle);
                    // Screen coordinates go down, but north is up, hence the minus sign
                    double jEnd = j - this.arrowLength * Math.sin(angle);
                    //logger.debug("i={}, j={}, dataIndex={}, east={}, north={}",
                    //    new Object[]{i, j, dataIndex, data[0][dataIndex], data[1][dataIndex]});
                    // Draw a dot representing the data location
                    g.fillOval(i - 2, j - 2, 4, 4);
                    // Draw a line representing the vector direction and magnitude
                    g.setStroke(new BasicStroke(1));
                    g.drawLine(i, j, (int)Math.round(iEnd), (int)Math.round(jEnd));
                    // Draw the arrow on the canvas
                    //drawArrow(g, i, j, (int)Math.round(iEnd), (int)Math.round(jEnd), 2);
                }
            }
        }
        
        return image;
    }
    
    // http://forum.java.sun.com/thread.jspa?threadID=378460&tstart=135
    private static void drawArrow(Graphics2D g2d, int xCentre, int yCentre, int x, int y, float stroke)
    {
        double aDir = Math.atan2(xCentre - x, yCentre - y);
        g2d.setStroke(new BasicStroke(stroke));
        g2d.drawLine(x, y, xCentre, yCentre);
        g2d.setStroke(new BasicStroke(1.0f));
        Polygon tmpPoly = new Polygon();
        int i1 = 12 + (int)(stroke * 2);
        int i2 = 6 + (int)stroke;
        tmpPoly.addPoint(x, y);
        tmpPoly.addPoint(x + xCor(i1, aDir + 0.5), y + yCor(i1, aDir + 0.5));
        tmpPoly.addPoint(x + xCor(i2, aDir), y + yCor(i2, aDir));
        tmpPoly.addPoint(x + xCor(i1, aDir - 0.5), y + yCor(i1, aDir - 0.5));
        tmpPoly.addPoint(x, y);
        g2d.drawPolygon(tmpPoly);
        g2d.fillPolygon(tmpPoly);
    }
    
    private static int yCor(int len, double dir)
    {
        return (int)(len * Math.cos(dir));
    }
    
    private static int xCor(int len, double dir)
    {
        return (int)(len * Math.sin(dir));
    }
    
}
