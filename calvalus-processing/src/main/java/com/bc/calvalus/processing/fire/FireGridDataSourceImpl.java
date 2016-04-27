package com.bc.calvalus.processing.fire;

import com.bc.calvalus.commons.CalvalusLogger;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Offers the standard implementation for reading pixels of arbitrary source rectangles
 *
 * @author thomas
 */
public class FireGridDataSourceImpl implements FireGridMapper.FireGridDataSource {

    private final Product sourceProduct;

    private static final Logger LOG = CalvalusLogger.getLogger();
    private int doyFirstOfMonth;
    private int doyLastOfMonth;
    private int doyFirstHalf;
    private int doySecondHalf;

    public FireGridDataSourceImpl(Product sourceProduct) {
        this.sourceProduct = sourceProduct;
    }

    @Override
    public void readPixels(Rectangle sourceRect, SourceData data) throws IOException {
        Band band = sourceProduct.getBand("band_1");
        band.readPixels(sourceRect.x, sourceRect.y, sourceRect.width, sourceRect.height, data.pixels);
        getAreas(band, data.areas);
        data.patchCountFirstHalf = getPatchNumbers(make2Dims(data.pixels), true);
        data.patchCountSecondHalf = getPatchNumbers(make2Dims(data.pixels), false);
    }

    int getPatchNumbers(int[][] pixels, boolean firstHalf) {
        int patchCount = 0;
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                if (clearObjects(pixels, i, j, firstHalf)) {
                    patchCount++;
                }
            }
        }
        return patchCount;
    }

    static int[][] make2Dims(int[] pixels) {
        int length = pixels.length;
        if ((int) (Math.sqrt(length) + 0.5) * (int) (Math.sqrt(length) + 0.5) != length) {
            throw new IllegalArgumentException();
        }
        int size = (int) Math.sqrt(length);
        int[][] result = new int[size][size];
        for (int r = 0; r < size; r++) {
            System.arraycopy(pixels, r * size, result[r], 0, size);
        }
        return result;
    }

    boolean clearObjects(int[][] array, int x, int y, boolean firstHalf) {
        if (x < 0 || y < 0 || x >= array.length || y >= array[x].length) {
            return false;
        }
        if (isBurned(array[x][y], firstHalf)) {
            array[x][y] = 0;
            clearObjects(array, x - 1, y, firstHalf);
            clearObjects(array, x + 1, y, firstHalf);
            clearObjects(array, x, y - 1, firstHalf);
            clearObjects(array, x, y + 1, firstHalf);
            return true;
        }
        return false;
    }

    @Override
    public void setDoyFirstOfMonth(int doyFirstOfMonth) {
        this.doyFirstOfMonth = doyFirstOfMonth;
    }

    @Override
    public void setDoyLastOfMonth(int doyLastOfMonth) {
        this.doyLastOfMonth = doyLastOfMonth;
    }

    @Override
    public void setDoyFirstHalf(int doyFirstHalf) {
        this.doyFirstHalf = doyFirstHalf;
    }

    @Override
    public void setDoySecondHalf(int doySecondHalf) {
        this.doySecondHalf = doySecondHalf;
    }

    private boolean isBurned(int pixel, boolean firstHalf) {
        if (firstHalf) {
            return pixel >= doyFirstOfMonth && pixel < doySecondHalf - 6 && pixel != 999 && pixel != FireGridMapper.NO_DATA;
        }
        return pixel > doyFirstHalf + 8 && pixel <= doyLastOfMonth && pixel != 999 && pixel != FireGridMapper.NO_DATA;
    }

    private static double[] getAreas(Band band, double[] areas) {
        if (band.getName().equals("null")) {
            LOG.info(String.format("Skipping band %s because it is empty.", band.getName()));
            return areas;
        }
        AreaCalculator areaCalculator = new AreaCalculator(band.getGeoCoding());
        for (int i = 0; i < areas.length; i++) {
            int sourceBandX = i % band.getRasterWidth();
            int sourceBandY = i / band.getRasterWidth();
            areas[i] = areaCalculator.calculatePixelSize(sourceBandX, sourceBandY);
        }
        return areas;
    }
}