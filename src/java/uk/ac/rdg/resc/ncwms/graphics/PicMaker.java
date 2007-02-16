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

package uk.ac.rdg.resc.ncwms.graphics;

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
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * Abstract superclass of picture makers.  Subclasses must have a no-argument
 * constructor
 * 
 * Makes a picture from an array of raw data, using a rainbow colour model.
 * Fill values are represented as transparent pixels and out-of-range values
 * are represented as black pixels.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class PicMaker
{
    private static final Logger logger = Logger.getLogger(PicMaker.class);
    /**
     * Maps MIME types to the required class of PicMaker
     */
    private static Hashtable<String, Class> picMakers;
    
    // Image MIME type
    protected String mimeType;
    // Width and height of the resulting picture
    private int picWidth;
    private int picHeight;
    // Scale range of the picture
    private float scaleMin;
    private float scaleMax;
    // The percentage opacity of the picture
    private int opacity;
    // The fill value of the data.
    private float fillValue;
    protected boolean transparent;
    private int bgColor; // Background colour as an integer
    protected VariableMetadata var;
    
    static
    {
        picMakers = new Hashtable<String, Class>();
        picMakers.put("image/png", SimplePicMaker.class);
        picMakers.put("image/gif", GifMaker.class);
        picMakers.put("application/vnd.google-earth.kmz", KmzMaker.class);
    }
    
    /**
     * @return the image formats (MIME types) that can be produced as a
     * Set of Strings.
     */
    public static final Set<String> getSupportedImageFormats()
    {
        return picMakers.keySet();
    }
    
    /**
     * Initializes fields to default values
     */
    protected PicMaker()
    {
        this.opacity = 100;
        this.scaleMin = 0.0f;
        this.scaleMax = 0.0f;
    }
    
    /**
     * Creates a PicMaker object for the given mime type.  Creates a new PicMaker
     * object with each call.
     * @param mimeType The MIME type of the image that is required
     * @return A PicMaker object
     * @throws a {@link InvalidFormatException} if there isn't a PicMaker for
     * the given MIME type
     * @throws an {@link WMSExceptionInJava} if the PicMaker could not be created
     */
    public static PicMaker createPicMaker(String mimeType)
        throws InvalidFormatException, WMSExceptionInJava
    {
        Class clazz = picMakers.get(mimeType.trim());
        if (clazz == null)
        {
            throw new InvalidFormatException(mimeType);
        }
        try
        {
            PicMaker pm = (PicMaker)clazz.newInstance();
            // Some PicMakers support multiple MIME types
            pm.mimeType = mimeType;
            return pm;
        }
        catch (InstantiationException ie)
        {
            throw new WMSExceptionInJava("Internal error: could not create PicMaker "
                + "of type " + clazz.getName());
        }
        catch (IllegalAccessException iae)
        {
            throw new WMSExceptionInJava("Internal error: IllegalAccessException" +
                " when creating PicMaker of type " + clazz.getName());
        }
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public int getPicWidth()
    {
        return picWidth;
    }

    /**
     * @param width The width of the picture in pixels
     */
    public void setPicWidth(int picWidth)
    {
        this.picWidth = picWidth;
    }

    public int getPicHeight()
    {
        return picHeight;
    }

    /**
     * @param height The height of the picture in pixels
     */
    public void setPicHeight(int picHeight)
    {
        this.picHeight = picHeight;
    }

    public float getScaleMin()
    {
        return scaleMin;
    }

    /**
     * @param scaleMin The minimum value for the scale
     */
    public void setScaleMin(float scaleMin)
    {
        this.scaleMin = scaleMin;
    }

    public float getScaleMax()
    {
        return scaleMax;
    }

    /**
     * @param scaleMax The maximum value for the scale
     */
    public void setScaleMax(float scaleMax)
    {
        this.scaleMax = scaleMax;
    }

    public int getOpacity()
    {
        return opacity;
    }
    
    /**
     * @return true if this image will have its colour range scaled automatically.
     * This is true if scaleMin and scaleMax are both zero
     */
    public boolean isAutoScale()
    {
        return (this.scaleMin == 0.0f && this.scaleMax == 0.0f);
    }

    /**
     * @param opacity Percentage opacity of the data pixels
     * @throws IllegalArgumentException if opacity is out of the range [0,100]
     */
    public void setOpacity(int opacity)
    {
        if (opacity < 0 || opacity > 100)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0 to 100");
        }
        this.opacity = opacity;
    }

    public float getFillValue()
    {
        return fillValue;
    }

    /**
     * @param fillValue The value to use for missing data
     */
    public void setFillValue(float fillValue)
    {
        this.fillValue = fillValue;
    }

    public boolean isTransparent()
    {
        return transparent;
    }

    /**
     * @param transparent True if the background (missing data) pixels will be transparent
     */
    public void setTransparent(boolean transparent)
    {
        this.transparent = transparent;
    }

    public int getBgColor()
    {
        return bgColor;
    }

    /**
     * @param bgcolor Colour of background pixels if not transparent
     */
    public void setBgColor(int bgColor)
    {
        this.bgColor = bgColor;
    }
    
    /**
     * @return the colour palette as an array of 256 * 3 bytes, i.e. 256 colours
     * in RGB order
     */
    protected byte[] getRGBPalette()
    {
        byte[] palette = new byte[256 * 3];
        
        // Use the supplied background color
        Color bg = new Color(this.bgColor);
        palette[0] = (byte)bg.getRed();
        palette[1] = (byte)bg.getGreen();
        palette[2] = (byte)bg.getBlue();
        
        // Colour with index 1 is black (represents out-of-range data)
        palette[3] = 0;
        palette[4] = 0;
        palette[5] = 0;
        
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
            // There are 63 colours and 254 remaining slots
            float index = (i - 2) * (62.0f / 253.0f);
            if (i == 255)
            {
                palette[3*i]   = (byte)red[62];
                palette[3*i+1] = (byte)green[62];
                palette[3*i+2] = (byte)blue[62];
            }
            else
            {
                // We merge the colours from adjacent indices
                float fromUpper = index - (int)index;
                float fromLower = 1.0f - fromUpper;
                palette[3*i]   = (byte)(fromLower * red[(int)index] + fromUpper * red[(int)index + 1]);
                palette[3*i+1] = (byte)(fromLower * green[(int)index] + fromUpper * green[(int)index + 1]);
                palette[3*i+2] = (byte)(fromLower * blue[(int)index] + fromUpper * blue[(int)index + 1]);
            }
        }
        
        return palette;
    }
    
    /**
     * @return a rainbow colour map for this PicMaker's opacity and transparency
     * @todo To avoid multiplicity of objects, could statically create color models
     * for opacity=100 and transparency=true/false.
     */
    protected IndexColorModel getRainbowColorModel()
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
        
        // Use the supplied background color
        Color bg = new Color(this.bgColor);
        r[0] = (byte)bg.getRed();
        g[0] = (byte)bg.getGreen();
        b[0] = (byte)bg.getBlue();
        a[0] = this.transparent ? 0 : alpha;
        
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
    
    /**
     * Adjusts the colour scale to accommodate the given frame.
     */
    protected void adjustColourScaleForFrame(float[] data)
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
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set.  The scale must be set before
     * calling this method
     */
    protected BufferedImage createFrame(float[] data, String label)
    {
        logger.debug("Creating frame with {} pixels and label {}", data.length, label);
        // Create the pixel array for the frame
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = getColourIndex(data[i]);
        }
        logger.debug("  ... created pixel array");
        
        // Create the Image
        DataBuffer buf = new DataBufferByte(pixels, pixels.length);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_BYTE, this.picWidth, this.picHeight, new int[]{0xff});
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        IndexColorModel colorModel = getRainbowColorModel();
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        logger.debug("  ... created Image");
        
        // Add the label to the image
        if (label != null && !label.equals(""))
        {
            logger.debug("  ... adding label");
            Graphics2D gfx = image.createGraphics();
            gfx.setColor(new Color(0, 0, 143));
            gfx.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
            gfx.setColor(new Color(255, 151, 0));
            gfx.drawString(label, 10, image.getHeight() - 5);
            logger.debug("  ... added label");
        }
        logger.debug("  ... returning image");
        return image;
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

    public VariableMetadata getVar()
    {
        return var;
    }

    public void setVar(VariableMetadata var)
    {
        this.var = var;
    }
    
    /**
     * Adds a new frame to the image.  The scale must be set (with setScaleMin()
     * and setScaleMax()) before calling this method.
     * @param data Array of data points
     * @param bbox Array of four numbers representing the bounding box of the image
     * @param zValue The elevation value for this frame
     * @param tValue the time value for this frame in ISO8601 format
     * @param isAnimation True if this frame is part of an animation
     * @throws IOException if there was an error creating the frame
     * @throws Something if a frame already exists and this image type does not
     * support multiple frames
     */
    public abstract void addFrame(float[] data, float[] bbox, String zValue,
        String tValue, boolean isAnimation) throws IOException;
    
    /**
     * Encodes and writes the image to the given OutputStream
     * @param out The {@link OutputStream} to which the image will be written
     * @throws IOException if there was an error writing the data
     */
    public abstract void writeImage(OutputStream out) throws IOException;
    
}
