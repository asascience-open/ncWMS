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

package uk.ac.rdg.resc.ncwms;

import java.util.Hashtable;

/**
 * Class for parsing the query string of a WMS request. Essentially, this
 * gives a method for treating parameters as case-insensitive.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class RequestParser
{
    private Hashtable<String, String> params; // Hashtable of parameters and values
    
    /**
     * Creates a new RequestParser
     * @param queryString the query string of the URL
     */
    public RequestParser(String queryString)
    {
        this.params = new Hashtable<String, String>();
        if (queryString != null)
        {
            // Cycle through each key-value pair
            for (String kvp : queryString.split("&"))
            {
                // We should be safe doing this because the query string has come
                // from a URL: it is not an arbitrary string
                String[] kv = kvp.split("=");
                String value = kv.length > 1 ? kv[1] : "";
                // We store the key in lower case: the case of the key does not matter
                // TODO: convert URL escape codes, e.g. %2f = /
                this.params.put(kv[0].toLowerCase(), value);
            }
        }
    }
    
    /**
     * Gets the value of a parameter
     * @param paramName The name of the parameter (case insensitive)
     * @return the value of the parameter
     * @throws WMSException if there is no parameter with the given name
     */
    public String getParameterValue(String paramName) throws WMSException
    {
        String value = this.getParameterValue(paramName, null);
        if (value == null)
        {
            throw new WMSException("Must provide a " + paramName.toUpperCase()
                + " argument");
        }
        else
        {
            return value;
        }
    }
    
    /**
     * Gets the value of a parameter, or a default value if the parameter does not exist
     * @param paramName The name of the parameter (case insensitive)
     * @param defaultValue The default value, returned if there is no parameter
     * with the given name
     * @return the value of the parameter
     */
    public String getParameterValue(String paramName, String defaultValue)
    {
        String value = this.params.get(paramName.toLowerCase());
        return value == null ? defaultValue : value;
    }
    
}
