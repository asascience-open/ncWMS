/*
 * Copyright (c) 2008 The University of Reading
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
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * A palette of colours that is used by an {@link ImageProducer} to render 
 * data into a BufferedImage
 * @author Jon
 */
public class ColorPalette
{
    private static final Logger logger = Logger.getLogger(ColorPalette.class);
    
    private static final Map<String, ColorPalette> palettes =
        new HashMap<String, ColorPalette>();
    
    /**
     * This is the palette that will be used if no specific palette has been
     * chosen.  This palette is taken from the SGT graphics toolkit.
     */
    public static final ColorPalette DEFAULT_PALETTE = new ColorPalette(new Color[] {
        new Color(0,0,143), new Color(0,0,159), new Color(0,0,175),
        new Color(0,0,191), new Color(0,0,207), new Color(0,0,223),
        new Color(0,0,239), new Color(0,0,255), new Color(0,11,255),
        new Color(0,27,255), new Color(0,43,255), new Color(0,59,255),
        new Color(0,75,255), new Color(0,91,255), new Color(0,107,255),
        new Color(0,123,255), new Color(0,139,255), new Color(0,155,255),
        new Color(0,171,255), new Color(0,187,255), new Color(0,203,255),
        new Color(0,219,255), new Color(0,235,255), new Color(0,251,255),
        new Color(7,255,247), new Color(23,255,231), new Color(39,255,215),
        new Color(55,255,199), new Color(71,255,183), new Color(87,255,167),
        new Color(103,255,151), new Color(119,255,135), new Color(135,255,119),
        new Color(151,255,103), new Color(167,255,87), new Color(183,255,71),
        new Color(199,255,55), new Color(215,255,39), new Color(231,255,23),
        new Color(247,255,7), new Color(255,247,0), new Color(255,231,0),
        new Color(255,215,0), new Color(255,199,0), new Color(255,183,0),
        new Color(255,167,0), new Color(255,151,0), new Color(255,135,0),
        new Color(255,119,0), new Color(255,103,0), new Color(255,87,0),
        new Color(255,71,0), new Color(255,55,0), new Color(255,39,0),
        new Color(255,23,0), new Color(255,7,0), new Color(246,0,0),
        new Color(228,0,0), new Color(211,0,0), new Color(193,0,0),
        new Color(175,0,0), new Color(158,0,0), new Color(140,0,0)
    });
    
    private Color[] palette;
    
    private ColorPalette(Color[] palette)
    {
        this.palette = palette;
    }
    
    /**
     * Gets the names of the supported palettes.
     * @return the names of the palettes as a Set of Strings.  All Strings
     * will be in lower case.
     */
    public static final Set<String> getAvailablePaletteNames()
    {
        return palettes.keySet();
    }
    
    /**
     * This is called by WmsController on initialization to load all the palettes
     * in the WEB-INF/conf/palettes directory.  This will attempt to load all files
     * with the file extension ".pal".
     * @param paletteLocationDir Directory containing the palette files.  This
     * has already been checked to exist and be a directory
     */
    public static final void loadPalettes(File paletteLocationDir)
    {
        for (File file : paletteLocationDir.listFiles())
        {
            if (file.getName().endsWith(".pal"))
            {
                try
                {
                    ColorPalette palette = new ColorPalette(readColorPalette(file));
                    String paletteName = file.getName().substring(0, file.getName().lastIndexOf("."));
                    logger.debug("Read palette with name {}", paletteName);
                    palettes.put(paletteName.toLowerCase(), palette);
                }
                catch(Exception e)
                {
                    logger.error("Error reading from palette file {}", file.getName(), e);
                }
            }
        }
    }
    
    /**
     * Gets the palette with the given name.
     * @param name Name of the palette, corresponding with the name of the
     * palette file in WEB-INF/conf/palettes. Case insensitive.
     * @return the ColorPalette object, or null if there is no palette with
     * the given name.  If name is null or the empty string this will return
     * the default palette.
     */
    public static ColorPalette get(String name)
    {
        if (name == null || name.trim().equals("")) return DEFAULT_PALETTE;
        return palettes.get(name.trim().toLowerCase());
    }
    
    /**
     * Creates and returns a BufferedImage representing the legend for this 
     * palette
     * @param layer The Layer object for which this legend is being
     * created (needed for title and units strings)
     * @param colourScaleMin Data value corresponding to the bottom of the colour
     * scale
     * @param colourScaleMax Data value corresponding to the top of the colour
     * scale
     * @return a BufferedImage object representing the legend.  This has a fixed
     * size (110 pixels wide, 264 pixels high)
     */
    public BufferedImage createLegend(Layer layer, float colourScaleMin,
        float colourScaleMax)
    {
        // NOTE!! If you change the width and height here, you need to change
        // them in the Capabilities documents too.
        // TODO: make this consistent
        BufferedImage colourScale = new BufferedImage(110, 264, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D gfx = colourScale.createGraphics();
        
        // Create the colour scale itself
        for (int i = 5; i < 259; i++)
        {
            // TODO: doesn't work!  needs palette to be of correct length
            gfx.setColor(this.palette[260 - i]);
            gfx.drawLine(2, i, 25, i);
        }
        
        // Draw the text items
        gfx.setColor(Color.WHITE);
        // Add the scale values
        double quarter = 0.25 * (colourScaleMax - colourScaleMin);
        String scaleMinStr          = format(colourScaleMin);
        String scaleQuarterStr      = format(colourScaleMin + quarter);
        String scaleMidStr          = format(colourScaleMin + 2 * quarter);
        String scaleThreeQuarterStr = format(colourScaleMin + 3 * quarter);
        String scaleMaxStr          = format(colourScaleMax);        
        gfx.drawString(scaleMaxStr, 27, 10);
        gfx.drawString(scaleThreeQuarterStr, 27, 73);
        gfx.drawString(scaleMidStr, 27, 137);
        gfx.drawString(scaleQuarterStr, 27, 201);
        gfx.drawString(scaleMinStr, 27, 264);
        
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
        return new DecimalFormat("0.#####").format(d);
    }
    
    /**
     * Creates and returns an IndexColorModel based on this palette.
     * @throws IllegalArgumentException if the requested number of colour bands
     * is less than one or greater than 254.
     */
    public IndexColorModel getColorModel(int numColorBands, int opacity,
        Color bgColor, boolean transparent)
    {
        if (numColorBands < 1 || numColorBands > 254)
        {
            // Shouldn't happen: we have constrained this to a sane value in
            // GetMapStyleRequest
            throw new IllegalArgumentException("numColorBands must be between 1 and 254");
        }
        Color[] targetPalette;
        if (numColorBands == this.palette.length)
        {
            // We can just use the source palette directly
            targetPalette = this.palette;
        }
        else
        {
            // We need to create a new palette
            targetPalette = new Color[numColorBands];
            // We fix the endpoints of the target palette to the endpoints of the source palette
            targetPalette[0] = this.palette[0];
            targetPalette[targetPalette.length - 1] = this.palette[this.palette.length - 1];

            if (targetPalette.length < this.palette.length)
            {
                // We only need some of the colours from the source palette
                // We search through the target palette and find the nearest colours
                // in the source palette
                for (int i = 1; i < targetPalette.length - 1; i++)
                {
                    // Find the nearest index in the source palette
                    // (Multiplying by 1.0f converts integers to floats)
                    int nearestIndex = Math.round(this.palette.length * i * 1.0f / targetPalette.length);
                    targetPalette[i] = this.palette[nearestIndex];
                }
            }
            else
            {
                // Transfer all the colours from the source palette into their corresponding
                // positions in the target palette and use interpolation to find the remaining
                // values
                int lastIndex = 0;
                for (int i = 1; i < this.palette.length - 1; i++)
                {
                    // Find the nearest index in the target palette
                    int nearestIndex = Math.round(targetPalette.length * i * 1.0f / this.palette.length);
                    targetPalette[nearestIndex] = this.palette[i];
                    // Now interpolate all the values we missed
                    for (int j = lastIndex + 1; j < nearestIndex; j++)
                    {
                        // Work out how much we need from the previous colour and how much
                        // from the new colour
                        float fracFromThis = (1.0f * j - lastIndex) / (nearestIndex - lastIndex);
                        targetPalette[j] = interpolate(targetPalette[nearestIndex],
                            targetPalette[lastIndex], fracFromThis);
                    }
                    lastIndex = nearestIndex;
                }
                // Now for the last bit of interpolation
                for (int j = lastIndex + 1; j < targetPalette.length - 1; j++)
                {
                    float fracFromThis = (1.0f * j - lastIndex) / (targetPalette.length - lastIndex);
                    targetPalette[j] = interpolate(targetPalette[targetPalette.length - 1],
                        targetPalette[lastIndex], fracFromThis);
                }
            }
        }
        // Compute the alpha value based on the percentage transparency
        int alpha;
        // Here we are playing safe and avoiding rounding errors that might
        // cause the alpha to be set to zero instead of 255
        if (opacity >= 100) alpha = 255;
        else if (opacity <= 0)  alpha = 0;
        else alpha = (int)(2.55 * opacity);

        // Now simply copy the target palette to arrays of r,g,b and a
        byte[] r = new byte[targetPalette.length + 2];
        byte[] g = new byte[targetPalette.length + 2];
        byte[] b = new byte[targetPalette.length + 2];
        byte[] a = new byte[targetPalette.length + 2];
        for (int i = 0; i < targetPalette.length; i++)
        {
            r[i] = (byte)targetPalette[i].getRed();
            g[i] = (byte)targetPalette[i].getGreen();
            b[i] = (byte)targetPalette[i].getBlue();
            a[i] = (byte)alpha;
        }

        // The next index represents the background colour (which may be transparent)
        r[targetPalette.length] = (byte)bgColor.getRed();
        g[targetPalette.length] = (byte)bgColor.getGreen();
        b[targetPalette.length] = (byte)bgColor.getBlue();
        a[targetPalette.length] = transparent ? 0 : (byte)alpha;

        // The next represents out-of-range pixels (black)
        r[targetPalette.length + 1] = 0;
        g[targetPalette.length + 1] = 0;
        b[targetPalette.length + 1] = 0;
        a[targetPalette.length + 1] = (byte)alpha;

        // Now we can create the color model
        return new IndexColorModel(8, r.length, r, g, b, a);
    }
    
    /**
     * Linearly interpolates between two RGB colours
     * @param c1 the first colour
     * @param c2 the second colour
     * @param fracFromC1 the fraction of the final colour that will come from c1
     * @return the interpolated Color
     */
    private static Color interpolate(Color c1, Color c2, float fracFromC1)
    {
        float fracFromC2 = 1.0f - fracFromC1;
        return new Color(
            Math.round(fracFromC1 * c1.getRed() + fracFromC2 * c2.getRed()),
            Math.round(fracFromC1 * c1.getGreen() + fracFromC2 * c2.getGreen()),
            Math.round(fracFromC1 * c1.getBlue() + fracFromC2 * c2.getBlue())
        );
    }
    
    /**
     * Reads a colour palette (as an array of Color object) from the given File.
     * Each line in the file contains a single colour, expressed as space-separated
     * RGB values.  These values can be integers in the range 0->255 or floats
     * in the range 0->1.  If the palette cannot be read, no exception is thrown
     * but an event is logged to the error log.
     * @throws Exception if the palette file could not be read or contains a
     * format error
     */
    private static Color[] readColorPalette(File paletteFile) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(paletteFile));
        List<Color> colours = new ArrayList<Color>();
        String line;
        try
        {
            while((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.trim().equals(""))
                {
                    continue; // Skip comment lines and blank lines
                }
                StringTokenizer tok = new StringTokenizer(line.trim());
                try
                {
                    if (tok.countTokens() < 3) throw new Exception();
                    // We only read the first three tokens
                    Float r = Float.valueOf(tok.nextToken());
                    Float g = Float.valueOf(tok.nextToken());
                    Float b = Float.valueOf(tok.nextToken());
                    // Check for negative numbers
                    if (r < 0.0f || g < 0.0f || b < 0.0f) throw new Exception();
                    if (r > 1.0f || g > 1.0f || b > 1.0f)
                    {
                        // We assume this colour is expressed in the range 0->255
                        if (r > 255.0f || g > 255.0f || b > 255.0f) throw new Exception();
                        colours.add(new Color(r.intValue(), g.intValue(), b.intValue()));
                    }
                    else
                    {
                        // the colours are expressed in the range 0->1
                        colours.add(new Color(r, g, b));
                    }
                }
                catch(Exception e)
                {
                    throw new Exception("File format error: each line must contain three numbers between 0 and 255 or 0.0 and 1.0 (R, G, B)");
                }
            }
        }
        finally
        {
            if (reader != null) reader.close();
        }
        return colours.toArray(new Color[0]);
    }

}
