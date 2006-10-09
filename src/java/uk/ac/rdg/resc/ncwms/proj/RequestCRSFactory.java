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
import java.util.Set;
import uk.ac.rdg.resc.ncwms.WMS;
import uk.ac.rdg.resc.ncwms.exceptions.ConfigurationException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCRSException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;

/**
 * Factory for RequestCRS objects.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class RequestCRSFactory
{
    // Maps projection codes to their associated RequestProjection classes
    private static Hashtable<String, Class> projs = new Hashtable<String, Class>();
    
    protected String code; // Unique code for this projection (e.g. CRS:84)
    protected Rectangle bbox; // Bounding box in this proj's coordinate space
    
    static
    {
        try
        {
            // Add the known CRSs (these can be added to and overridden by registerCRS())
            registerCRS(WMS.CRS_84, RequestCRS_CRS84.class);
        }
        catch(ConfigurationException ce)
        {
            // Should not happen since we know that the built-in CRSs are
            // RequestCRSs
            throw new ExceptionInInitializerError(ce);
        }
    }
    
    /**
     * @return a set of codes for the supported request CRSs
     */
    public static Set<String> getSupportedCRSCodes()
    {
        return projs.keySet();
    }
    
    /**
     * Registers the class with the given name as being the handler for the 
     * CRS with the given code.  This will override any previous handlers for
     * this CRS code, allowing users to plug in their own handlers if required.
     * Does not check to see if className represents a valid class.
     * @param code The unique code for the projection
     * @param className The name of the class that will handle this projection
     * @throws ClassNotFoundException if a class called className could not be
     * found.
     * @throws ConfigurationException if the class was found but it is not a
     * subclass of {@link RequestCRS}
     */
    public synchronized static void registerCRS(String code, String className)
        throws ClassNotFoundException, ConfigurationException
    {
        registerCRS(code, Class.forName(className));
    }
    
    /**
     * Registers the given class as being the handler for the CRS with the
     * given code.  This will override any previous handlers for
     * this CRS code, allowing users to plug in their own handlers if required.
     * Does not check to see if className represents a valid class.
     * @param code The unique code for the projection
     * @param theClass The class that will handle this projection
     * @throws ConfigurationException if the given class is not a subclass
     * of {@link RequestCRS}
     */
    public synchronized static void registerCRS(String code, Class theClass)
        throws ConfigurationException
    {
        if (RequestCRS.class.isAssignableFrom(theClass))
        {
            projs.put(code, theClass);
        }
        else
        {
            throw new ConfigurationException(theClass.getName() +
                " is not a subclass of RequestCRS");
        }
    }
    
    /**
     * Gets the {@link RequestCRS} object that is specified by the given code.
     * Called by the {@link GetMap} operation.
     * @param code The unique code for this projection (e.g. CRS:84)
     * @return the RequestProjection object
     * @throws {@link InvalidCRSException} if the code does not represent a projection
     * that is supported by this server
     * @throws {@link WMSInternalError} if the {@link RequestCRS} object could not be
     * created (e.g. constructor was private)
     */
    public static RequestCRS getRequestCRS(String code)
        throws InvalidCRSException, WMSInternalError
    {
        Class theClass = projs.get(code);
        if (theClass == null)
        {
            throw new InvalidCRSException(code);
        }
        try
        {
            return (RequestCRS)theClass.newInstance();
        }
        catch(Exception e)
        {
            // Could be an InstantiationException or IllegalAccessException
            throw new WMSInternalError("Could not create the a RequestCRS object" +
                " for CRS " + code + ": " + e.getMessage(), e);
        }
        // ClassCastExceptions should not happen because we have checked in
        // registerCRS that the RequestCRS class is of the correct type
    }
    
}
