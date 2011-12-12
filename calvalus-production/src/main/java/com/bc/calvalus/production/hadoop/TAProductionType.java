package com.bc.calvalus.production.hadoop;

import com.bc.calvalus.commons.Workflow;
import com.bc.calvalus.inventory.InventoryService;
import com.bc.calvalus.processing.JobConfigNames;
import com.bc.calvalus.processing.hadoop.HadoopProcessingService;
import com.bc.calvalus.processing.l3.L3WorkflowItem;
import com.bc.calvalus.processing.ta.TAConfig;
import com.bc.calvalus.processing.ta.TAWorkflowItem;
import com.bc.calvalus.production.DateRange;
import com.bc.calvalus.production.Production;
import com.bc.calvalus.production.ProductionException;
import com.bc.calvalus.production.ProductionRequest;
import com.bc.calvalus.staging.Staging;
import com.bc.calvalus.staging.StagingService;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.hadoop.conf.Configuration;
import org.esa.beam.util.StringUtils;

import java.util.List;

/**
 * Trend analysis: A production type used for generating a time-series generated from L3 products and a numb er of
 * given regions.
 *
 * @author MarcoZ
 * @author Norman
 */
public class TAProductionType extends HadoopProductionType {
    public static final String NAME = "TA";

    public TAProductionType(InventoryService inventoryService, HadoopProcessingService processingService, StagingService stagingService) throws ProductionException {
        super(NAME, inventoryService, processingService, stagingService);
    }

    @Override
    public Production createProduction(ProductionRequest productionRequest) throws ProductionException {

        final String productionId = Production.createId(productionRequest.getProductionType());
        String defaultProductionName = L2ProductionType.createProductionName("Trend analysis ", productionRequest);
        final String productionName = productionRequest.getProdcutionName(defaultProductionName);

        String inputPath = productionRequest.getString("inputPath");
        List<DateRange> dateRanges = L3ProductionType.getDateRanges(productionRequest, 32);

        String processorName = productionRequest.getString("processorName", null);
        String processorParameters = null;
        String processorBundle = null;
        if (processorName != null) {
            processorParameters = productionRequest.getString("processorParameters", "<parameters/>");
            processorBundle = String.format("%s-%s",
                                            productionRequest.getString("processorBundleName"),
                                            productionRequest.getString("processorBundleVersion"));
        }

        String regionName = productionRequest.getRegionName();
        Geometry regionGeometry = productionRequest.getRegionGeometry();

        String l3ConfigXml = L3ProductionType.getL3ConfigXml(productionRequest);
        TAConfig taConfig = getTAConfig(productionRequest);

        Workflow.Parallel parallel = new Workflow.Parallel();
        for (int i = 0; i < dateRanges.size(); i++) {
            DateRange dateRange = dateRanges.get(i);
            String date1Str = ProductionRequest.getDateFormat().format(dateRange.getStartDate());
            String date2Str = ProductionRequest.getDateFormat().format(dateRange.getStopDate());

            Workflow.Sequential sequential = new Workflow.Sequential();

            String l3JobName = String.format("%s L3-%s", productionName, date1Str);
            String taJobName = String.format("%s TA-%s", productionName, date1Str);

            String[] l1InputFiles = getInputPaths(getInventoryService(), inputPath, dateRange.getStartDate(), dateRange.getStopDate(), regionName);
            if (l1InputFiles.length > 0) {
                String l3OutputDir = getOutputPath(productionRequest, productionId, "-L3-" + (i + 1));
                String taOutputDir = getOutputPath(productionRequest, productionId, "-TA-" + (i + 1));

                Configuration l3JobConfig = createJobConfig(productionRequest);
                l3JobConfig.set(JobConfigNames.CALVALUS_INPUT, StringUtils.join(l1InputFiles, ","));
                l3JobConfig.set(JobConfigNames.CALVALUS_OUTPUT_DIR, l3OutputDir);
                if (processorName != null) {
                    l3JobConfig.set(JobConfigNames.CALVALUS_L2_BUNDLE, processorBundle);
                    l3JobConfig.set(JobConfigNames.CALVALUS_L2_OPERATOR, processorName);
                    l3JobConfig.set(JobConfigNames.CALVALUS_L2_PARAMETERS, processorParameters);
                }
                l3JobConfig.set(JobConfigNames.CALVALUS_L3_PARAMETERS, l3ConfigXml);
                l3JobConfig.set(JobConfigNames.CALVALUS_REGION_GEOMETRY, regionGeometry != null ? regionGeometry.toString() : "");
                l3JobConfig.set(JobConfigNames.CALVALUS_MIN_DATE, date1Str);
                l3JobConfig.set(JobConfigNames.CALVALUS_MAX_DATE, date2Str);
                L3WorkflowItem l3WorkflowItem = new L3WorkflowItem(getProcessingService(), l3JobName, l3JobConfig);

                Configuration taJobConfig = createJobConfig(productionRequest);
                taJobConfig.set(JobConfigNames.CALVALUS_INPUT, l3OutputDir);
                taJobConfig.set(JobConfigNames.CALVALUS_OUTPUT_DIR, taOutputDir);
                taJobConfig.set(JobConfigNames.CALVALUS_L3_PARAMETERS, l3ConfigXml);
                taJobConfig.set(JobConfigNames.CALVALUS_TA_PARAMETERS, taConfig.toXml());
                taJobConfig.set(JobConfigNames.CALVALUS_MIN_DATE, date1Str);
                taJobConfig.set(JobConfigNames.CALVALUS_MAX_DATE, date2Str);
                TAWorkflowItem taWorkflowItem = new TAWorkflowItem(getProcessingService(), taJobName, taJobConfig);

                sequential.add(l3WorkflowItem);
                sequential.add(taWorkflowItem);

                parallel.add(sequential);
            }
        }
        if (parallel.getItems().length == 0) {
            throw new ProductionException("No input products found for given time range.");
        }

        String stagingDir = productionRequest.getStagingDirectory(productionId);
        boolean autoStaging = productionRequest.isAutoStaging();
        return new Production(productionId,
                              productionName,
                              null, // no dedicated output directory
                              stagingDir,
                              autoStaging,
                              productionRequest,
                              parallel);
    }

    @Override
    protected Staging createUnsubmittedStaging(Production production) {
        return new TAStaging(production,
                             getProcessingService().getJobClient().getConf(),
                             getStagingService().getStagingDir());
    }

    static TAConfig getTAConfig(ProductionRequest productionRequest) throws ProductionException {
        String regionName = productionRequest.getString("regionName");
        Geometry regionGeometry = productionRequest.getRegionGeometry();
        TAConfig.RegionConfiguration regionConfiguration = new TAConfig.RegionConfiguration(regionName, regionGeometry);
        return new TAConfig(regionConfiguration);
    }
}
