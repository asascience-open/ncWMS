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

package uk.ac.rdg.resc.ncwms.config;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.load.PersistenceException;
import org.simpleframework.xml.load.Validate;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * Contains fields that can be filled in to override automatically-detected
 * values.
 * @author Jon
 */
@Root(name="variable")
public class Variable
{
    @Attribute(name="id")
    private String id;

    @Attribute(name="title", required=false)
    private String title = null;

    @Attribute(name="colorScaleRange", required=false)
    private String colorScaleRangeStr = null; // comma-separated pair of floats

    private Dataset dataset;

    private float[] colorScaleRange = null;

    /**
     * Checks that the information in the XML is valid: specifically, checks
     * the colorScaleRange attribute.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        if (colorScaleRangeStr != null)
        {
            String[] els = colorScaleRangeStr.split(",");
            if (els.length != 2)
            {
                throw new PersistenceException("Invalid colorScaleRange attribute for variable " + this.id);
            }
            try
            {
                float min = Float.parseFloat(els[0]);
                float max = Float.parseFloat(els[1]);
                if (max <= min)
                {
                    throw new PersistenceException("Invalid colorScaleRange attribute for variable " + this.id);
                }
                this.colorScaleRange = new float[]{min, max};
            }
            catch(NumberFormatException nfe)
            {
                throw new PersistenceException("Invalid colorScaleRange attribute for variable " + this.id);
            }
        }
    }

    /**
     * Gets the ID of this variable, which is unique within the containing
     * {@link Dataset} and corresponds with {@link Layer#getId()}.
     * @return
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the title that the administrator has set for this variable,
     * or null if no title has been set.
     * @return
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Dataset getDataset() {
        return dataset;
    }

    void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Gets the default colour scale range for this variable, or null if not
     * set.
     * @return
     */
    public float[] getColorScaleRange()
    {
        return this.colorScaleRange;
    }

    public void setColorScaleRange(float[] colorScaleRange)
    {
        if (colorScaleRange == null)
        {
            this.colorScaleRange = null;
            this.colorScaleRangeStr = null;
        }
        else if (colorScaleRange.length != 2)
        {
            throw new IllegalArgumentException("Colorscalerange must have two elements");
        }
        else if (colorScaleRange[0] >= colorScaleRange[1])
        {
            throw new IllegalArgumentException("Invalid color scale range");
        }
        else
        {
            this.colorScaleRange = colorScaleRange;
            this.colorScaleRangeStr = colorScaleRange[0] + "," + colorScaleRange[1];
        }
    }

}
