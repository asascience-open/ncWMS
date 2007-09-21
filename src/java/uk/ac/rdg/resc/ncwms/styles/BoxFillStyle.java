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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.text.DecimalFormat;
import java.util.List;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * Renders an image using a boxfill style (i.e. solid regions of colour)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class BoxFillStyle extends AbstractStyle
{
    private static final Logger logger = Logger.getLogger(BoxFillStyle.class);
    
    /**
     * Defines the names of styles that this class supports: see Factory.setClasses()
     */
    public static final String[] KEYS = new String[]{"boxfill"};
    
    // Scale range of the picture
    private float scaleMin;
    private float scaleMax;
    // The percentage opacity of the picture
    private int opacity;
    
    // Elements of the default palette
    private static final int[] RED =
        {  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  7, 23, 39, 55, 71, 87,103,
           119,135,151,167,183,199,215,231,
           247,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,246,228,211,193,175,158,140};
    private static final int[] GREEN =
        {  0,  0,  0,  0,  0,  0,  0,
           0, 11, 27, 43, 59, 75, 91,107,
           123,139,155,171,187,203,219,235,
           251,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,247,231,215,199,183,167,151,
           135,119,103, 87, 71, 55, 39, 23,
           7,  0,  0,  0,  0,  0,  0,  0};
    private static final int[] BLUE =
        {  143,159,175,191,207,223,239,
           255,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,247,231,215,199,183,167,151,
           135,119,103, 87, 71, 55, 39, 23,
           7,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0};
    
    /** Creates a new instance of BoxFillStyle */
    public BoxFillStyle()
    {
        super(KEYS[0]);
        this.opacity = 100;
        this.scaleMin = 0.0f;
        this.scaleMax = 0.0f;
    }

    /**
     * Sets attributes that are particular to this style.  Valid attributes for
     * this style are "opacity" and "scale".
     * @todo error handling and reporting
     */
    public void setAttribute(String attName, String[] values)
        throws StyleNotDefinedException
    {
        if (attName.trim().equalsIgnoreCase("opacity"))
        {
            if (values.length != 1)
            {
                throw new StyleNotDefinedException("Format error for OPACITY attribute of BOXFILL style");
            }
            int opVal = Integer.parseInt(values[0]); // TODO: trap number format errors
            if (opVal < 0 || opVal > 100)
            {
                throw new IllegalArgumentException("Opacity must be in the range 0 to 100");
            }
            this.opacity = opVal;
            logger.debug("Set OPACITY to {}", this.opacity);
        }
        else if (attName.trim().equalsIgnoreCase("scale"))
        {
            if (values.length != 2)
            {
                throw new StyleNotDefinedException("Format error for SCALE attribute of BOXFILL style");
            }
            this.scaleMin = Float.parseFloat(values[0]); // TODO: trap number format errors
            this.scaleMax = Float.parseFloat(values[1]);
            logger.debug("Set SCALE to {},{}", this.scaleMin, this.scaleMax);
        }
        // TODO: set the palette ("rainbow", etc)
        else
        {
            throw new StyleNotDefinedException("Attribute " + attName + 
                " is not supported by the " + this.name + " style");
        }
    }
    
    /**
     * Calculates the magnitude of the data in-place, replacing data.get(0) with the
     * magnitude of the data.
     */
    public void processData(List<float[]> data)
    {
        logger.debug("Calculating the magnitude of {} components", data.size());
        if (data.size() == 1)
        {
            return;
        }
        float[] firstComponent = data.get(0);
        for (int i = 0; i < firstComponent.length; i++)
        {
            if (!Float.isNaN(firstComponent[i]))
            {
                double sumsq = firstComponent[i] * firstComponent[i];
                for (int j = 1; j < data.size(); j++)
                {
                    sumsq += data.get(j)[i] * data.get(j)[i];
                }
                firstComponent[i] = (float)Math.sqrt(sumsq);
            }
        }
    }
    
    /**
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set.  The scale must be set before
     * calling this method.
     */
    public void createImage(List<float[]> data, String label)
    {
        // Create the pixel array for the frame
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        // We only use the first of the data arrays: this is a Style for scalars
        float[] firstArray = data.get(0);
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = getColourIndex(firstArray[i]);
        }
        
        // Create the Image
        DataBuffer buf = new DataBufferByte(pixels, pixels.length);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_BYTE, this.picWidth, this.picHeight, new int[]{0xff});
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        IndexColorModel colorModel = getRainbowColorModel();
        BufferedImage image = new BufferedImage(colorModel, raster, false, null); 
        
        // Add the label to the image
        if (label != null && !label.equals(""))
        {
            Graphics2D gfx = (Graphics2D)image.getGraphics();
            gfx.setPaint(new Color(0, 0, 143));
            gfx.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
            gfx.setPaint(new Color(255, 151, 0));
            gfx.drawString(label, 10, image.getHeight() - 5);
        }
        
        this.renderedFrames.add(image);
    }
    
    /**
     * @return true if this image will have its colour range scaled automatically.
     * This is true if scaleMin and scaleMax are both zero
     */
    protected boolean isAutoScale()
    {
        return (this.scaleMin == 0.0f && this.scaleMax == 0.0f);
    }
    
    /**
     * Adjusts the colour scale to accommodate the given frame.
     */
    protected void adjustScaleForFrame(List<float[]> data)
    {
        // We only use the first data array: this is a Style for scalars
        this.scaleMin = Float.MAX_VALUE;
        this.scaleMax = -Float.MAX_VALUE;
        for (float val : data.get(0))
        {
            if (!Float.isNaN(val))
            {
                if (val < this.scaleMin) this.scaleMin = val;
                if (val > this.scaleMax) this.scaleMax = val;
            }
        }
    }
    
    /**
     * @return the colour index that corresponds to the given value
     */
    private byte getColourIndex(float value)
    {
        if (Float.isNaN(value))
        {
            return 0; // represents a transparent pixel
        }
        else if (value < this.scaleMin || value > this.scaleMax)
        {
            return 1; // represents an out-of-range pixel
        }
        else
        {
            return (byte)(((253.0f / (this.scaleMax - this.scaleMin)) * (value - this.scaleMin)) + 2);
        }
    }
    
    /**
     * @return a rainbow colour map for this PicMaker's opacity and transparency
     * @todo To avoid multiplicity of objects, could statically create color models
     * for opacity=100 and transparency=true/false.
     */
    protected IndexColorModel getRainbowColorModel()
    {
        // Get 256 colors representing this palette
        Color[] colors = this.getColorPalette();
        
        // Extract to arrays of r, g, b, a
        byte[] r = new byte[colors.length];
        byte[] g = new byte[colors.length];
        byte[] b = new byte[colors.length];
        byte[] a = new byte[colors.length];        
        for (int i = 0; i < colors.length; i++)
        {
            r[i] = (byte)colors[i].getRed();
            g[i] = (byte)colors[i].getGreen();
            b[i] = (byte)colors[i].getBlue();
            a[i] = (byte)colors[i].getAlpha();
        }
        
        return new IndexColorModel(8, 256, r, g, b, a);
    }
    
    /**
     * @return Array of 256 RGBA Color objects representing the palette.
     */
    protected Color[] getColorPalette()
    {
        Color[] colors = new Color[256];
        
        // Set the alpha value based on the percentage transparency
        int alpha;
        // Here we are playing safe and avoiding rounding errors that might
        // cause the alpha to be set to zero instead of 255
        if (this.opacity >= 100)
        {
            alpha = 255;
        }
        else if (this.opacity <= 0)
        {
            alpha = 0;
        }
        else
        {
            alpha = (int)(2.55 * this.opacity);
        }
        
        // Use the supplied background color or set transparent
        Color bg = new Color(this.bgColor);
        colors[0] = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(),
            this.transparent ? 0 : alpha);
        
        // Colour with index 1 is black (represents out-of-range data)
        colors[1] = new Color(0, 0, 0, alpha);
        
        for (int i = 2; i < colors.length; i++)
        {
            // There are 63 colours and 254 remaining slots
            float index = (i - 2) * (62.0f / 253.0f);
            if (i == colors.length - 1)
            {
                colors[i] = new Color(RED[62], GREEN[62], BLUE[62]);
            }
            else
            {
                // We merge the colours from adjacent indices
                float fromUpper = index - (int)index;
                float fromLower = 1.0f - fromUpper;
                int r = (int)(fromLower * RED[(int)index] + fromUpper * RED[(int)index + 1]);
                int g = (int)(fromLower * GREEN[(int)index] + fromUpper * GREEN[(int)index + 1]);
                int b = (int)(fromLower * BLUE[(int)index] + fromUpper * BLUE[(int)index + 1]);
                colors[i] = new Color(r, g, b, alpha);
            }
        }
        
        return colors;
    }
    
    /**
     * Creates and returns a BufferedImage representing the legend for this 
     * Style instance
     * @param layer The Layer object for which this legend is being 
     * created (needed for title and units strings)
     */
    public BufferedImage createLegend(Layer layer)
    {
        BufferedImage colourScale = new BufferedImage(110, 264, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gfx = colourScale.createGraphics();
        
        // Create the colour scale itself
        Color[] palette = this.getColorPalette();
        for (int i = 5; i < 259; i++)
        {
            gfx.setColor(palette[260 - i]);
            gfx.drawLine(2, i, 25, i);
        }
        
        // Draw the text items
        gfx.setColor(Color.WHITE);
        // Add the scale values
        double quarter = 0.25 * (this.scaleMax - this.scaleMin);
        String scaleMin          = format(this.scaleMin);
        String scaleQuarter      = format(this.scaleMin + quarter);
        String scaleMid          = format(this.scaleMin + 2 * quarter);
        String scaleThreeQuarter = format(this.scaleMin + 3 * quarter);
        String scaleMax          = format(this.scaleMax);        
        gfx.drawString(scaleMax, 27, 10);
        gfx.drawString(scaleThreeQuarter, 27, 73);
        gfx.drawString(scaleMid, 27, 137);
        gfx.drawString(scaleQuarter, 27, 201);
        gfx.drawString(scaleMin, 27, 264);
        
        // Add the title as rotated text        
        AffineTransform trans = new AffineTransform();
        trans.setToTranslation(90, 0);
        AffineTransform rot = new AffineTransform();
        rot.setToRotation(Math.PI / 2.0);
        trans.concatenate(rot);
        gfx.setTransform(trans);
        String title = layer.getTitle();
        if (layer.getUnits() != null)
        {
            title += " (" + layer.getUnits() + ")";
        }
        gfx.drawString(title, 5, 0);
        
        return colourScale;
    }
    
    /**
     * Formats a number to a limited number of d.p., using scientific notation
     * if necessary
     */
    private static String format(double d)
    {
        if (Math.abs(d) > 1000 || Math.abs(d) < 0.01)
        {
            return new DecimalFormat("0.###E0").format(d);
        }
        else
        {
            return new DecimalFormat("0.#####").format(d);
        }
    }
}
