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

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A dataset: contains a number of VariableMetadata objects.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class Dataset
{
    /**
     * The state of a Dataset
     */
    public static enum State { LOADING, READY, ERROR };
    /**
     * The datasets currently in memory: maps ids to Dataset objects
     */
    private static Hashtable<String, Dataset> datasets = new Hashtable<String, Dataset>();
    
    private String id; // Unique ID for this dataset
    private String location; // Location of this dataset (NcML file, OPeNDAP location etc)
    private Vector<VariableMetadata> vars; // Variables contained in this dataset
    private State state; // State of this dataset
    private Exception err; // Set if there is an error loading the dataset
    private DataReader dataReader; // Object used to read data and metadata
    
    
    
    /** Creates a new instance of Dataset */
    public Dataset(String id, String location)
    {
        this.id = id;
        this.setLocation(location);
        this.vars = new Vector<VariableMetadata>();
    }

    public String getId()
    {
        return this.id;
    }
    
    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
        // TODO: reload this dataset?
    }

    public Vector<VariableMetadata> getVariables()
    {
        return vars;
    }
    
    /**
     * (Re)loads the metadata for this Dataset in a new thread.
     */
    private void loadMetadata()
    {
        
    }
    
}
