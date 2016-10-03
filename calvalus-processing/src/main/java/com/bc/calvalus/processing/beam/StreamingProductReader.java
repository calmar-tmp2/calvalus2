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

package com.bc.calvalus.processing.beam;

import com.bc.calvalus.processing.hadoop.ByteArrayWritable;
import com.bc.ceres.core.ProgressMonitor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.dimap.DimapProductHelpers;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.image.ResolutionLevel;
import org.esa.snap.core.image.SingleBandedOpImage;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.jdom.Document;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;


public class StreamingProductReader extends AbstractProductReader {

    private enum Format {V1, V2}

    private Path path;
    private Configuration configuration;
    private Map<String, Long> keyIndex;

    private SequenceFile.Reader seqReader;
    private Format format;
    private Document dom;

    StreamingProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        Object input = getInput();
        if (input instanceof PathConfiguration) {
            PathConfiguration pathConfiguration = (PathConfiguration) input;
            this.path = pathConfiguration.getPath();
            this.configuration = pathConfiguration.getConfiguration();
        } else {
            throw new IllegalFileFormatException("input is not of the correct type.");
        }
        FileSystem fileSystem = path.getFileSystem(configuration);
        seqReader = new SequenceFile.Reader(fileSystem, path, configuration);
        Product product = readHeader();
        Band[] bands = product.getBands();
        for (Band band : bands) {
            band.setSourceImage(new BandImage(band, product.getPreferredTileSize(), seqReader, keyIndex, format));
        }
        initGeoCodings(dom, product);
        return product;
    }

    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                                       int sourceWidth, int sourceHeight,
                                                       int sourceStepX, int sourceStepY,
                                                       Band destBand,
                                                       int destOffsetX, int destOffsetY,
                                                       int destWidth, int destHeight,
                                                       ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Reading is done though BandImage.");
    }

    public void close() throws IOException {
        seqReader.close();
        keyIndex.clear();
    }

    private Product readHeader() throws IOException {
        SequenceFile.Metadata metadata = seqReader.getMetadata();
        long startPos = seqReader.getPosition();

        dom = createDOM(metadata.get(new Text("dim")));
        Product product = DimapProductHelpers.createProduct(dom);
        readTiepoints(product);

        int tileHeight;
        int tileWidth;
        if (metadata.getMetadata().containsKey(new Text("slice.height"))) {
            tileHeight = Integer.parseInt(metadata.get(new Text("slice.height")).toString());
            tileWidth = product.getSceneRasterWidth();
            format = Format.V1;
        } else {
            tileHeight = Integer.parseInt(metadata.get(new Text("tile.height")).toString());
            tileWidth = Integer.parseInt(metadata.get(new Text("tile.width")).toString());
            format = Format.V2;
        }
        product.setPreferredTileSize(tileWidth, tileHeight);

        Path indexPath = StreamingProductIndex.getIndexPath(path);
        StreamingProductIndex streamingProductIndex = new StreamingProductIndex(indexPath, configuration);
        keyIndex = streamingProductIndex.readIndex();
        if (keyIndex.isEmpty()) {
            seqReader.seek(startPos);
            keyIndex = StreamingProductIndex.buildIndex(seqReader);
            streamingProductIndex.writeIndex(keyIndex);
        }
        return product;
    }

    private Document createDOM(Text dimText) {
        final InputStream inputStream = new ByteArrayInputStream(dimText.getBytes());
        return DimapProductHelpers.createDom(inputStream);
    }

    private void readTiepoints(Product product) throws IOException {
        Text key = new Text();
        ByteArrayWritable value = new ByteArrayWritable();
        TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        for (TiePointGrid tpg : tiePointGrids) {
            String expectedKey = "tiepoint:" + tpg.getName();
            seqReader.next(key, value);
            if (!key.toString().equals(expectedKey)) {
                throw new IllegalStateException(String.format("key '%s' expected but got '%s'", expectedKey, key));
            }
            byte[] byteArray = value.getArray();
            ProductData productData = ProductData.createInstance(tpg.getDataType(), (int) tpg.getNumDataElems());

            InputStream inputStream = new ByteArrayInputStream(byteArray);
            ImageInputStream iis = new MemoryCacheImageInputStream(inputStream);

            productData.readFrom(iis);
            tpg.setData(productData);
        }
    }

    private void initGeoCodings(Document dom, Product product) {
        final GeoCoding[] geoCodings = DimapProductHelpers.createGeoCoding(dom, product);
        if (geoCodings != null) {
            if (geoCodings.length == 1) {
                product.setSceneGeoCoding(geoCodings[0]);
            } else {
                for (int i = 0; i < geoCodings.length; i++) {
                    product.getBandAt(i).setGeoCoding(geoCodings[i]);
                }
            }
        } else {
            final Band lonBand = product.getBand("longitude");
            final Band latBand = product.getBand("latitude");
            if (latBand != null && lonBand != null) {
                product.setSceneGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 6));
            }
        }
    }

    private static final class BandImage extends SingleBandedOpImage {

        private final RasterDataNode rasterDataNode;
        private final SequenceFile.Reader seqReader;
        private final Map<String, Long> keyIndex;
        private final Format format;

        private BandImage(RasterDataNode rasterDataNode, Dimension tileSize,SequenceFile.Reader seqReader, Map<String, Long> keyIndex, Format format) {
            super(ImageManager.getDataBufferType(rasterDataNode.getDataType()),
                  rasterDataNode.getRasterWidth(),
                  rasterDataNode.getRasterHeight(),
                  tileSize,
                  null,
                  ResolutionLevel.MAXRES);
            this.rasterDataNode = rasterDataNode;
            this.seqReader = seqReader;
            this.keyIndex = keyIndex;
            this.format = format;
        }

        @Override
        protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
            ProductData productData;
            boolean directMode = tile.getDataBuffer().getSize() == destRect.width * destRect.height;
            if (directMode) {
                productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                         ImageUtils.getPrimitiveArray(tile.getDataBuffer()));
            } else {
                productData = ProductData.createInstance(rasterDataNode.getDataType(),
                                                         destRect.width * destRect.height);
            }

            try {
                computeProductData(productData, destRect.x, destRect.y);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (!directMode) {
                tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, productData.getElems());
            }
        }

        private void computeProductData(ProductData productData, int x, int y) throws IOException {
            String expectedKey;
            if (format == Format.V1) {
                int sliceIndex = MathUtils.floorInt(y / tileHeight);
                expectedKey = rasterDataNode.getName() + ":" + sliceIndex;
            } else {
                expectedKey = rasterDataNode.getName() + ":" + x + ":" + y;
            }

            Text key = new Text();
            ByteArrayWritable value = new ByteArrayWritable();
            synchronized (seqReader) {
                Long keyPosition = keyIndex.get(expectedKey);
                if (keyPosition != seqReader.getPosition()) {
                    seqReader.seek(keyPosition);
                }
                seqReader.next(key, value);
            }
            if (!key.toString().equals(expectedKey)) {
                throw new IllegalStateException(String.format("key '%s' expected but got '%s'", expectedKey, key));
            }
            byte[] byteArray = value.getArray();

            InputStream inputStream = new ByteArrayInputStream(byteArray);
            ImageInputStream iis = new MemoryCacheImageInputStream(inputStream);

            productData.readFrom(iis);
        }

    }
}
