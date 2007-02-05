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
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

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
    // Scale range of the picture
    private float scaleMin;
    private float scaleMax;
    // The percentage opacity of the picture
    private int opacity;
    
    /** Creates a new instance of BoxFillStyle */
    public BoxFillStyle()
    {
        super("boxfill");
        this.opacity = 100;
        this.scaleMin = 0.0f;
        this.scaleMax = 0.0f;
    }

    /**
     * Sets attributes that are particular to this style.  Valid attributes for
     * this style are "opacity" and "scale".
     * @todo error handling and reporting
     */
    public void setAttribute(String attName, String value)
    {
        if (attName.trim().equalsIgnoreCase("opacity"))
        {
            int opVal = Integer.parseInt(value);
            if (opacity < 0 || opacity > 100)
            {
                throw new IllegalArgumentException("Opacity must be in the range 0 to 100");
            }
            this.opacity = opVal;
        }
        else if (attName.trim().equalsIgnoreCase("scale"))
        {
            String[] scVals = value.trim().split(":");
            this.scaleMin = Float.parseFloat(scVals[0]);
            this.scaleMax = Float.parseFloat(scVals[1]);
        }
        else
        {
            // TODO: do something here
        }
    }
    
    /**
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set.  The scale must be set before
     * calling this method.
     */
    public void createImage(float[] data, String label)
    {
        // Create the pixel array for the frame
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = getColourIndex(data[i]);
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

    public ArrayList<BufferedImage> getRenderedFrames()
    {
        return this.renderedFrames;
    }
    
    /**
     * @return true if this image will have its colour range scaled automatically.
     * This is true if scaleMin and scaleMax are both zero
     */
    private boolean isAutoScale()
    {
        return (this.scaleMin == 0.0f && this.scaleMax == 0.0f);
    }
    
    /**
     * Adjusts the colour scale to accommodate the given frame.
     */
    private void adjustColourScaleForFrame(float[] data)
    {
        this.scaleMin = Float.MAX_VALUE;
        this.scaleMax = -Float.MAX_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] != this.fillValue)
            {
                if (data[i] < this.scaleMin)
                {
                    this.scaleMin = data[i];
                }
                if (data[i] > this.scaleMax)
                {
                    this.scaleMax = data[i];
                }
            }
        }
    }
    
    /**
     * @return the colour index that corresponds to the given value
     */
    private byte getColourIndex(float value)
    {
        if (value == this.fillValue)
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
    private IndexColorModel getRainbowColorModel()
    {
        byte[] r = new byte[256];   byte[] g = new byte[256];
        byte[] b = new byte[256];   byte[] a = new byte[256];
        
        // Set the alpha value based on the percentage transparency
        byte alpha;
        // Here we are playing safe and avoiding rounding errors that might
        // cause the alpha to be set to zero instead of 255
        if (this.opacity >= 100)
        {
            alpha = (byte)255;
        }
        else if (this.opacity <= 0)
        {
            alpha = 0;
        }
        else
        {
            alpha = (byte)(2.55 * this.opacity);
        }
        
        if (this.transparent)
        {
            // Colour with index 0 is fully transparent
            r[0] = 0;   g[0] = 0;   b[0] = 0;   a[0] = 0;
        }
        else
        {
            // Use the supplied background color
            Color bg = new Color(this.bgColor);
            r[0] = (byte)bg.getRed();
            g[0] = (byte)bg.getGreen();
            b[0] = (byte)bg.getBlue();
            a[0] = alpha;
        }
        // Colour with index 1 is black (represents out-of-range data)
        r[1] = 0;   g[1] = 0;   b[1] = 0;   a[1] = alpha;
        
        int[] red =
        {  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  7, 23, 39, 55, 71, 87,103,
           119,135,151,167,183,199,215,231,
           247,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,246,228,211,193,175,158,140};
        int[] green =
        {  0,  0,  0,  0,  0,  0,  0,
           0, 11, 27, 43, 59, 75, 91,107,
           123,139,155,171,187,203,219,235,
           251,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,247,231,215,199,183,167,151,
           135,119,103, 87, 71, 55, 39, 23,
           7,  0,  0,  0,  0,  0,  0,  0};
        int[] blue =
        {  143,159,175,191,207,223,239,
           255,255,255,255,255,255,255,255,
           255,255,255,255,255,255,255,255,
           255,247,231,215,199,183,167,151,
           135,119,103, 87, 71, 55, 39, 23,
           7,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0,
           0,  0,  0,  0,  0,  0,  0,  0};
        
        for (int i = 2; i < 256; i++)
        {
            a[i] = alpha;
            // There are 63 colours and 254 remaining slots
            float index = (i - 2) * (62.0f / 253.0f);
            if (i == 255)
            {
                r[i] = (byte)red[62];
                g[i] = (byte)green[62];
                b[i] = (byte)blue[62];
            }
            else
            {
                // We merge the colours from adjacent indices
                float fromUpper = index - (int)index;
                float fromLower = 1.0f - fromUpper;
                r[i] = (byte)(fromLower * red[(int)index] + fromUpper * red[(int)index + 1]);
                g[i] = (byte)(fromLower * green[(int)index] + fromUpper * green[(int)index + 1]);
                b[i] = (byte)(fromLower * blue[(int)index] + fromUpper * blue[(int)index + 1]);
            }
        }
        
        return new IndexColorModel(8, 256, r, g, b, a);
    }
    
}
