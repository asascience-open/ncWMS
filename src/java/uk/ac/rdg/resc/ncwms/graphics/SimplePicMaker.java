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

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Makes a picture from an array of raw data, using a rainbow colour model.
 * Fill values are represented as transparent pixels and out-of-range values
 * are represented as black pixels.
 *
 * Supports any output format that is supported by ImageIO class
 * @author jdb
 */
public class SimplePicMaker extends PicMaker
{
    // Data to turn into an image
    protected float[] data;
    
    public SimplePicMaker()
    {
        this.data = null;
    }

    public void addFrame(float[] data, float[] bbox, String zValue,
        String tValue, boolean isAnimation) throws IOException
    {
        if (this.data != null)
        {
            // TODO Throw an Exception: this does not support animations
        }
        this.data = data;
    }

    public void writeImage(OutputStream out) throws IOException
    {
        // Create the colour scale if we haven't already done so
        if (this.isAutoScale())
        {
            this.adjustColourScaleForFrame(this.data);
        }
        BufferedImage im = this.createFrame(this.data, "");
        // Create the image type from the mime type (e.g. "image/png" gives "png")
        String imageType = this.mimeType.split("/")[1];
        ImageIO.write(im, imageType, out);
    }
    
    
}
