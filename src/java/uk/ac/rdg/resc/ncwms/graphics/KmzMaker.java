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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Creates KMZ files for importing into Google Earth.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class KmzMaker extends PicMaker
{
    private static final Logger logger = Logger.getLogger(KmzMaker.class);
    /**
     * Defines the MIME types that this PicMaker supports: see Factory.setClasses()
     */
    public static final String[] KEYS = new String[]{"application/vnd.google-earth.kmz"};
    
    private static final String PICNAME = "frame";
    private static final String PICEXT  = "png";
    private static final String COLOUR_SCALE_FILENAME = "legend.png";
    
    public boolean needsLegend()
    {
        return true;
    }

    public void writeImage(List<BufferedImage> frames, String mimeType,
        OutputStream out) throws IOException
    {
        StringBuffer kml = new StringBuffer();
        
        for (int frameIndex = 0; frameIndex < frames.size(); frameIndex++)
        {
            if (frameIndex == 0)
            {
                // This is the first frame.  Add the KML header and folder metadata
                kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                kml.append(System.getProperty("line.separator"));
                kml.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">");
                kml.append("<Folder>");
                kml.append("<visibility>1</visibility>");
                kml.append("<name>" + this.layer.getDataset().getId() + ", " +
                    this.layer.getId() + "</name>");
                kml.append("<description>" + this.layer.getDataset().getTitle() + ", "
                    + this.layer.getTitle() + ": " + this.layer.getAbstract() +
                    "</description>");

                // Add the screen overlay containing the colour scale
                kml.append("<ScreenOverlay>");
                kml.append("<name>Colour scale</name>");
                kml.append("<Icon><href>" + COLOUR_SCALE_FILENAME + "</href></Icon>");
                kml.append("<overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>");
                kml.append("<screenXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>");
                kml.append("<rotationXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>");
                kml.append("<size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>");
                kml.append("</ScreenOverlay>");
            }
        
            kml.append("<GroundOverlay>");
            String timestamp = null;
            String z = null;
            if (this.tValues.get(frameIndex) != null && !this.tValues.get(frameIndex).equals(""))
            {
                // We must make sure the ISO8601 timestamp is full and includes
                // seconds, otherwise Google Earth gets confused.  This is why we
                // convert to a long and back again.
                long millisecondsSinceEpoch = WmsUtils.iso8601ToMilliseconds(this.tValues.get(frameIndex));
                timestamp = WmsUtils.millisecondsToISO8601(millisecondsSinceEpoch);
                kml.append("<TimeStamp><when>" + timestamp + "</when></TimeStamp>");
            }
            if (this.zValue != null && !this.zValue.equals("") && this.layer.getZvalues() != null)
            {
                z = "";
                if (timestamp != null) z += "<br />";
                z += "Elevation: " + this.zValue + " " + this.layer.getZunits();
            }
            kml.append("<name>");
            if (timestamp == null && z == null)
            {
                kml.append("Frame " + frameIndex);
            }
            else
            {
                kml.append("<![CDATA[");
                if (timestamp != null)
                {
                    kml.append("Time: " + timestamp);
                }
                if (z != null)
                {
                    kml.append(z);
                }
                kml.append("]]>");
            }
            kml.append("</name>");
            kml.append("<visibility>1</visibility>");

            kml.append("<Icon><href>" + getPicFileName(frameIndex) + "</href></Icon>");

            kml.append("<LatLonBox id=\"" + frameIndex + "\">");
            kml.append("<west>"  + this.bbox[0] + "</west>");
            kml.append("<south>" + this.bbox[1] + "</south>");
            kml.append("<east>"  + this.bbox[2] + "</east>");
            kml.append("<north>" + this.bbox[3] + "</north>");
            kml.append("<rotation>0</rotation>");
            kml.append("</LatLonBox>");
            kml.append("</GroundOverlay>");
        }

        // Write the footer of the KML file
        kml.append("</Folder>");
        kml.append("</kml>");
        
        ZipOutputStream zipOut = new ZipOutputStream(out);
        
        // Write the KML file: todo get filename properly
        logger.debug("Writing KML file to KMZ file");
        ZipEntry kmlEntry = new ZipEntry(this.layer.getDataset().getId() + "_" +
            this.layer.getId() + ".kml");
        kmlEntry.setTime(System.currentTimeMillis());
        zipOut.putNextEntry(kmlEntry);
        zipOut.write(kml.toString().getBytes());
        
        // Now write all the images
        int frameIndex = 0;
        logger.debug("Writing frames to KMZ file");
        for (BufferedImage frame : frames)
        {
            ZipEntry picEntry = new ZipEntry(getPicFileName(frameIndex));
            frameIndex++;
            zipOut.putNextEntry(picEntry);
            ImageIO.write(frame, PICEXT, zipOut);
        }
        
        // Finally, write the colour scale
        logger.debug("Constructing colour scale image");
        ZipEntry scaleEntry = new ZipEntry(COLOUR_SCALE_FILENAME);
        zipOut.putNextEntry(scaleEntry);
        // TODO: need handle to the Style object
        // Write the colour scale bar to the KMZ file
        logger.debug("Writing colour scale image to KMZ file");
        ImageIO.write(this.getLegend(), PICEXT, zipOut);
        
        zipOut.close();
    }
    
    /**
     * @return the name of the picture file with the given index
     */
    private static final String getPicFileName(int frameIndex)
    {
        return PICNAME + frameIndex + "." + PICEXT;
    }
}
