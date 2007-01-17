/*
 * Copyright (c) 2005 The University of Reading
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

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.awt.Color;

import java.io.OutputStream;
import java.io.IOException;
import net.jmge.gif.Gif89Encoder;

/**
 * Makes a picture from an array of raw data, using a rainbow colour model.
 * Fill values are represented as transparent pixels and out-of-range values
 * are represented as black pixels.
 * @author jdb
 */
public class SimplePicMaker
{
    // Data to turn into an image
    protected float[] data;
    private int numFrames;
    // Image MIME type
    protected String mimeType;
    // Width and height of the resulting picture
    protected int picWidth;
    protected int picHeight;
    // Scale range of the picture
    protected float scaleMin;
    protected float scaleMax;
    // The percentage opacity of the picture
    protected float opacity;
    // The fill value of the data.
    protected float fillValue;
    
    private boolean transparent;
    private Color bgColor;
    
    public static final String GIF_FORMAT = "image/gif";
    public static final String PNG_FORMAT = "image/png";
    
    /**
     * @return array of Strings representing the MIME types of supported image
     * formats
     */
    public static final String[] getSupportedImageFormats()
    {
        return new String[]{GIF_FORMAT, PNG_FORMAT};
    }
    
    /**
     * Creates a new instance of SimplePicMaker, manually setting the scale.  If scaleMin
     * and scaleMax are both zero (0.0f) the picture will be auto-scaled.
     * @param data The raw data to turn into a picture
     * @param mimeType The MIME type for the image
     * @param width The width of the picture in pixels
     * @param height The height of the picture in pixels
     * @param fillValue The value to use for missing data
     * @param transparent True if the background (missing data) pixels will be transparent
     * @param bgcolor Colour of background pixels if not transparent
     * @param opacity Percentage opacity of the data pixels
     * @param scaleMin The minimum value for the scale
     * @param scaleMax The maximum value for the scale
     * @throws IllegalArgumentException if the <code>mimeType</code> is not
     * supported or if data.length / (width * height) is not an integer or if
     * we have tried to create a PNG of an animation
     */
    public SimplePicMaker(float[] data, String mimeType, int width,
        int height, float fillValue, boolean transparent, int bgcolor, float opacity,
        float scaleMin, float scaleMax)
    {
        if (data.length % (width * height) != 0)
        {
            throw new IllegalArgumentException("The given width and height are " +
                "inconsistent with the data size");
        }
        this.data = data;
        this.numFrames = data.length / (width * height);
        if (!mimeType.equals(GIF_FORMAT) && !mimeType.equals(PNG_FORMAT))
        {
            // TODO This should really be an InvalidFormatException, but
            // this error should have been caught in the Jython code (getmap.py)
            // so this check is just being safe
            throw new IllegalArgumentException("The image format " + mimeType + 
                " is not supported by this server");
        }
        if (this.numFrames > 1 && !mimeType.equals(GIF_FORMAT))
        {
            throw new IllegalArgumentException("Cannot create an animation in "
                + mimeType + " format");
        }
        this.mimeType = mimeType;
        this.picWidth = width;
        this.picHeight = height;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.opacity = 100;
        this.fillValue = fillValue;
        this.transparent = transparent;
        this.bgColor = new Color(bgcolor);
        this.opacity = opacity;
        if (scaleMin == 0.0f && scaleMax == 0.0f)
        {
            this.setScaleAuto();
        }
    }
    
    /**
     * Sets the percentage transparency of the picture (100 = fully opaque,
     * 0 = fully transparent)
     * @throws IllegalArgumentException if the transparency is out of the range 0 - 100
     */
    public void setOpacity(int opacity)
    {
        if (opacity < 0 || opacity > 100)
        {
            throw new IllegalArgumentException("Opacity must be in the range 0 to 100");
        }
        this.opacity = opacity;
    }
    
    /**
     * Sets the scale of the picture as the minimum and maximum values of the given
     * data array
     */
    private void setScaleAuto()
    {
        if (this.data != null)
        {
            this.scaleMin = Float.MAX_VALUE;
            this.scaleMax = -Float.MAX_VALUE;
            for (int i = 0; i < this.data.length; i++)
            {
                if (this.data[i] != this.fillValue)
                {
                    if (this.data[i] < this.scaleMin)
                    {
                        this.scaleMin = this.data[i];
                    }
                    if (this.data[i] > this.scaleMax)
                    {
                        this.scaleMax = this.data[i];
                    }
                }
            }
        }
    }
    
    /**
     * Makes a picture (array of pixels) from the data array.
     */
    private byte[] makePicture(int frame)
    {
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = getColourIndex(this.data[frame * this.picWidth * this.picHeight + i]);
        }
        return pixels;
    }
    
    /**
     * @return the colour index that corresponds to the given value
     */
    public byte getColourIndex(float value)
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
     * Creates the picture and writes it to the given OutputStream
     * @throws IOException if the picture could not be written to the stream
     * @todo could be neater: refactor?
     */
    public void createAndOutputPicture(OutputStream out) throws IOException
    {
        Gif89Encoder gifenc = null;
        for (int i = 0; i < this.numFrames; i++)
        {
            byte[] pixels = this.makePicture(i);
            DataBuffer buf = new DataBufferByte(pixels, pixels.length);
            SampleModel sampleModel = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_BYTE, this.picWidth, this.picHeight, new int[]{0xff});
            WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
            BufferedImage image = new BufferedImage(getRainbowColorModel(), raster, false, null);
            // Now write the image
            if (this.mimeType.equals(GIF_FORMAT))
            {
                if (gifenc == null)
                {
                    gifenc = new Gif89Encoder();
                }
                gifenc.addFrame(image);
            }
            else
            {
                // Default to a PNG: we have already checked that the format
                // is either gif or png.
                // We have already checked that there is only one frame
                ImageIO.write(image, "png", out);
            }
        }
        if (this.mimeType.equals(GIF_FORMAT))
        {
            if (this.numFrames > 1)
            {
                gifenc.setLoopCount(-1); // Infinite looping of animated GIFs
            }
            gifenc.encode(out);
        }
    }
    
    /**
     * @return an IndexColorModel with rainbow colours.  The pixel with index
     * 0 is fully transparent.  The pixel with index 1 is black (used to indicate
     * out-of-range values).  Low indices will give blue colours, high indices
     * will give red colours
     */
    private static IndexColorModel getRainbowColorModel2()
    {
        byte[] r = new byte[256];   byte[] g = new byte[256];
        byte[] b = new byte[256];   byte[] a = new byte[256];
        
        // Set the alpha value based on the percentage transparency
        byte alpha = (byte)(255);
        
        // Colour with index 0 is background
        r[0] = 0;   g[0] = 0;   b[0] = 0;   a[0] = 0;
        // Colour with index 1 is black
        r[1] = 0;   g[1] = 0;   b[1] = 0;   a[1] = alpha;
        
        // Saturation and brightness are always at maximum
        float sat = 1.0f;
        float bri = 1.0f;
        
        // Set the rest of the colours, based on incrementing the hue
        Color rgb;
        for (int i = 2; i < 256; i++)
        {
            float hue = (255 - i) / 300.0f;
            rgb = Color.getHSBColor(hue, sat, bri);
            r[i] = (byte)rgb.getRed();
            g[i] = (byte)rgb.getGreen();
            b[i] = (byte)rgb.getBlue();
            a[i] = alpha;
        }
        return new IndexColorModel(8, 256, r, g, b, a);
    }
    
    /**
     * @return a better colour map
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
            r[0] = (byte)this.bgColor.getRed();
            g[0] = (byte)this.bgColor.getGreen();
            b[0] = (byte)this.bgColor.getBlue();
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
