/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.calvalus.processing.productinventory;

import com.bc.calvalus.processing.JobConfigNames;
import com.bc.calvalus.processing.beam.ProductFactory;
import com.bc.calvalus.processing.productinventory.ProductInventoryEntry;
import com.bc.calvalus.processing.shellexec.ProcessorException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.io.IOException;

/**
 * A mapper that checks products for validity and generates an inventory entry.
 *
 * @author MarcoZ
 */
public class ProductInventoryMapper extends Mapper<NullWritable, NullWritable, Text /*product name*/, Text /*inventory entry CSV*/> {

    @Override
    public void run(Context context) throws IOException, InterruptedException, ProcessorException {
        Configuration jobConfig = context.getConfiguration();
        ProductFactory productFactory = new ProductFactory(jobConfig);

        final FileSplit split = (FileSplit) context.getInputSplit();
        // parse request
        Path inputPath = split.getPath();

        long length = split.getLength();
        if (length <= 12029L) {
            report(context, "Input file to small", inputPath);
            return;
        }

        Product product;
        try {
            String inputFormat = jobConfig.get(JobConfigNames.CALVALUS_INPUT_FORMAT, null);
            product = productFactory.readProduct(inputPath, inputFormat);
        } catch (IOException ioe) {
            report(context, "Failed to read product: " + ioe.getMessage(), inputPath);
            productFactory.dispose();
            return;
        }

        try {
            if (productHasEmptyTiepoints(product)) {
                report(context, product, false, "Product has empty tie-points", inputPath);
            } else if (productHasEmptyLatLonLines(product)) {
                report(context, product, false, "Product has empty lat/lon lines", inputPath);
            } else {
                report(context, product, true, "Good", inputPath);
            }
        } finally {
            product.dispose();
            productFactory.dispose();
        }
    }

    private void report(Context context, String message, Path path) throws IOException, InterruptedException {
        report(context, ProductInventoryEntry.createEmpty(message), path);
    }

    private void report(Context context, Product product, boolean good, String message, Path path) throws IOException, InterruptedException {
        ProductInventoryEntry entry;
        if (good) {
            entry = ProductInventoryEntry.createForGoodProduct(product, message);
        } else {
            entry = ProductInventoryEntry.createForBadProduct(product, message);
        }
        report(context, entry, path);
    }

    private void report(Context context, ProductInventoryEntry entry, Path path) throws IOException, InterruptedException {
        context.write(new Text(path.getName()), new Text(entry.toCSVString()));
        context.getCounter("Products", entry.getMessage()).increment(1);
    }

    private static boolean productHasEmptyTiepoints(Product sourceProduct) {
        // "AMORGOS" can produce products that are corrupted.
        // All tie point grids contain only zeros, check the first one,
        // if the product has one.
        TiePointGrid[] tiePointGrids = sourceProduct.getTiePointGrids();
        if (tiePointGrids != null && tiePointGrids.length > 0) {
            TiePointGrid firstGrid = tiePointGrids[0];
            float[] tiePoints = firstGrid.getTiePoints();
            for (float tiePoint : tiePoints) {
                if (tiePoint != 0.0f) {
                    return false;
                }
            }
            // all values are zero
            return true;
        }
        return false;
    }

    private static boolean productHasEmptyLatLonLines(Product sourceProduct) {
        TiePointGrid latitude = sourceProduct.getTiePointGrid("latitude");
        float[] latData = (float[]) latitude.getDataElems();
        TiePointGrid longitude = sourceProduct.getTiePointGrid("longitude");
        float[] lonData = (float[]) longitude.getDataElems();

        int width = latitude.getRasterWidth();
        int height = latitude.getRasterHeight();

        for (int y = 0; y < height; y++) {
            if (isLineZero(latData, width, y) && isLineZero(lonData, width, y)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLineZero(float[] floatData, int width, int y) {
        for (int x = 0; x < width; x++) {
            if (floatData[(y * width + x)] != 0) {
                return false;
            }
        }
        return true;
    }

}