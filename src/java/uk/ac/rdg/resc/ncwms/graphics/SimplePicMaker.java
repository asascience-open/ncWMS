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

/**
 * Makes a picture from an array of raw data, using a rainbow colour model.
 * Fill values are represented as transparent pixels and out-of-range values
 * are represented as black pixels.
 * @author jdb
 */
public class SimplePicMaker extends PicMaker
{
    private byte[] pixels;
    
    /**
     * Creates a new SimplePicMaker, automatically creating a colour scale
     * @param data The raw data to turn into a picture
     * @param width The width of the picture in pixels
     * @param height The height of the picture in pixels
     * @param fillValue The value to use for missing data
     * @throws IllegalArgumentException if width * height != data.length
     */
    public SimplePicMaker(float[] data, int width, int height, float fillValue)
    {
        this(data, width, height, fillValue, 0.0f, 0.0f);
    }
    
    /**
     * Creates a new instance of SimplePicMaker, manually setting the scale.  If scaleMin
     * and scaleMax are both zero (0.0f) the picture will be auto-scaled.
     * @param data The raw data to turn into a picture
     * @param width The width of the picture in pixels
     * @param height The height of the picture in pixels
     * @param fillValue The value to use for missing data
     * @param scaleMin The minimum value for the scale
     * @param scaleMax The maximum value for the scale
     * @throws IllegalArgumentException if width * height != data.length
     */
    public SimplePicMaker(float[] data, int width, int height, 
        float fillValue, float scaleMin, float scaleMax)
    {
        super(data, width, height, fillValue, scaleMin, scaleMax);
        if (scaleMin == 0.0f && scaleMax == 0.0f)
        {
            this.setScaleAuto();
        }
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
     * Makes the picture (array of pixels) from the data array.
     */
    private void makePicture()
    {
        this.pixels = new byte[this.data.length];
        for (int i = 0; i < this.data.length; i++)
        {
            //int row = i / this.picWidth;
            //int col = i % this.picWidth;
            //int newRow = this.picHeight - 1 - row;
            //int newLoc = newRow * this.picWidth + col;
            //this.pixels[newLoc] = getColourIndex(this.data[i]);
            this.pixels[i] = getColourIndex(this.data[i]);
        }
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
     * Sets the array of pixels that make up this picture
     */
    public void setPixels(byte[] pixels)
    {
        this.pixels = pixels;
    }
    
    /**
     * Creates the picture and writes it to the given OutputStream
     * @throws IOException if the picture could not be written to the stream
     */
    public void createAndOutputPicture(OutputStream out) throws IOException
    {
        if (this.pixels == null)
        {
            this.makePicture();
        }
        DataBuffer buf = new DataBufferByte(this.pixels, this.pixels.length);
        SampleModel sampleModel = new SinglePixelPackedSampleModel(
            DataBuffer.TYPE_BYTE, this.picWidth, this.picHeight, new int[]{0xff});
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        BufferedImage image = new BufferedImage(getRainbowColorModel(), raster, false, null);
        // Now write the image
        ImageIO.write(image, "png", out);
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
        
        // Colour with index 0 is fully transparent
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
        if (this.opacity == 100)
        {
            alpha = (byte)255;
        }
        else if (this.opacity == 0)
        {
            alpha = 0;
        }
        else
        {
            alpha = (byte)(2.55 * this.opacity);
        }
        
        // Colour with index 0 is fully transparent
        r[0] = 0;   g[0] = 0;   b[0] = 0;   a[0] = 0;
        // Colour with index 1 is black
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
