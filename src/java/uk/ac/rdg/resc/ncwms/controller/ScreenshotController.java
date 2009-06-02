/*
 * Copyright (c) 2009 The University of Reading
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

package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;

/**
 * Controller for generating screenshots from the Godiva2 site.
 * @author Abdul Rauf Butt
 * @author Jon Blower
 */
public class ScreenshotController extends MultiActionController
{
    private static final Logger log = LoggerFactory.getLogger(ScreenshotController.class);

    /** We only need one random number generator */
    private static final Random RANDOM = new Random();

    /** This is set by Spring so that we can find the location of the working directory */
    protected NcwmsContext ncwmsContext;

    /** Directory where the screenshots will be stored (full path) */
    private File screenshotCache;

    /**
     * Called by Spring to initialize the controller: this method creates a
     * directory for screenshots in the ncWMS working directory.
     * @throws Exception if the directory for the screenshots could not be created.
     */
    public void init() throws Exception
    {
        this.screenshotCache = new File(ncwmsContext.getWorkingDirectory(), "screenshots");
        if (this.screenshotCache.exists())
        {
            if (this.screenshotCache.isDirectory())
            {
                log.debug("Screenshots directory already exists");
            }
            else
            {
                throw new Exception(this.screenshotCache.getPath() + " already exists but is not a directory");
            }
        }
        else
        {
            if (this.screenshotCache.mkdirs())
            {
                log.debug("Screenshots directory " + this.screenshotCache.getPath()
                    + " created");
            }
            else
            {
                throw new Exception("Screenshots directory " + this.screenshotCache.getPath()
                    + " could not be created");
            }
        }
    }

    /** Simple class to hold a bounding box */
    private static final class BoundingBox
    {
        private float minX, maxX, minY, maxY;
        @Override public String toString()
        {
            return minX + "," + minY + "," + maxX + "," + maxY;
        }
    }

    /**
     * Creates a screenshot, saves it on the server and returns the URL to the
     * screenshot.
     * @param request
     * @param response
     * @return
     * @throws java.lang.Exception
     */
    public void createScreenshot(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        log.debug("Called createScreenshot");
        
        ServletOutputStream out = response.getOutputStream();

        try {

		String title = request.getParameter("title"); //"Hello World";
		String time = request.getParameter("time"); //"null";
		String elevation = request.getParameter("elevation"); //"null";
		String upperValue = request.getParameter("upperValue"); //1.0967412;
        String twoThirds = request.getParameter("twoThirds");
        String oneThird = request.getParameter("oneThird");
		String lowerValue = request.getParameter("lowerValue"); //-0.9546131;


		String BGparam = request.getParameter("urlBG");
        String FGparam = request.getParameter("urlFG"); //"http://localhost:8084/ncWMS/wms?LAYERS=OSTIA%2Fanalysed_sst&ELEVATION=0&TIME=2009-04-26T12%3A00%3A00.000Z&TRANSPARENT=true&STYLES=BOXFILL%2Frainbow&CRS=EPSG%3A4326&COLORSCALERANGE=268.48398%2C305.79602&NUMCOLORBANDS=254&LOGSCALE=false&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&EXCEPTIONS=XML&FORMAT=image%2Fpng&BBOX=-180,-90,0,90&WIDTH=256&HEIGHT=256";
        String Paletteparam = request.getParameter("urlPalette");

        if(BGparam == null || FGparam == null || Paletteparam == null) return; // TODO

        String urlStringBG = URLDecoder.decode(BGparam, "UTF-8");
        String urlStringFG = "http://localhost:8084/ncWMS/" + URLDecoder.decode(FGparam, "UTF-8");

        // Find the bounding box of the image
        BoundingBox bbox = getBoundingBox(urlStringBG);

        String[] serverName = urlStringBG.split("\\?");
        StringBuffer result = buildURL(serverName[1], serverName[0]);
        serverName = urlStringFG.split("\\?");
        StringBuffer resultFG = buildURL(serverName[1], serverName[0]);

        float minX1 = 0;
        float minX2 = 0;
        float maxX1 = 0;
        float maxX2 = 0;
        int WIDTH_OF_BG_IMAGE1 = 0;  // 340 RHS one
        int WIDTH_OF_BG_IMAGE2 = 0;  // 512 LHS one
        int START_OF_IMAGE3 = 0;
        int START_OF_IMAGE4 = 0;
        final int WIDTH_TOTAL = 512;
        String URL1 = "";
        String URL2 = "";
        float coverage = 0;

        boolean isGT180 = false;
        boolean isReplicate = false;

        String bboxParam = "&BBOX=" + bbox.toString();

        if( (Float.compare(bbox.minX,-180)<0 ))// || (Float.compare(maxXValue,180.0)>0) ) // means we need to generate two URLs
		{
            minX1 = -180; //minXValue;
            if (Float.compare(bbox.maxX,180) > 0) // It will only happen for the case of zoom out: when maxX > 180
            {
                maxX1 = bbox.maxX - 360;
                isReplicate = true;
            }
            else{
                maxX1 = bbox.maxX;
            }
            minX2 = bbox.minX + 360;
            maxX2 = +180;

            float rangeofImg1 =  Math.abs(maxX1 - minX1);
            float rangeofImg2 =  Math.abs(maxX2 - minX2);
            float totalSpan = rangeofImg1 + rangeofImg2;

            // in normal viewing case, the span is 360
            // with first zoom-in, the span becomes 180
            // with first zoom out, the spam becoms 720
            if (isReplicate) coverage =  (rangeofImg1/(totalSpan*2));
            else coverage =  (rangeofImg1/totalSpan);

            WIDTH_OF_BG_IMAGE1 = Math.round(((float) (WIDTH_TOTAL)*coverage));   // RHS Image
            if (isReplicate)
            {
                WIDTH_OF_BG_IMAGE2 =  (WIDTH_TOTAL/2) - WIDTH_OF_BG_IMAGE1;
                START_OF_IMAGE3 = WIDTH_OF_BG_IMAGE1 + WIDTH_OF_BG_IMAGE2;
                START_OF_IMAGE4 = START_OF_IMAGE3 + WIDTH_OF_BG_IMAGE2;
            }
            else{
                WIDTH_OF_BG_IMAGE2 =  WIDTH_TOTAL - WIDTH_OF_BG_IMAGE1;          // LHS Image
            }

            String bboxParam1 = "&BBOX=" + minX1 + "," + bbox.minY + "," + maxX1 + "," + bbox.maxY;
            String bboxParam2 = "&BBOX=" + minX2 + "," + bbox.minY + "," + maxX2 + "," + bbox.maxY;

            URL1 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE1 + "&HEIGHT=384" + bboxParam1;
            URL2 = result.toString() + "WIDTH=" + WIDTH_OF_BG_IMAGE2 + "&HEIGHT=384" + bboxParam2;
            isGT180 = true;
        }
        else
        {
            URL1 = result.toString() + "WIDTH=512&HEIGHT=384" + bboxParam;
        }

        String URL3 = resultFG.toString() + "WIDTH=512&HEIGHT=384" + bboxParam;

		BufferedImage bimgBG1 = null;
        BufferedImage bimgBG2 = null;

		BufferedImage bimgFG = null;
		BufferedImage bimgPalette = null;

        if(isGT180){
            bimgBG1 = downloadImage(URL1); //(path[0]);  // right-hand side
            bimgBG2 = downloadImage(URL2); //(path[1]);  // left-hand side
        }
        else{
            bimgBG1 = downloadImage(URL1);
        }
        bimgFG = downloadImage(URL3);
        bimgPalette = downloadImage(Paletteparam);//(path[2]);

        /* Prepare the final Image */
        int w = bimgBG1.getWidth();
        int h = bimgBG1.getHeight();
        int type = BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(w + 550, h + 550, type);
        Graphics2D g = image.createGraphics();

        // The Font and Text
        Font font = new Font("SansSerif", Font.BOLD, 12);
        g.setFont(font);
        g.setBackground(Color.white);
        g.fillRect(0, 0, w+550, h+550);

        g.setColor(Color.black);
        g.drawString("Title: " + title, 0, 10);
        g.drawString("Time : " + time, 0, 30);
        g.drawString("Elevation : " + elevation, 0, 50);

        // Now draw the image
        if(isGT180){
            g.drawImage(bimgBG1, null, WIDTH_OF_BG_IMAGE2, 60);
            g.drawImage(bimgBG2, null, 0, 60);
            if(isReplicate)
            {
                g.drawImage(bimgBG2, null, START_OF_IMAGE3, 60);
                g.drawImage(bimgBG1, null, START_OF_IMAGE4, 60);
            }
        }
        else{
            g.drawImage(bimgBG1, null, 0, 60);
        }
        g.drawImage(bimgFG, null, 0, 60);
        g.drawImage(bimgPalette, null, WIDTH_TOTAL, 60);

        g.drawString(upperValue, 525, 55);
        g.drawString(twoThirds, 560, 160);
        g.drawString(oneThird, 560, 365);
        g.drawString(lowerValue, 525, 470);

        g.dispose();

        // write the image to a file in the screenshots directory
        String imageName = RANDOM.nextLong() + ".png";
        File imageSrcFinal = new File(this.screenshotCache, imageName);
        log.debug("Writing screenshot to {}", imageSrcFinal.getPath());
        ImageIO.write(image, "png", imageSrcFinal);	// write the image to File Output stream
        out.print("http://localhost:8084/ncWMS/screenshots/getScreenshot?img="+ imageName);

        } finally {
            out.close();
        }
    }

    private static BoundingBox getBoundingBox(String urlStr) throws MalformedURLException
    {
        URL url = new URL(urlStr);
        for (String param : url.getQuery().split("&"))
        {
            if(param.toUpperCase().startsWith("BBOX"))
            {
                BoundingBox bbox = new BoundingBox();
                String bbValues = param.substring(5); // to remove BBOX= from the start of the string
                String[] bboxEls = bbValues.split(",");
                bbox.minX = (float) Double.parseDouble(bboxEls[0]);
                bbox.maxX = (float) Double.parseDouble(bboxEls[2]);
                bbox.minY = (float) Double.parseDouble(bboxEls[1]);
                bbox.maxY = (float) Double.parseDouble(bboxEls[3]);
                return bbox;
            }
        }
        throw new IllegalArgumentException(urlStr + " does not contain a bounding box");
    }

    private static StringBuffer buildURL(String url, String serverName) {

        String[] params = url.split("&");
        StringBuffer result = new StringBuffer();
        result.append(serverName);
        result.append("?");
        String separator = "&";
        for (int i=0; i< params.length; i++){
            result.append(params[i]);
            result.append(separator);
        }
        return result;
    }

	private static BufferedImage downloadImage(String path) throws IOException {
	    return ImageIO.read(new URL(path));
	}

    /**
     * Downloads a screenshot from the server
     * @param request
     * @param response
     */
    public void getScreenshot(HttpServletRequest request, HttpServletResponse response)
        throws Exception
    {
        log.debug("Called getScreenshot with params {}", request.getParameterMap());
        String imageName = request.getParameter("img");
        if (imageName == null) throw new Exception("Must give a screenshot image name");
        File screenshotFile = new File(this.screenshotCache, imageName);
        InputStream in = null;
        OutputStream out = null;
        try
        {
            in = new FileInputStream(screenshotFile);
            byte[] imageBytes = new byte[1024]; // read 1MB at a time
            response.setContentType("image/png");
            out = response.getOutputStream();
            int n;
            do
            {
                n = in.read(imageBytes);
                if (n >= 0)
                {
                    out.write(imageBytes);
                }
            } while (n >= 0);
        }
        catch (FileNotFoundException fnfe)
        {
            // rethrow this exception
            throw new Exception(imageName + " not found");
        }
        finally
        {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
     * Called by Spring to inject the context object
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }

}
