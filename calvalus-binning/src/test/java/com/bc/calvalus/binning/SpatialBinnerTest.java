package com.bc.calvalus.binning;


import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class SpatialBinnerTest {
    static final int SUM_X = 0;
    static final int SUM_XX = 1;
    static final int WEIGHT = 2;

    @Test
    public void testThatObservationsAreAggregated() throws Exception {

        MyBinningGrid binningGrid = new MyBinningGrid();
        MyVariableContext variableContext = new MyVariableContext("x");
        MyBinManager binManager = new MyBinManager(new AggregatorAverageML(variableContext, "x", null, null));
        BinningContextImpl binningContext = new BinningContextImpl(binningGrid, variableContext, binManager);
        TemporalBinner temporalBinner = new TemporalBinner(binManager);
        SpatialBinner spatialBinner = new SpatialBinner(binningContext,
                                                        temporalBinner);

        spatialBinner.processObservationSlice(new ObservationImpl(0, 1.1, 1.1f),
                                              new ObservationImpl(0, 1.1, 1.2f),
                                              new ObservationImpl(0, 2.1, 1.3f),
                                              new ObservationImpl(0, 2.1, 1.4f));

        spatialBinner.processObservationSlice(new ObservationImpl(0, 1.1, 2.1f),
                                              new ObservationImpl(0, 2.1, 2.2f),
                                              new ObservationImpl(0, 2.1, 2.3f),
                                              new ObservationImpl(0, 2.1, 2.4f),
                                              new ObservationImpl(0, 3.1, 2.5f));

        spatialBinner.complete();

        Map<Long,TemporalBin> binMap = temporalBinner.binMap;
        assertEquals(3, binMap.size());

        TemporalBin tbin1 = binMap.get(1L);
        assertNotNull(tbin1);
        assertEquals(3, tbin1.getNumObs());
        assertEquals(1, tbin1.getNumPasses());
        Vector agg1 = binManager.getTemporalVector(tbin1, 0);
        assertNotNull(agg1);
        assertEquals(sqrt(3),
                     agg1.get(WEIGHT), 1e-5);
        assertEquals((log(1.1) + log(1.2) + log(2.1)) / sqrt(3),
                     agg1.get(SUM_X), 1e-5);

        TemporalBin tbin2 = binMap.get(2L);
        assertNotNull(tbin2);
        assertEquals(5, tbin2.getNumObs());
        assertEquals(1, tbin2.getNumPasses());
        Vector agg2 = binManager.getTemporalVector(tbin2, 0);
        assertNotNull(agg2);
        assertEquals(sqrt(5),
                agg2.get(WEIGHT), 1e-5);
        assertEquals((log(1.3) + log(1.4) + log(2.2) + log(2.3) + log(2.4)) / sqrt(5),
                     agg2.get(SUM_X), 1e-5);

        TemporalBin tbin3 = binMap.get(3L);
        assertNotNull(tbin3);
        assertEquals(1, tbin3.getNumObs());
        assertEquals(1, tbin3.getNumPasses());
        Vector agg3 = binManager.getTemporalVector(tbin3, 0);
        assertNotNull(agg3);
        assertEquals(1.0,
                     agg3.get(WEIGHT), 1e-10);
        assertEquals(log(2.5),
                     agg3.get(SUM_X), 1e-5);
    }

    @Test
    public void testThatCellsAreDeterminedCorrectly() throws Exception {
        IsinBinningGrid binningGrid = new IsinBinningGrid();

        // bin size in degree
        double binEdgeSize = 180.0 / binningGrid.getNumRows();

        // we want 4 x 4 pixels per bin
        int pixelsPerBinEdge = 4;

        // we want 4 x 4 pixels per bin
        double pixelEdgeSize = binEdgeSize / pixelsPerBinEdge;

        // we want w x h pixels total --> num bins expected = w x h / (pixelsPerBinEdge*pixelsPerBinEdge)
        int w = 16;
        int h = 16;

        double rotAngle = 0.0;
        double lonRange = w * pixelEdgeSize;
        double latRange = h * pixelEdgeSize;
        ObservationImpl[][] pixelSlices = new ObservationImpl[h][w];
        AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(rotAngle));
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                double lat = 0.5f * pixelEdgeSize + latRange * ((double) j / (double) h - 0.5);
                double lon = 0.5f * pixelEdgeSize + lonRange * ((double) i / (double) w - 0.5);
                double[] srcPts = new double[]{lat, lon};
                double[] dstPts = new double[2];
                at.transform(srcPts, 0, dstPts, 0, 1);
                pixelSlices[j][i] = new ObservationImpl(dstPts[0], dstPts[1]);
            }
        }

        MyVariableContext variableContext = new MyVariableContext("x");
        MyBinManager binManager = new MyBinManager(new AggregatorAverageML(variableContext, "x", null, null));
        MySpatialBinProcessor spatialBinProcessor = new MySpatialBinProcessor();
        BinningContextImpl binningContext = new BinningContextImpl(binningGrid, variableContext, binManager);

        SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinProcessor);

        for (ObservationImpl[] pixelSlice : pixelSlices) {
            spatialBinner.processObservationSlice(pixelSlice);
        }
        spatialBinner.complete();

        ArrayList<SpatialBin> producedSpatialBins = binManager.producedSpatialBins;

        int numObs = w * h;
        assertEquals(256, numObs);
        assertEquals(256, spatialBinProcessor.numObservationsTotal);
        for (int i = 0; i < 16; i++) {
            assertEquals(String.format("Problem with bin[%d]", i), 16, producedSpatialBins.get(i).getNumObs());
        }

        int numBins = w * h / (pixelsPerBinEdge * pixelsPerBinEdge);
        assertEquals(16, numBins);
        assertEquals(16, producedSpatialBins.size());
        int[] expectedIndexes = new int[]{
                2976689, 2976690, 2976691, 2976692,
                2972369, 2972370, 2972371, 2972372,
                2968049, 2968050, 2968051, 2968052,
                2963729, 2963730, 2963731, 2963732,
        };
        for (int i = 0; i < 16; i++) {
            assertEquals(String.format("Problem with bin[%d]", i), expectedIndexes[i], producedSpatialBins.get(i).getIndex());
        }
    }

    private static class MySpatialBinProcessor implements SpatialBinProcessor {
        int numObservationsTotal;
        boolean verbous = false;
        int sliceIndex;

        @Override
        public void processSpatialBinSlice(BinningContext ctx, List<SpatialBin> sliceBins) {
            if (verbous) {
                // Sort for better readability
                Collections.sort(sliceBins, new BinComparator());
                System.out.println("Slice  " + sliceIndex + " =================");
            }
            for (SpatialBin bin : sliceBins) {
                if (verbous) {
                    System.out.println("  writing " + bin.getIndex() + " with " + bin.getNumObs() + " obs.");
                }
                numObservationsTotal += bin.getNumObs();
            }
            sliceIndex++;
        }
    }

    private static class BinComparator implements Comparator<SpatialBin> {
        @Override
        public int compare(SpatialBin b1, SpatialBin b2) {
            return (int) (b1.getIndex() - b2.getIndex());
        }
    }
}
