package com.bc.calvalus.processing.fire.format.grid;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.bc.calvalus.processing.fire.format.grid.GridFormatUtils.make2Dims;
import static org.junit.Assert.assertEquals;

public class AbstractFireGridDataSourceTest {

    private AbstractFireGridDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        dataSource = new AbstractFireGridDataSource() {
            @Override
            public SourceData readPixels(int x, int y) throws IOException {
                return null;
            }
        };
        dataSource.setDoyFirstHalf(1);
        dataSource.setDoyFirstOfMonth(1);
        dataSource.setDoySecondHalf(22);
        dataSource.setDoyLastOfMonth(31);
    }

    @Test
    public void testGetPatches_0() throws Exception {
        int[] pixels = {
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0
        };
        assertEquals(0, dataSource.getPatchNumbers(make2Dims(pixels), true));
    }

    @Test
    public void testGetPatches_1() throws Exception {
        int[] pixels = {
                0, 0, 0, 1,
                0, 0, 0, 1,
                1, 0, 0, 0,
                1, 0, 0, 1
        };
        assertEquals(3, dataSource.getPatchNumbers(make2Dims(pixels), true));
    }

    @Test
    public void testGetPatches_2() throws Exception {
        int[] pixels = {
                0, 0, 0, 0,
                0, 1, 0, 0,
                0, 1, 1, 0,
                0, 0, 0, 0
        };
        assertEquals(1, dataSource.getPatchNumbers(make2Dims(pixels), true));
    }

    @Test
    public void testGetPatches_3() throws Exception {
        int[] pixels = {
                1, 0, 1, 1,
                0, 1, 0, 1,
                0, 1, 1, 0,
                0, 0, 0, 1
        };
        assertEquals(4, dataSource.getPatchNumbers(make2Dims(pixels), true));
    }

    @Test
    public void testGetPatches_Large() throws Exception {
        int[] pixels = new int[90 * 90];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (int) (Math.random() * 2);
        }
        dataSource.getPatchNumbers(make2Dims(pixels), true);
    }
}