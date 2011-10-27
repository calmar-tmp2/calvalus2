package com.bc.calvalus.portal.client;

import junit.framework.TestCase;

import java.util.Date;

import static com.bc.calvalus.portal.client.L3ConfigUtils.getPeriodCount;

public class L3ConfigUtilsTest  extends TestCase {
    public void testPeriodCount() throws Exception {
        assertEquals(1, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 5, 10),
                                       30, 10));

        assertEquals(0, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 5, 10),
                                       30, 30));

        assertEquals(1, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 5, 30),
                                       30, 10));

        assertEquals(2, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 6, 30),
                                       30, 10));

        assertEquals(3, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 7, 15),
                                       30, 10));

        assertEquals(3, getPeriodCount(new Date(2008, 5, 1),
                                       new Date(2008, 7, 30),
                                       30, 10));

        assertEquals(1, getPeriodCount(new Date(2008, 12, 1),
                                       new Date(2008, 12, 31),
                                       31, 31));
    }

    public void testTargetSize() throws Exception {
        int[] targetSize;

        targetSize = L3ConfigUtils.getTargetSizeEstimation(180, 360, 9.28);
        assertEquals(4319, targetSize[0]);
        assertEquals(2160, targetSize[1]);
    }
}
