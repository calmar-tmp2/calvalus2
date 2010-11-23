package com.bc.calvalus.b3.job;

import com.bc.calvalus.b3.AggregatorAverage;
import com.bc.calvalus.b3.BinningContext;
import com.bc.calvalus.b3.BinManagerImpl;
import com.bc.calvalus.b3.IsinBinningGrid;
import com.bc.calvalus.b3.ObservationImpl;
import com.bc.calvalus.b3.SpatialBin;
import com.bc.calvalus.b3.SpatialBinProcessor;
import com.bc.calvalus.b3.SpatialBinner;
import com.bc.calvalus.b3.VariableContext;
import com.bc.calvalus.experiments.util.CalvalusLogger;
import com.bc.calvalus.hadoop.io.FSImageInputStream;
import com.bc.ceres.glevel.MultiLevelImage;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.esa.beam.dataio.envisat.EnvisatProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import java.awt.Point;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads an N1 product and produces an emits (binIndex, spatialBin) pairs.
 *
 * @author Marco Zuehlke
 * @author Norman Fomferra
 */
public class L3Mapper extends Mapper<NullWritable, NullWritable, IntWritable, SpatialBin> {

    private static final Logger LOG = CalvalusLogger.getLogger();

    @Override
    public void run(Context context) throws IOException, InterruptedException {

        JAI.enableDefaultTileCache();
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(512 * 1024 * 1024); // 512 MB
        int tileHeight = context.getConfiguration().getInt(L3Tool.CONFNAME_L3_NUM_SCANS_PER_SLICE, L3Tool.DEFAULT_L3_NUM_SCANS_PER_SLICE);
        System.setProperty("beam.envisat.tileHeight", Integer.toString(tileHeight));

        FileSplit split = (FileSplit) context.getInputSplit();

        // write initial log entry for runtime measurements
        LOG.info(MessageFormat.format("{0} starts processing of split {1}",
                                      context.getTaskAttemptID(), split));
        long startTime = System.nanoTime();

        // open the single split as ImageInputStream and create a product via an Envisat product reader
        Path path = split.getPath();
        FileSystem inputFileSystem = path.getFileSystem(context.getConfiguration());
        FSDataInputStream fsDataInputStream = inputFileSystem.open(path);
        FileStatus status = inputFileSystem.getFileStatus(path);
        ImageInputStream imageInputStream = new FSImageInputStream(fsDataInputStream, status.getLen());
        int numObsTotal = 0;
        int numBinsTotal = 0;
        try {
            EnvisatProductReaderPlugIn plugIn = new EnvisatProductReaderPlugIn();
            ProductReader productReader = plugIn.createReaderInstance();

            Product product = productReader.readProductNodes(imageInputStream, null);
            VirtualBand band = new VirtualBand("ndvi", ProductData.TYPE_FLOAT32, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                               "(radiance_10 - radiance_6) / (radiance_10 + radiance_6)");
            band.setValidPixelExpression("!l1_flags.INVALID && !l1_flags.BRIGHT && l1_flags.LAND_OCEAN");
            product.addBand(band);
            GeoCoding geoCoding = product.getGeoCoding();
            MultiLevelImage maskImage = band.getValidMaskImage();
            MultiLevelImage varImage = band.getGeophysicalImage();
            assertThatImageIsSliced(product, maskImage);
            assertThatImageIsSliced(product, varImage);
            Point[] tileIndices = varImage.getTileIndices(null);

            int numBinningGridRows = context.getConfiguration().getInt(L3Tool.CONFNAME_L3_NUM_ROWS, -1);
            SpatialBinEmitter spatialBinEmitter = new SpatialBinEmitter(context);

            VariableContext variableContext = new MyVariableContext();

            SpatialBinner spatialBinner
                    = new SpatialBinner(new IsinBinningGrid(numBinningGridRows),
                                        new BinManagerImpl(new AggregatorAverage(variableContext, "ndvi")),
                                        spatialBinEmitter,
                                        tileIndices.length);

            for (Point tileIndex : tileIndices) {
                Raster varRaster = varImage.getTile(tileIndex.x, tileIndex.y);
                Raster maskRaster = maskImage.getTile(tileIndex.x, tileIndex.y);
                int sliceWidth = varRaster.getWidth();
                int sliceHeight = varRaster.getHeight();
                int numObs = 0;
                ObservationImpl[] observations = new ObservationImpl[sliceWidth * sliceHeight];
                for (int y = varRaster.getMinY(); y < varRaster.getMinY() + varRaster.getHeight(); y++) {
                    for (int x = varRaster.getMinX(); x < varRaster.getMinX() + sliceWidth; x++) {
                        // todo - use bin filter here, for each var, get from config
                        if (maskRaster.getSample(x, y, 0) != 0) {
                            GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(x + 0.5f, y + 0.5f), null);
                            observations[numObs++] = new ObservationImpl(geoPos.lat, geoPos.lon, varRaster.getSampleFloat(x, y, 0));
                        }
                    }
                }
                try {
                    spatialBinner.processObservationSlice(Arrays.copyOf(observations, numObs));
                } catch (Exception e) {
                    String m = MessageFormat.format("Failed to process slice {0} of input product {1}",
                                                    tileIndex,
                                                    product.getName());
                    LOG.log(Level.SEVERE, m, e);
                }

                numObsTotal += numObs;
                numBinsTotal += spatialBinEmitter.numBinsEmitted;
            }
        } finally {
            imageInputStream.close();
        }

        // write final log entry for runtime measurements
        long stopTime = System.nanoTime();
        LOG.info(MessageFormat.format("{0} stops processing of split {1} after {2} sec ({3} observations seen, {4} bins produced)",
                                      context.getTaskAttemptID(), split, (stopTime - startTime) / 1E9, numObsTotal, numBinsTotal));
    }

    private void assertThatImageIsSliced(Product product, MultiLevelImage image) {
        int tileWidth = image.getTileWidth();
        int sceneRasterWidth = product.getSceneRasterWidth();
        if (tileWidth != sceneRasterWidth) {
            throw new IllegalStateException(MessageFormat.format("Product not sliced: image.tileSize = {0}x{1}, product.sceneRasterSize = {2}x{3}",
                                                                 tileWidth, image.getTileHeight(), sceneRasterWidth, product.getSceneRasterHeight()));
        }
    }


    private static class SpatialBinEmitter implements SpatialBinProcessor {
        private Context context;
        int numBinsEmitted;

        public SpatialBinEmitter(Context context) {
            this.context = context;
        }

        @Override
        public void processSpatialBinSlice(BinningContext ctx, int sliceIndex, List<SpatialBin> spatialBins) throws Exception {
            for (SpatialBin spatialBin : spatialBins) {
                context.write(new IntWritable(spatialBin.getIndex()), spatialBin);
            }
            numBinsEmitted += spatialBins.size();
        }
    }

}
