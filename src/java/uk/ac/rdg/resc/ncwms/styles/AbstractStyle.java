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
    protected String name;
    // Width and height of the resulting picture
    protected int picWidth;
    protected int picHeight;
    // The fill value of the data.
    protected float fillValue;
    protected boolean transparent;
    protected int bgColor; // Background colour as an integer
    // set of rendered images, ready to be turned into a picture
    protected ArrayList<BufferedImage> renderedFrames;
    
    /**
     * Creates a new instance of AbstractStyle
     * @param name A name for this style
     */
    protected AbstractStyle(String name)
    {
        this.name = name;
        this.renderedFrames = new ArrayList<BufferedImage>();
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
     * Sets an attribute of this Style
     * @param attName The name of the attribute (e.g. "fgcolor")
     * @param value The value for the attribute
     */
    public abstract void setAttribute(String attName, String value);
    
    /**
     * Creates a single image in this style and adds to the internal store
     * of BufferedImages.
     * @param data The data to be rendered into an image
     * @param label Label to add to the image (ignored if null or the empty string)
     */
    public abstract void createImage(float[] data, String label);

    /**
     * Gets the frames as BufferedImages, ready to be turned into a picture or
     * animation.  This is called just before the picture is due to be created,
     * so subclasses can delay creating the BufferedImages until all the data
     * has been extracted (for example, if we are auto-scaling an animation,
     * we can't create each individual frame until we have data for all the frames)
     * @return ArrayList of BufferedImages
     */
    public abstract ArrayList<BufferedImage> getRenderedFrames();
    
}
