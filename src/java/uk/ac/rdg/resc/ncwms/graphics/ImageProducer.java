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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.exceptions.StyleNotDefinedException;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.Ranges;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * An object that is used to render data into images.  Instances of this class
 * must be created through the {@link Builder}.
 *
 * @author Jon Blower
 */
public final class ImageProducer
{
    private static final Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    private enum Style {BOXFILL, VECTOR};
    
    private Layer layer;
    private Style style;
    // Width and height of the resulting picture
    private int picWidth;
    private int picHeight;
    private boolean transparent;
    private int opacity;
    private int numColourBands;
    private boolean logarithmic;  // True if the colour scale is to be logarithmic,
                                  // false if linear
    private Color bgColor;
    private ColorPalette colorPalette;
    
    /**
     * Colour scale range of the picture.  An {@link Range#isEmpty() empty Range}
     * means that the picture will be auto-scaled.
     */
    private Range<Float> scaleRange;
    
    /**
     * The length of arrows in pixels, only used for vector plots
     */
    private float arrowLength = 10.0f;
    
    // set of rendered images, ready to be turned into a picture
    private List<BufferedImage> renderedFrames = new ArrayList<BufferedImage>();
    // If we need to cache the frame data and associated labels (we do this if
    // we have to auto-scale the image) this is where we put them.
    // The inner List<Float> is the data for a single vector component
    // The middle List contains the two vector components
    // The outer List contains data for each animation frame
    private List<List<List<Float>>> frameData; // YUCK!!!
    private List<String> labels;

    /** Prevents direct instantiation */
    private ImageProducer() {}

    public BufferedImage getLegend()
    {
        return this.colorPalette.createLegend(this.numColourBands, this.layer,
            this.logarithmic, this.scaleRange);
    }
    
    public int getPicWidth()
    {
        return picWidth;
    }
    
    public int getPicHeight()
    {
        return picHeight;
    }
    
    public boolean isTransparent()
    {
        return transparent;
    }
    
    /**
     * Adds a frame of data to this ImageProducer.  If the data cannot yet be rendered
     * into a BufferedImage, the data and label are stored.
     */
    public void addFrame(List<List<Float>> data, String label)
    {
        logger.debug("Adding frame with label {}", label);
        if (this.scaleRange.isEmpty())
        {
            logger.debug("Auto-scaling, so caching frame");
            if (this.frameData == null)
            {
                this.frameData = new ArrayList<List<List<Float>>>();
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
     * Creates and returns a single frame as an Image, based on the given data.
     * Adds the label if one has been set.  The scale must be set before
     * calling this method.
     */
    private BufferedImage createImage(List<List<Float>> data, String label)
    {
        // Create the pixel array for the frame
        byte[] pixels = new byte[this.picWidth * this.picHeight];
        // We get the magnitude of the input data (takes care of the case
        // in which the data are two components of a vector)
        List<Float> magnitudes = data.size() == 1
            ? data.get(0)
            : WmsUtils.getMagnitudes(data.get(0), data.get(1));
        for (int i = 0; i < pixels.length; i++)
        {
            pixels[i] = (byte)this.getColourIndex(magnitudes.get(i));
        }
        
        // Create a ColorModel for the image
        ColorModel colorModel = this.colorPalette.getColorModel(this.numColourBands,
            this.opacity, this.bgColor, this.transparent);
        
        // Create the Image
        DataBuffer buf = new DataBufferByte(pixels, pixels.length);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(this.picWidth, this.picHeight);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, buf, null);
        BufferedImage image = new BufferedImage(colorModel, raster, false, null);
        
        // Add the label to the image
        // TODO: colour needs to change with different palettes!
        if (label != null && !label.equals(""))
        {
            Graphics2D gfx = (Graphics2D)image.getGraphics();
            gfx.setPaint(new Color(0, 0, 143));
            gfx.fillRect(1, image.getHeight() - 19, image.getWidth() - 1, 18);
            gfx.setPaint(new Color(255, 151, 0));
            gfx.drawString(label, 10, image.getHeight() - 5);
        }
        
        if (this.style == Style.VECTOR)
        {
            // We superimpose direction arrows on top of the background
            // TODO: only do this for lat-lon projections!
            Graphics2D g = image.createGraphics();
            // TODO: control the colour of the arrows with an attribute
            // Must be part of the colour palette (here we use the colour
            // for out-of-range values)
            g.setColor(Color.BLACK);

            logger.debug("Drawing vectors, length = {} pixels", this.arrowLength);
            List<Float> east = data.get(0);
            List<Float> north = data.get(1);
            for (int i = 0; i < this.picWidth; i += Math.ceil(this.arrowLength * 1.2))
            {
                for (int j = 0; j < this.picHeight; j += Math.ceil(this.arrowLength * 1.2))
                {
                    int dataIndex = j * this.picWidth + i;
                    Float eastVal = east.get(dataIndex);
                    Float northVal = north.get(dataIndex);
                    if (eastVal != null && northVal != null)
                    {
                        double angle = Math.atan2(northVal.doubleValue(), eastVal.doubleValue());
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
        }
        
        return image;
    }
    
    /**
     * @return the colour index that corresponds to the given value
     */
    private int getColourIndex(Float value)
    {
        if (value == null)
        {
            return this.numColourBands; // represents a background pixel
        }
        else if (!this.scaleRange.contains(value))
        {
            return this.numColourBands + 1; // represents an out-of-range pixel
        }
        else
        {
            float scaleMin = this.scaleRange.getMinimum().floatValue();
            float scaleMax = this.scaleRange.getMaximum().floatValue();
            double min = this.logarithmic ? Math.log(scaleMin) : scaleMin;
            double max = this.logarithmic ? Math.log(scaleMax) : scaleMax;
            double val = this.logarithmic ? Math.log(value) : value;
            double frac = (val - min) / (max - min);
            // Compute and return the index of the corresponding colour
            int index = (int)(frac * this.numColourBands);
            // For values very close to the maximum value in the range, this
            // index might turn out to be equal to this.numColourBands due to
            // rounding error.  In this case we subtract one from the index to
            // ensure that such pixels are not displayed as background pixels.
            if (index == this.numColourBands) index--;
            return index;
        }
    }
    
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
     * already been set, this does nothing.
     */
    private void setScale()
    {
        if (this.scaleRange.isEmpty())
        {
            Float scaleMin = null;
            Float scaleMax = null;
            logger.debug("Setting the scale automatically");
            // We have a cache of image data, which we use to generate the colour scale
            for (List<List<Float>> data : this.frameData)
            {
                // We only use the first component if this is a vector quantity
                Range<Float> range = Ranges.findMinMax(data.get(0));
                // TODO: could move this logic to the Range/Ranges class
                if (!range.isEmpty())
                {
                    if (scaleMin == null || range.getMinimum().compareTo(scaleMin) < 0)
                    {
                        scaleMin = range.getMinimum();
                    }
                    if (scaleMax == null || range.getMaximum().compareTo(scaleMax) > 0)
                    {
                        scaleMax = range.getMaximum();
                    }
                }
            }
            this.scaleRange = Ranges.newRange(scaleMin, scaleMax);
        }
    }

    public int getOpacity()
    {
        return opacity;
    }

    /**
     * Builds an ImageProducer
     */
    public static final class Builder
    {
        private Layer layer = null;
        private int picWidth = -1;
        private int picHeight = -1;
        private boolean transparent = false;
        private int opacity = 100;
        private int numColourBands = 254;
        private Boolean logarithmic = null;
        private Color bgColor = Color.WHITE;
        private Range<Float> scaleRange = null;
        private Style style = null;
        private ColorPalette colorPalette = null;

        /** Sets the layer object (must be set: there is no default) */
        public Builder layer(Layer layer) {
            if (layer == null) throw new NullPointerException();
            this.layer = layer;
            return this;
        }

        /**
         * Sets the style to be used.  If not set or if the parameter is null,
         * the layer's default style and colour palette will be used.
         * @param styleSpec String in the form "&lt;styletype&gt;/&lt;paletteName&gt;
         * @throws StyleNotDefinedException if the string is not valid, or if
         * the style type or palette name are not supported by this server
         */
        public Builder style(String styleSpec) throws StyleNotDefinedException {
            if (styleSpec == null) return this;
            String[] styleStrEls = styleSpec.split("/");

            // Get the style type
            String styleType = styleStrEls[0];
            if (styleType.equalsIgnoreCase("boxfill")) this.style = Style.BOXFILL;
            else if (styleType.equalsIgnoreCase("vector")) this.style = Style.VECTOR;
            else throw new StyleNotDefinedException("The style " + styleSpec +
                " is not supported by this server");

            // Now get the colour palette
            String paletteName = null;
            if (styleStrEls.length > 1) paletteName = styleStrEls[1];
            this.colorPalette = ColorPalette.get(paletteName);
            if (this.colorPalette == null) {
                throw new StyleNotDefinedException("There is no palette with the name "
                    + paletteName);
            }
            return this;
        }

        /** Sets the width of the picture (must be set: there is no default) */
        public Builder width(int width) {
            if (width < 0) throw new IllegalArgumentException();
            this.picWidth = width;
            return this;
        }

        /** Sets the height of the picture (must be set: there is no default) */
        public Builder height(int height) {
            if (height < 0) throw new IllegalArgumentException();
            this.picHeight = height;
            return this;
        }

        /** Sets whether or not background pixels should be transparent
         * (defaults to false) */
        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        /** Sets the opacity of the picture, from 0 to 100 (default 100) */
        public Builder opacity(int opacity) {
            if (opacity < 0 || opacity > 100) throw new IllegalArgumentException();
            this.opacity = opacity;
            return this;
        }

        /** Sets the colour scale range.  If not set (or if set to null), the
         * layer's approx value range will be used. */
        public Builder colourScaleRange(Range<Float> scaleRange) {
            this.scaleRange = scaleRange;
            return this;
        }

        /** Sets the number of colour bands to use in the image, from 0 to 254
         * (default 254) */
        public Builder numColourBands(int numColourBands) {
            if (numColourBands < 0 || numColourBands > 254) {
                throw new IllegalArgumentException();
            }
            this.numColourBands = numColourBands;
            return this;
        }

        /**
         * Sets whether or not the colour scale is to be spaced logarithmically
         * (null is the default and means "use the Layer's default).
         */
        public Builder logarithmic(Boolean logarithmic) {
            this.logarithmic = logarithmic;
            return this;
        }

        /**
         * Sets the background colour, which is used only if transparent==false,
         * for background pixels.  Defaults to white.  If the passed-in color
         * is null, it is ignored.
         */
        public Builder backgroundColour(Color bgColor) {
            if (bgColor != null) this.bgColor = bgColor;
            return this;
        }

        /**
         * Checks the fields for internal consistency, then creates and returns
         * a new ImageProducer object.
         * @throws IllegalStateException if the builder cannot create a vali
         * ImageProducer object
         * @throws StyleNotDefinedException if the set style is not supported
         * by the set layer
         */
        public ImageProducer build() throws StyleNotDefinedException
        {
            // Perform consistency checks
            if (layer == null) {
                throw new IllegalStateException("Must set the Layer object");
            }
            if (this.picWidth < 0 || this.picHeight < 0) {
                throw new IllegalStateException("picture width and height must be >= 0");
            }
            if (this.style == Style.VECTOR && !(this.layer instanceof VectorLayer)) {
                throw new StyleNotDefinedException("The style " + this.style +
                    " is not supported by this layer");
            }

            ImageProducer ip = new ImageProducer();
            ip.layer = this.layer;
            ip.picWidth = this.picWidth;
            ip.picHeight = this.picHeight;
            ip.opacity = this.opacity;
            ip.transparent = this.transparent;
            ip.bgColor = this.bgColor;
            ip.numColourBands = this.numColourBands;
            ip.style = this.style == null
                ? (layer instanceof VectorLayer ? Style.VECTOR : Style.BOXFILL)
                : this.style;
            ip.colorPalette = this.colorPalette == null
                ? layer.getDefaultColorPalette()
                : this.colorPalette;
            ip.logarithmic = this.logarithmic == null
                ? layer.isLogScaling()
                : this.logarithmic.booleanValue();
            ip.scaleRange = this.scaleRange == null
                ? layer.getApproxValueRange()
                : this.scaleRange;

            return ip;
        }
    }
}
