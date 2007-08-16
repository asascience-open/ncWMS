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

package uk.ac.rdg.resc.ncwms.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * A Factory stores a Map of keys to class names, and can create objects of these
 * classes using the createObject(key) method.  All the classes must inherit from
 * a common superclass, T.  Factories are used to create various
 * things in the WMS that are keyed by Strings (e.g. PicMakers are keyed by
 * MIME types, Styles are keyed by the style name, Grids are keyed by the
 * CRS string, etc.)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public final class Factory<T>
{
    private static final Logger logger = Logger.getLogger(Factory.class);
    
    /**
     * Maps String keys to zero-argument Constructors of the correct class
     */
    private Map<String, Constructor<? extends T>> constructors =
        new HashMap<String, Constructor<? extends T>>();
    
    /**
     * The superclass of objects that createObject will return
     */
    private Class<T> superClass;
    
    /**
     * Creates a new Factory that can be used to create objects that extend
     * the given superclass
     */
    public Factory(Class<T> superClass)
    {
        this.superClass = superClass;
    }
    
    /**
     * @return the supported keys as a Set of Strings
     */
    public Set<String> getKeys()
    {
        return this.constructors.keySet();
    }
    
    /**
     * @return true if this Factory supports the given key
     */
    public boolean supportsKey(String key)
    {
        return this.getKeys().contains(key.trim());
    }
    
    /**
     * Creates a new object from the class given by the given key.
     * @param key The key
     * @return An object of type T, or null if the given key is not recognized.
     * @throws an {@link Exception} if the object could not be created (an
     * internal error, unlikely to happen).
     */
    public T createObject(String key) throws Exception
    {
        Constructor<? extends T> constructor = this.constructors.get(key);
        if (constructor == null) return null;
        // It's very unlikely that newInstance() will throw an error because
        // we've checked in setClasses() that the class is of the correct type.
        return constructor.newInstance();
    }
    
    /**
     * Sets the classes that can be instantiated by this Factory.  These classes
     * must all inherit from T.  Also, each class must provide a static String
     * array field called "KEYS", which defines the keys (e.g. MIME type, Style
     * name, CRS code) that will be used to identify the class.
     * @throws Exception if a supplied class does not extend the superclass, if a
     * supplied class does not have a zero-argument constructor, if the
     * zero-argument constructor is not accessible or if the class does not
     * provide a static KEYS field.
     */
    public void setClasses(List<Class<? extends T>> classes) throws Exception
    {
        for (Class<? extends T> clazz : classes)
        {
            // Spring2.0 doesn't do the necessary checks on the type of the
            // classes it injects into this method so we make sure here that the
            // class really does inherit from T.  This works on the principle of
            // catching errors early and avoids the possibility of later
            // ClassCastExceptions in createObject().
            if (!this.superClass.isAssignableFrom(clazz))
            {
                throw new Exception(clazz.getName() + " does not inherit from "
                    + this.superClass.getName());
            }
            // Get the zero-argument constructor: will throw an exception if
            // there is no zero-argument constructor, or if it is not accessible
            Constructor<? extends T> constructor = clazz.getConstructor();
            // Look for the static field KEYS in the subclass
            Field field = clazz.getDeclaredField("KEYS");
            if (field.getType() != String[].class)
            {
                throw new Exception("The KEYS field of class " + clazz.getName()
                    + " must be of type String[]");
            }
            // Get the value of the KEYS field: we pass in null because this is
            // a static field.  This will throw an exception if the field is not
            // static, or not accessible.
            try
            {
                String[] keys = (String[])field.get(null);
                for (String key : keys)
                {
                    this.constructors.put(key.trim(), constructor);
                }
            }
            catch(NullPointerException npe)
            {
                throw new Exception("The KEYS field must be a static field of class "
                    + clazz.getName());
            }
        }
    }
    
    private static class TestClass
    {
        public static final String TEST = "hello";
        //public String TEST_2 = "hello2";
    }
    
}
