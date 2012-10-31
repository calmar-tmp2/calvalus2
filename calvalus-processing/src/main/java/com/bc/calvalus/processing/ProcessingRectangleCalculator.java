package com.bc.calvalus.processing;

import com.bc.calvalus.processing.hadoop.ProductSplit;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.hadoop.mapreduce.InputSplit;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.gpf.operators.standard.SubsetOp;

import java.awt.Rectangle;
import java.io.IOException;

/**
 * Computes the rectangle of fo the input product that should be processed.
 */
abstract class ProcessingRectangleCalculator {

    private final Geometry regionGeometry;
    private final Rectangle roiRectangle;
    private final InputSplit inputSplit;

    public ProcessingRectangleCalculator(Geometry regionGeometry, Rectangle roiRectangle, InputSplit inputSplit) {
        this.regionGeometry = regionGeometry;
        this.roiRectangle = roiRectangle;
        this.inputSplit = inputSplit;
    }

    abstract Product getProduct() throws IOException;

    /**
     * Computes the intersection between the input product and the given geometries.
     * If there is no intersection an empty rectangle will be returned.
     * If the whole product should be processed, {@code null} will be returned.
     * The pixel region will also take information
     * from the {@code ProductSplit} based on an inventory into account.
     *
     * @return The intersection, or {@code null} if no restriction is given.
     * @throws IOException
     */
    public Rectangle computeRect() throws IOException {
        Rectangle geometryRect = getGeometryAsRectangle(regionGeometry);
        Rectangle productSplitRect = getProductSplitAsRectangle();

        Rectangle pixelRegion = intersectionSafe(geometryRect, productSplitRect);
        return intersectionSafe(pixelRegion, roiRectangle);
    }

    /**
     * get rectangle from start/stop line
     */
    Rectangle getProductSplitAsRectangle() throws IOException {
        if (inputSplit instanceof ProductSplit) {
            ProductSplit productSplit = (ProductSplit) inputSplit;
            final int processStart = productSplit.getProcessStartLine();
            final int processLength = productSplit.getProcessLength();
            if (processLength > 0) {
                Product product = getProduct();
                final int width = product.getSceneRasterWidth();
                return new Rectangle(0, processStart, width, processLength);
            }
        }
        return null;
    }

    Rectangle getGeometryAsRectangle(Geometry regionGeometry) {
        if (!(regionGeometry == null || regionGeometry.isEmpty() || isGlobalCoverageGeometry(regionGeometry))) {
            try {
                return SubsetOp.computePixelRegion(getProduct(), regionGeometry, 1);
            } catch (Exception e) {
                // Computation of pixel region could fail (JTS Exception), if the geo-coding of the product is messed up
                // in this case ignore this product
                return new Rectangle();
            }
        }
        return null;
    }

    static Rectangle intersectionSafe(Rectangle r1, Rectangle r2) {
        if (r1 == null) {
            return r2;
        } else if (r2 == null) {
            return r1;
        } else {
            return r1.intersection(r2);
        }
    }

    static boolean isGlobalCoverageGeometry(Geometry geometry) {
        Envelope envelopeInternal = geometry.getEnvelopeInternal();
        return eq(envelopeInternal.getMinX(), -180.0, 1E-8)
                && eq(envelopeInternal.getMaxX(), 180.0, 1E-8)
                && eq(envelopeInternal.getMinY(), -90.0, 1E-8)
                && eq(envelopeInternal.getMaxY(), 90.0, 1E-8);
    }

    private static boolean eq(double x1, double x2, double eps) {
        double delta = x1 - x2;
        return delta > 0 ? delta < eps : -delta < eps;
    }

}
