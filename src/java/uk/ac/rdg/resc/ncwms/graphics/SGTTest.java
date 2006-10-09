/*
 * Copyright (c) 2006 The University of Reading
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

import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.swing.JPlotLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;

/**
 * Test of generating graphics with the SGT library
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class SGTTest
{
    
    public static void main(String[] args) throws Exception
    {
        long start = System.currentTimeMillis();
        JGridDemo demo = new JGridDemo();
        JPlotLayout jpl = demo.makeGraph();
        JPane jp = jpl.getKeyPane();
        System.out.println("Took " + (System.currentTimeMillis() - start) + " milliseconds");
        BufferedImage bufIm = new BufferedImage(jp.getWidth(), jp.getHeight(),
            BufferedImage.TYPE_4BYTE_ABGR);
        System.out.println("Took " + (System.currentTimeMillis() - start) + " milliseconds");
        jp.draw(bufIm.createGraphics());
        System.out.println("Took " + (System.currentTimeMillis() - start) + " milliseconds");
        ImageIO.write(bufIm, "png", new File("C:\\test.png"));
        System.out.println("Took " + (System.currentTimeMillis() - start) + " milliseconds");
    }
    
}
