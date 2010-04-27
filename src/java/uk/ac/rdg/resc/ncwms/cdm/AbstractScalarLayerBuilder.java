/*
 * Copyright (c) 2010 The University of Reading
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

package uk.ac.rdg.resc.ncwms.cdm;

import java.util.List;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.wms.AbstractScalarLayer;

/**
 * A {@link LayerBuilder} that builds subclasses of {@link AbstractScalarLayer}.
 * @author Jon
 */
public abstract class AbstractScalarLayerBuilder<L extends AbstractScalarLayer> implements LayerBuilder<L> {

    @Override
    public void setTitle(L layer, String title) {
        layer.setTitle(title);
    }

    @Override
    public void setAbstract(L layer, String abstr) {
        layer.setAbstract(abstr);
    }

    @Override
    public void setGeographicBoundingBox(L layer, GeographicBoundingBox bbox) {
        layer.setGeographicBoundingBox(bbox);
    }

    @Override
    public void setUnits(L layer, String units) {
        layer.setUnits(units);
    }

    @Override
    public void setHorizontalCoordSys(L layer, HorizontalCoordSys coordSys) {
        layer.setHorizontalCoordSys(coordSys);
    }

    @Override
    public void setElevationAxis(L layer, List<Double> zValues, boolean zPositive, String zUnits) {
        layer.setElevationValues(zValues);
        layer.setElevationPositive(zPositive);
        layer.setElevationUnits(zUnits);
    }

}
