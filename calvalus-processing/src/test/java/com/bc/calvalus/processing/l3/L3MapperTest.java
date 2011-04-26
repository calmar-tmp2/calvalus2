/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.calvalus.processing.l3;

import com.bc.calvalus.binning.aggregators.AggregatorAverage;
import com.bc.calvalus.binning.BinManager;
import com.bc.calvalus.binning.BinManagerImpl;
import com.bc.calvalus.binning.BinningContext;
import com.bc.calvalus.binning.BinningContextImpl;
import com.bc.calvalus.binning.IsinBinningGrid;
import com.bc.calvalus.binning.SpatialBin;
import com.bc.calvalus.binning.SpatialBinProcessor;
import com.bc.calvalus.binning.SpatialBinner;
import com.bc.calvalus.binning.VariableContextImpl;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class L3MapperTest {
    @Test
    public void testThatProductMustHaveAGeoCoding() {
        BinningContext ctx = createValidCtx();

        try {
            MySpatialBinProcessor mySpatialBinProcessor = new MySpatialBinProcessor();
            L3Mapper.processProduct(new Product("p", "t", 32, 256), ctx, new SpatialBinner(ctx, mySpatialBinProcessor), new float[]{0.5f});
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void testProcessProduct() {

        BinningContext ctx = createValidCtx();
        Product product = new Product("p", "t", 32, 256);
        final TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{+40f, +40f, -40f, -40f});
        final TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0f, 0f, 32f, 256f,
                                                  new float[]{-80f, +80f, -80f, +80f});
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setGeoCoding(new TiePointGeoCoding(lat, lon));
        product.setPreferredTileSize(32, 16);

        MySpatialBinProcessor mySpatialBinProcessor = new MySpatialBinProcessor();
        L3Mapper.processProduct(product, ctx, new SpatialBinner(ctx, mySpatialBinProcessor), new float[]{0.5f});
        assertEquals(32 * 256, mySpatialBinProcessor.numObs);
    }

    private static BinningContext createValidCtx() {
        VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.setMaskExpr("!invalid");
        variableContext.defineVariable("invalid", "0");
        variableContext.defineVariable("a", "2.4");
        variableContext.defineVariable("b", "1.8");

        IsinBinningGrid binningGrid = new IsinBinningGrid(6);
        BinManager binManager = new BinManagerImpl(new AggregatorAverage(variableContext, "a", null, null),
                                                   new AggregatorAverage(variableContext, "b", null, null));

        return new BinningContextImpl(binningGrid, variableContext, binManager);
    }

    private static class MySpatialBinProcessor implements SpatialBinProcessor {
        int numObs;

        @Override
        public void processSpatialBinSlice(BinningContext ctx, List<SpatialBin> spatialBins) throws Exception {
            // System.out.println("spatialBins = " + Arrays.toString(spatialBins.toArray()));
            for (SpatialBin spatialBin : spatialBins) {
                assertEquals(2.4f, spatialBin.getFeatureValues()[0], 0.01f);  // mean of a
                assertEquals(1.8f, spatialBin.getFeatureValues()[2], 0.01f);  // mean of b
                numObs += spatialBin.getNumObs();
            }
        }
    }
}
