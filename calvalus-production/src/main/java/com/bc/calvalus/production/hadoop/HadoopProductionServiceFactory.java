package com.bc.calvalus.production.hadoop;

import com.bc.calvalus.inventory.hadoop.HdfsInventoryService;
import com.bc.calvalus.processing.hadoop.HadoopProcessingService;
import com.bc.calvalus.production.ProductionException;
import com.bc.calvalus.production.ProductionService;
import com.bc.calvalus.production.ProductionServiceFactory;
import com.bc.calvalus.production.ProductionServiceImpl;
import com.bc.calvalus.production.ProductionType;
import com.bc.calvalus.production.store.MemoryProductionStore;
import com.bc.calvalus.production.store.ProductionStore;
import com.bc.calvalus.production.store.SqlProductionStore;
import com.bc.calvalus.staging.SimpleStagingService;
import com.bc.calvalus.staging.StagingService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Creates a hadoop production service.
 */
public class HadoopProductionServiceFactory implements ProductionServiceFactory {
    private static final String DEFAULT_PRODUCTIONS_DB_NAME = "calvalus-database";

    @Override
    public ProductionService create(Map<String, String> serviceConfiguration,
                                    File appDataDir,
                                    File stagingDir) throws ProductionException {

        // Prevent Windows from using ';' as path separator
        System.setProperty("path.separator", ":");

        JobConf jobConf = new JobConf(createJobConfiguration(serviceConfiguration));
        try {
            JobClient jobClient = new JobClient(jobConf);
            HdfsInventoryService inventoryService = new HdfsInventoryService(jobClient.getFs());
            HadoopProcessingService processingService = new HadoopProcessingService(jobClient);
            ProductionStore productionStore = null;
            if ("memory".equals(serviceConfiguration.get("production.db.type"))) {
                productionStore = new MemoryProductionStore();
            } else {
                // todo - get the database connect info from configuration
                String databaseUrl = "jdbc:hsqldb:file:" + new File(appDataDir, DEFAULT_PRODUCTIONS_DB_NAME).getPath();
                boolean databaseExists = new File(appDataDir, DEFAULT_PRODUCTIONS_DB_NAME + ".properties").exists();
                productionStore = SqlProductionStore.create(processingService,
                                                            "org.hsqldb.jdbcDriver",
                                                            databaseUrl,
                                                            "SA", "",
                                                            !databaseExists);
            }
            StagingService stagingService = new SimpleStagingService(stagingDir, 3);
            ProductionType l2ProductionType = new L2ProductionType(inventoryService, processingService, stagingService);
            ProductionType l2PlusProductionType = new L2PlusProductionType(inventoryService, processingService, stagingService);
            ProductionType l2fProductionType = new L2FProductionType(inventoryService, processingService, stagingService);
            ProductionType l3ProductionType = new L3ProductionType(inventoryService, processingService, stagingService);
            ProductionType taProductionType = new TAProductionType(inventoryService, processingService, stagingService);
            ProductionType maProductionType = new MAProductionType(inventoryService, processingService, stagingService);

            ProductionType pvProductionType = new InventoryProductionType(inventoryService, processingService, stagingService);
            ProductionType qlProductionType = new QLProductionType(inventoryService, processingService, stagingService);
            ProductionType pgProductionType = new GeometryProductionType(inventoryService, processingService, stagingService);

            ProductionType lcl3ProductionType = new LcL3ProductionType(inventoryService, processingService, stagingService);
            ProductionType lcl3frrrProductionType = new LcL3FrRrProductionType(inventoryService, processingService, stagingService);
            ProductionType lcl3SeasonalProductionType = new LcSeasonalProductionType(inventoryService, processingService, stagingService);

            ProductionType prevueProductionType = new PrevueProductionType(inventoryService, processingService, stagingService);

            ProductionType globVegProductionType = new GLobVegProductionType(inventoryService, processingService, stagingService);

            return new ProductionServiceImpl(inventoryService,
                                             processingService,
                                             stagingService,
                                             productionStore,
                                             l2ProductionType,
                                             l2PlusProductionType,
                                             l2fProductionType,
                                             l3ProductionType,
                                             taProductionType,
                                             maProductionType,
                                             pvProductionType,
                                             lcl3ProductionType,
                                             lcl3frrrProductionType,
                                             lcl3SeasonalProductionType,
                                             qlProductionType,
                                             pgProductionType,
                                             prevueProductionType,
                                             globVegProductionType);
        } catch (IOException e) {
            throw new ProductionException("Failed to create Hadoop JobClient." + e.getMessage(), e);
        }
    }

    private static Configuration createJobConfiguration(Map<String, String> serviceConfiguration) {
        Configuration jobConfiguration = new Configuration();
        HadoopProductionType.setJobConfig(jobConfiguration, serviceConfiguration);
        return jobConfiguration;
    }

}
