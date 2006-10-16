/*
 * PicMaker.java
 *
 * Created on 13 February 2006, 12:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.ac.rdg.resc.ncwms.graphics;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstract class for users to implement code to turn data into a picture and
 * output it.
 * @author jdb
 */
public abstract class PicMaker
{
    // Data to turn into an image
    protected float[] data;
    // Width and height of the resulting picture
    protected int picWidth;
    protected int picHeight;
    // Scale range of the picture
    protected float scaleMin;
    protected float scaleMax;
    // The percentage opacity of the picture
    protected int opacity;
    // The fill value of the data.  Set to GmapsDataReader.FILL_VALUE by default
    protected float fillValue;
    
    /**
     * Creates a new instance of PicMaker, manually setting the scale.  If scaleMin
     * and scaleMax are both zero (0.0f) the picture will be auto-scaled.
     * @param data The raw data to turn into a picture
     * @param width The width of the picture in pixels
     * @param height The height of the picture in pixels
     * @param scaleMin The minimum value for the scale
     * @param scaleMax The maximum value for the scale
     * @throws IllegalArgumentException if width * height != data.length
     */
    public PicMaker(float[] data, int width, int height, float scaleMin, float scaleMax)
    {
        if (data != null && width * height != data.length)
        {
            throw new IllegalArgumentException("The dimensions of the picture ("
                + width + ", " + height + ") do not match the size of the provided" +
                " data array (" + data.length + " elements)");
        }
        this.data = data;
        this.picWidth = width;
        this.picHeight = height;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.opacity = 100;
        this.fillValue = Float.NaN;
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
     * Sets a new value for the fill value that represents missing data and will
     * be rendered as transparent pixels in the final picture
     */
    public void setFillValue(float newFillValue)
    {
        this.fillValue = newFillValue;
    }
    
    /**
     * Creates the picture and writes it to the given OutputStream
     * @throws IOException if the picture could not be written to the stream
     */
    public abstract void createAndOutputPicture(OutputStream out) throws IOException;
    
}
