package com.bc.calvalus.processing.fire.format.grid.avhrr;

import com.bc.calvalus.commons.CalvalusLogger;
import com.bc.calvalus.processing.fire.format.LcRemapping;
import com.bc.calvalus.processing.fire.format.grid.AbstractFireGridDataSource;
import com.bc.calvalus.processing.fire.format.grid.AreaCalculator;
import com.bc.calvalus.processing.fire.format.grid.SourceData;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;

import java.io.IOException;
import java.util.logging.Logger;

public class AvhrrFireGridDataSource extends AbstractFireGridDataSource {

    protected static final Logger LOG = CalvalusLogger.getLogger();

    private final Product porcProduct;
    private final Product lcProduct;
    private final Product uncProduct;
    private final int tileIndex;

    public AvhrrFireGridDataSource(Product porcProduct, Product lcProduct, Product uncProduct, int tileIndex) {
        super(1000, 7200);
        this.porcProduct = porcProduct;
        this.lcProduct = lcProduct;
        this.uncProduct = uncProduct;
        this.tileIndex = tileIndex;
    }

    @Override
    public SourceData readPixels(int x, int y) throws IOException {
        if (x == 0) {
            CalvalusLogger.getLogger().info("Reading data for line " + (y + 1) + "/80" + " for tile with index " + tileIndex);
        }
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        // target grid: 1440*720
        // source grid: 7200*3600
        // --> for a single target grid cell, read 5*5 source pixels
        SourceData data = new SourceData(5, 5);
        data.reset();

        AreaCalculator areaCalculator = new AreaCalculator(porcProduct.getSceneGeoCoding());
        Band pc = porcProduct.getBand("band_1");
        Band cl = uncProduct.getBand("band_1");
        Band lc = lcProduct.getBand("band_1");

        for (int sourceY = 0; sourceY < data.height; sourceY++) {
            for (int sourceX = 0; sourceX < data.width; sourceX++) {

                int sourcePixelIndex = getPixelIndex(x, y, sourceX, sourceY, tileIndex);

                float sourcePC = getFloatPixelValue(pc, "porcentage", sourcePixelIndex);
                int targetPixelIndex = sourceY * 5 + sourceX;
                if (isValidPixel(sourcePC)) {
                    float sourceCL = getFloatPixelValue(cl, "confidence", sourcePixelIndex) / 100.0F;
                    data.burnedPixels[targetPixelIndex] = sourcePC;
                    data.probabilityOfBurn[targetPixelIndex] = sourceCL;
                }
                int sourceLC = getIntPixelValue(lc, "landcover", sourcePixelIndex);
                data.burnable[targetPixelIndex] = LcRemapping.isInBurnableLcClass(sourceLC);
                data.lcClasses[targetPixelIndex] = sourceLC;

                if (sourcePC >= 0) { // has data -> observed pixel
                    data.statusPixels[targetPixelIndex] = 1;
                }

                int x1 = sourcePixelIndex % porcProduct.getSceneRasterWidth();
                int y1 = sourcePixelIndex / porcProduct.getSceneRasterWidth();
                int width = porcProduct.getSceneRasterWidth();
                int height = porcProduct.getSceneRasterHeight();
                data.areas[targetPixelIndex] = areaCalculator.calculatePixelSize(x1, y1, width, height);
            }
        }

        data.patchCount = -1;

        return data;
    }

    private boolean isValidPixel(float sourcePC) {
        return sourcePC != -1.0 && sourcePC != -2.0;
    }

    static int getPixelIndex(int targetX, int targetY, int sourceX, int sourceY, int tileIndex) {
        int SCALE = 5;
        int SOURCE_WIDTH = 7200;
        int TILE_WIDTH = 400;
        int TILES_PER_ROW = 18;

        int tileYOffset = (tileIndex / TILES_PER_ROW) * SOURCE_WIDTH * TILE_WIDTH;
        int tileXOffset = (tileIndex % TILES_PER_ROW) * TILE_WIDTH;
        int gridYOffset = targetY * SCALE * SOURCE_WIDTH;
        int gridXOffset = targetX * SCALE;
        int innerYOffset = sourceY * SOURCE_WIDTH;
        int innerXOffset = sourceX;

        return tileYOffset
                + tileXOffset
                + gridYOffset
                + gridXOffset
                + innerYOffset
                + innerXOffset;
    }

}