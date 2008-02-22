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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * An abstract definition of a Style
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class AbstractStyle
{
    private static final Logger logger = Logger.getLogger(AbstractStyle.class);
    
    protected String name;
    // Width and height of the resulting picture
    protected int picWidth;
    protected int picHeight;
    protected boolean transparent;
    protected int bgColor; // Background colour as an integer
    
    // Scale range of the picture
    protected float scaleMin;
    protected float scaleMax;
    
    // set of rendered images, ready to be turned into a picture
    protected List<BufferedImage> renderedFrames;
    // If we need to cache the frame data and associated labels (we do this if
    // we have to auto-scale the image) this is where we put them.
    protected List<List<float[]>> frameData;
    protected List<String> labels;
    
    /**
     * Creates a new instance of AbstractStyle
     * @param name A name for this style
     */
    protected AbstractStyle(String name)
    {
        this.name = name;
        this.renderedFrames = new ArrayList<BufferedImage>();
    }
    
    public void setScaleRange(float[] scaleRange)
    {
        if (scaleRange.length != 2)
        {
            throw new IllegalArgumentException("scaleRange must have two elements");
        }
        this.setScaleRange(scaleRange[0], scaleRange[1]);
    }
    
    public void setScaleRange(float scaleMin, float scaleMax)
    {
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        logger.debug("Set SCALE to {},{}", this.scaleMin, this.scaleMax);
    }
    
    /**
     * @return the name for this style (e.g. "boxfill")
     */
    public String getName()
    {
        return this.name;
    }
    
    public int getPicWidth()
    {
        return picWidth;
    }
    
    /**
     * @param picWidth The width of the picture in pixels
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
     * @param picHeight The height of the picture in pixels
     */
    public void setPicHeight(int picHeight)
    {
        this.picHeight = picHeight;
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
     * @param bgColor Colour of background pixels if not transparent
     */
    public void setBgColor(int bgColor)
    {
        this.bgColor = bgColor;
    }
    
    /**
     * Sets an attribute of this Style
     * @param attName The name of the attribute (e.g. "fgcolor")
     * @param values The value(s) for the attribute
     * @throws StyleNotDefinedException if there is an error with the attribute
     */
    public abstract void setAttribute(String attName, String[] values)
        throws StyleNotDefinedException;
    
    /**
     * Adds a frame of data to this Style.  If the data cannot yet be rendered
     * into a BufferedImage, the data and label are stored.
     */
    public void addFrame(List<float[]> data, String label)
    {
        logger.debug("Adding frame with label {}", label);
        if (this.isAutoScale())
        {
            logger.debug("Auto-scaling, so caching frame");
            if (this.frameData == null)
            {
                this.frameData = new ArrayList<List<float[]>>();
                this.labels = new ArrayList<String>();
            }
            this.frameData.add(data);
            this.labels.add(label);
        }
        else
        {
            logger.debug("Scale is set, so rendering image");
            this.renderedFrames.add(this.createImage(data, label));
        }
    }
    
    /**
     * Creates a single image in this style and returns it as a
     * BufferedImage.  This is only called when the scale information has been
     * set, so all info should be present for creating the image.
     * @param data The data to be rendered into an image.
     * @param label Label to add to the image (ignored if null or the empty string)
     */
    protected abstract BufferedImage createImage(List<float[]> data, String label);
    
    /**
     * @return true if this image is to be auto-scaled (meaning we have to collect
     * all of the frame data before we calculate the scale)
     */
    protected abstract boolean isAutoScale();
    
    /**
     * Adjusts the colour scale of the image based on the given data.  Used if
     * <code>isAutoScale() == true</code>.
     */
    protected abstract void adjustScaleForFrame(List<float[]> data);
    
    /**
     * Creates and returns a BufferedImage representing the legend for this
     * Style instance.  Sets the colour scale if we need to.
     * @param layer The Layer object for which this legend is being
     * created (needed for title and units strings)
     * @todo Allow setting of width and height of legend
     */
    public BufferedImage getLegend(Layer layer)
    {
        this.setScale();
        return this.createLegend(layer);
    }
    
    /**
     * Creates and returns a BufferedImage representing the legend for this
     * Style instance.  The colour scale will already have been set before this
     * is called.
     * @param layer The Layer object for which this legend is being
     * created (needed for title and units strings)
     * @todo Allow setting of width and height of legend
     */
    protected abstract BufferedImage createLegend(Layer layer);
    
    /**
     * Gets the frames as BufferedImages, ready to be turned into a picture or
     * animation.  This is called just before the picture is due to be created,
     * so subclasses can delay creating the BufferedImages until all the data
     * has been extracted (for example, if we are auto-scaling an animation,
     * we can't create each individual frame until we have data for all the frames)
     * @return List of BufferedImages
     */
    public List<BufferedImage> getRenderedFrames()
    {
        this.setScale(); // Make sure the colour scale is set before proceeding
        // We render the frames if we have not done so already
        if (this.frameData != null)
        {
            logger.debug("Rendering image frames...");
            for (int i = 0; i < this.frameData.size(); i++)
            {
                logger.debug("    ... rendering frame {}", i);
                this.renderedFrames.add(this.createImage(this.frameData.get(i), this.labels.get(i)));
            }
        }
        return this.renderedFrames;
    }
    
    /**
     * Makes sure that the scale is set: if we are auto-scaling, this reads all
     * of the data we have stored to find the extremes.  If the scale has
     * already been set, this does nothing
     */
    protected void setScale()
    {
        if (this.isAutoScale())
        {
            logger.debug("Setting the scale automatically");
            // We have a cache of image data, which we use to generate the colour scale
            for (List<float[]> data : this.frameData)
            {
                this.adjustScaleForFrame(data);
            }
        }
    }
    
}
