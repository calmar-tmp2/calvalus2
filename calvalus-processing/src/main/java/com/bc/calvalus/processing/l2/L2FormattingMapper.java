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

package com.bc.calvalus.processing.l2;

import com.bc.calvalus.commons.CalvalusLogger;
import com.bc.calvalus.processing.JobConfigNames;
import com.bc.calvalus.processing.ProcessorAdapter;
import com.bc.calvalus.processing.analysis.QLMapper;
import com.bc.calvalus.processing.analysis.Quicklooks;
import com.bc.calvalus.processing.beam.BeamProcessorAdapter;
import com.bc.calvalus.processing.beam.SubsetProcessorAdapter;
import com.bc.calvalus.processing.hadoop.ProductSplitProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A mapper which converts L2 products from the
 * (internal) SequenceFiles into different BEAM product formats.
 */
public class L2FormattingMapper extends Mapper<NullWritable, NullWritable, NullWritable, NullWritable> {

    private static final String COUNTER_GROUP_NAME_PRODUCTS = "Products";
    private static final Logger LOG = CalvalusLogger.getLogger();

    @Override
    public void run(Mapper.Context context) throws IOException, InterruptedException {
        // todo - replace by an IdentityAdapter or something similar
        ProcessorAdapter processorAdapter = new SubsetProcessorAdapter(context);
        ProgressMonitor pm = new ProductSplitProgressMonitor(context);
        pm.beginTask("Level 2 format", 100);
        try {
            Configuration jobConfig = context.getConfiguration();
            Path inputPath = processorAdapter.getInputPath();
            String productName = getProductName(jobConfig, inputPath.getName());

            String format = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_FORMAT);
            String compression = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_COMPRESSION);
            ProductFormatter productFormatter = new ProductFormatter(productName, format, compression);
            String outputFilename = productFormatter.getOutputFilename();
            String outputFormat = productFormatter.getOutputFormat();

            if (jobConfig.getBoolean(JobConfigNames.CALVALUS_RESUME_PROCESSING, false)) {
                Path outputProductPath = new Path(FileOutputFormat.getOutputPath(context), outputFilename);
                if (FileSystem.get(jobConfig).exists(outputProductPath)) {
                    context.getCounter(COUNTER_GROUP_NAME_PRODUCTS, "Product exist").increment(1);
                    LOG.info("resume: target product already exist, skip processing");
                    return;
                }
            }
            Product targetProduct = processorAdapter.getInputProduct();

            if (!isProductEmpty(context, targetProduct)) {
                targetProduct = doReprojection(jobConfig, targetProduct);
                Map<String, Object> geoSubsetParameter = createGeoSubsetParameter(jobConfig);
                if (!geoSubsetParameter.isEmpty()) {
                    targetProduct = GPF.createProduct("Subset", geoSubsetParameter, targetProduct);
                }
                BeamProcessorAdapter.copyTimeCoding(processorAdapter.getInputProduct(), targetProduct);

                try {
                    context.setStatus("Quicklooks");
                    if (jobConfig.getBoolean(JobConfigNames.CALVALUS_OUTPUT_QUICKLOOKS, false)) {
                        if (jobConfig.get(JobConfigNames.CALVALUS_QUICKLOOK_PARAMETERS) != null) {
                            writeQuicklooks(context, jobConfig, productName, targetProduct);
                        } else {
                            LOG.warning("Missing parameters for quicklook generation.");
                        }
                    }
                    pm.worked(5);

                    context.setStatus("Writing");
                    targetProduct = writeProductFile(targetProduct, productFormatter, context, jobConfig,
                                                     outputFormat, pm);
                    pm.worked(10);

                    context.setStatus("Metadata");
                    if (jobConfig.get(JobConfigNames.CALVALUS_METADATA_TEMPLATE) != null) {
                        writeMetadatafile(targetProduct, inputPath, outputFilename, context);
                        LOG.info("Finished creating metadata.");
                    }
                    pm.worked(5);
                } finally {
                    context.setStatus("");
                    productFormatter.cleanupTempDir();
                }
            }
        } finally {
            pm.done();
            processorAdapter.dispose();
        }
    }

    private void writeMetadatafile(Product targetProduct, Path inputPath, String outputFilename,
                                   Mapper.Context context) throws IOException, InterruptedException {
        LOG.info("Creating metadata.");
        Path outputPath = new Path(FileOutputFormat.getWorkOutputPath(context), outputFilename);
        L2Mapper.processMetadata(context,
                                 inputPath.toString(), targetProduct,
                                 outputPath.toString(), targetProduct);
    }

    private Product writeProductFile(Product targetProduct, ProductFormatter productFormatter, Mapper.Context context,
                                     Configuration jobConfig, String outputFormat, ProgressMonitor pm) throws
                                                                                                       IOException {
        Map<String, Object> bandSubsetParameter = createBandSubsetParameter(jobConfig);
        if (!bandSubsetParameter.isEmpty()) {
            targetProduct = GPF.createProduct("Subset", bandSubsetParameter, targetProduct);
        }

        File productFile = productFormatter.createTemporaryProductFile();
        LOG.info("Start writing product to file: " + productFile.getName());

        ProductIO.writeProduct(targetProduct, productFile, outputFormat, false,
                               SubProgressMonitor.create(pm, 80));
        LOG.info("Finished writing product.");

        context.setStatus("Copying");
        productFormatter.compressToHDFS(context, productFile);
        context.getCounter(COUNTER_GROUP_NAME_PRODUCTS, "Product formatted").increment(1);
        return targetProduct;
    }

    private void writeQuicklooks(Mapper.Context context, Configuration jobConfig, String productName,
                                 Product targetProduct) {
        LOG.info("Creating quicklooks.");
        String outputBandList = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_BANDLIST);
        String[] bandNames = outputBandList.split(",");
        for (String bandName : bandNames) {
            Quicklooks.QLConfig qlConfig = getQuicklookConfig(context, bandName);
            if (qlConfig != null) {
                QLMapper.createQuicklook(targetProduct, productName, context, qlConfig);
            }
        }
        LOG.info("Finished creating quicklooks.");
    }

    private Quicklooks.QLConfig getQuicklookConfig(Mapper.Context context, String bandName) {
        Quicklooks.QLConfig[] configs = Quicklooks.get(context.getConfiguration());
        for (Quicklooks.QLConfig config : configs) {
            if (bandName.equalsIgnoreCase(config.getBandName())) {
                return config;
            }
        }
        return null;
    }

    private Product doReprojection(Configuration jobConfig, Product product) {
        String crsWkt = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_CRS);
        boolean hasCrsWkt = StringUtils.isNotNullAndNotEmpty(crsWkt);

        if (hasCrsWkt) {
            Map<String, Object> reprojParams = new HashMap<String, Object>();
            reprojParams.put("crs", crsWkt);
            if (jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_REPLACE_NAN_VALUE) != null) {
                double replacement = jobConfig.getFloat(JobConfigNames.CALVALUS_OUTPUT_REPLACE_NAN_VALUE, 0);
                reprojParams.put("noDataValue", replacement);
            }
            product = GPF.createProduct("Reproject", reprojParams, product);
        }
        return product;
    }

    private Map<String, Object> createGeoSubsetParameter(Configuration jobConfig) {
        String crsWkt = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_CRS);
        boolean hasCrsWkt = StringUtils.isNotNullAndNotEmpty(crsWkt);
        String regionGeometry = jobConfig.get(JobConfigNames.CALVALUS_REGION_GEOMETRY);
        boolean hasGeometry = StringUtils.isNotNullAndNotEmpty(regionGeometry);

        Map<String, Object> subsetParams = new HashMap<String, Object>();
        // do only the subset if the product is reprojected, otherwise input
        // and output region would be the same
        if (hasGeometry && hasCrsWkt) {
            subsetParams.put("geoRegion", regionGeometry);
        }
        return subsetParams;
    }

    private Map<String, Object> createBandSubsetParameter(Configuration jobConfig) {
        String outputBandList = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_BANDLIST);
        boolean hasBandList = StringUtils.isNotNullAndNotEmpty(outputBandList);

        Map<String, Object> subsetParams = new HashMap<String, Object>();
        if (hasBandList) {
            subsetParams.put("bandNames", outputBandList);
        }
        return subsetParams;
    }

    private boolean isProductEmpty(Mapper.Context context, Product product) {
        if (product == null || product.getSceneRasterWidth() == 0 || product.getSceneRasterHeight() == 0) {
            LOG.warning("target product is empty, skip writing.");
            context.getCounter(COUNTER_GROUP_NAME_PRODUCTS, "Product is empty").increment(1);
            return true;
        } else {
            return false;
        }
    }

    private static String getProductName(Configuration jobConfig, String fileName) {
        String regex = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_REGEX, null);
        String replacement = jobConfig.get(JobConfigNames.CALVALUS_OUTPUT_REPLACEMENT, null);
        String newProductName = FileUtils.getFilenameWithoutExtension(fileName);
        if (regex != null && replacement != null) {
            newProductName = getNewProductName(newProductName, regex, replacement);
        }
        return newProductName;
    }

    static String getNewProductName(String productName, String regex, String replacement) {
        return productName.replaceAll(regex, replacement);
    }
}
