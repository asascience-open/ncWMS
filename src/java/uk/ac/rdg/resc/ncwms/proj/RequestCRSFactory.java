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

package uk.ac.rdg.resc.ncwms.proj;

import java.awt.Rectangle;
import java.util.Hashtable;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCRSException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSException;

/**
 * Factory for RequestCRS objects.
 * Interface for a requested map CRS (as opposed to a CRS
 * for source data).  Subclasses should provide a no-argument constructor.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class RequestCRSFactory
{
    // Maps projection codes to their associated RequestProjection class names
    private static Hashtable<String, String> projs;
    
    protected String code; // Unique code for this projection (e.g. CRS:84)
    protected Rectangle bbox; // Bounding box in this proj's coordinate space
    
    static
    {
        projs = new Hashtable<String, String>();
        registerCRS("CRS:84", "uk.ac.rdg.resc.ncwms.proj.ReqProjCRS84");
    }
    
    /**
     * Registers the class with the given name as being the handler for the 
     * given code. Does not check to see if className represents a valid class.
     * @param code The unique code for the projection
     * @param className The name of the class that will handle this projection
     */
    public synchronized static void registerCRS(String code, String className)
    {
        if (projs.containsKey(code))
        {
            // TODO: throw an Exception
        }
        projs.put(code, className);
    }
    
    /**
     * Gets the RequestCRS object that is specified by the given code.
     * @param code The unique code for this projection (e.g. CRS:84)
     * @return the RequestProjection object
     * @throws InvalidCRSException if the code does not represent a projection
     * that is supported by this server
     */
    public RequestCRS getRequestCRS(String code) throws WMSException
    {
        String className = projs.get(code);
        if (className == null)
        {
            throw new InvalidCRSException(code);
        }
        return null; // TODO (RequestCRS)Class.forName(className).newInstance();
    }
    
    /**
     * Sets the bounding box in this projection's coordinate system
     * @param bbox The bounding box.
     */
    public void setBoundingBox(Rectangle bbox)
    {
        this.bbox = bbox;
    }
    
    
}
