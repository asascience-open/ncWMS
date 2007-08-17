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

package uk.ac.rdg.resc.ncwms.config;

import java.io.File;

/**
 * Contains information about the context of the ncWMS application, in particular
 * the location of the working directory, which will contain the configuration
 * file, metadata store and caches.  The location of this working directory
 * defaults to $HOME/.ncWMS and can be changed using WMS-servlet.xml.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NcwmsContext
{
    
    private File workingDirectory;
    
    /**
     * Creates a context based on the given directory.
     * @param workingDirectory java.io.File representing the working directory
     * @throws Exception if the directory does not exist and cannot be created
     */
    public NcwmsContext(File workingDirectory) throws Exception
    {
        createDirectory(workingDirectory);
        this.workingDirectory = workingDirectory;
    }
    
    /**
     * Creates a context based on the default directory ($HOME/.ncWMS)
     * @throws Exception if the directory does not exist and cannot be created
     */
    public NcwmsContext() throws Exception
    {
        this(new File(System.getProperty("user.home"), ".ncWMS"));
    }
    
    /**
     * @return a java.io.File representing the working directory, which is
     * guaranteed to exist and to be a directory (although not guaranteed to be empty)
     */
    public File getWorkingDirectory()
    {
        return this.workingDirectory;
    }
    
    /**
     * Creates the working directory, throwing an Exception if the working directory
     * could not be created
     */
    private static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdir();
            if (!created)
            {
                throw new Exception("Could not create working directory "
                    + dir.getPath());
            }
        }
    }
    
}
