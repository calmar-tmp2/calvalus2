package com.bc.calvalus.processing.fire;

import com.bc.calvalus.JobClientsMap;
import com.bc.calvalus.commons.CalvalusLogger;
import com.bc.calvalus.commons.InputPathResolver;
import com.bc.calvalus.inventory.hadoop.HdfsInventoryService;
import com.bc.calvalus.processing.JobConfigNames;
import com.bc.calvalus.processing.hadoop.NoRecordReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author thomas
 * @author marcop
 */
public class FireGridInputFormat extends InputFormat {

    @Override
    public List<InputSplit> getSplits(JobContext context) throws IOException {
        Configuration conf = context.getConfiguration();
        String inputPathPatterns = conf.get(JobConfigNames.CALVALUS_INPUT_PATH_PATTERNS);

        JobClientsMap jobClientsMap = new JobClientsMap(new JobConf(conf));
        HdfsInventoryService hdfsInventoryService = new HdfsInventoryService(jobClientsMap, "eodata");

        List<InputSplit> splits = new ArrayList<>(1000);
        FileStatus[] fileStatuses = getFileStatuses(hdfsInventoryService, inputPathPatterns, conf);

        createSplits(fileStatuses, splits, conf);
        CalvalusLogger.getLogger().info(String.format("Created %d split(s).", splits.size()));
        return splits;
    }

    private void createSplits(FileStatus[] fileStatuses,
                              List<InputSplit> splits, Configuration conf) throws IOException {
        for (FileStatus fileStatus : fileStatuses) {
            List<Path> filePaths = new ArrayList<>(9);
            List<Long> fileLengths = new ArrayList<>(9);
            Path path = fileStatus.getPath();
            filePaths.add(path);
            fileLengths.add(fileStatus.getLen());
            FileStatus lcPath = getLcFileStatus(path, path.getFileSystem(conf));
            filePaths.add(lcPath.getPath());
            fileLengths.add(lcPath.getLen());

            splits.add(new CombineFileSplit(filePaths.toArray(new Path[filePaths.size()]),
                    fileLengths.stream().mapToLong(Long::longValue).toArray()));
        }
    }

    private FileStatus getLcFileStatus(Path path, FileSystem fileSystem) throws IOException {
        String baInputPath = path.toString(); // hdfs://calvalus/calvalus/projects/fire/meris-ba/$year/BA_PIX_MER_$tile_$year$month_v4.0.tif
        String lcInputPath = getLcInputPath(baInputPath);
        return fileSystem.getFileStatus(new Path(lcInputPath));
    }

    static String getLcInputPath(String baInputPath) {
        int yearIndex = baInputPath.indexOf("meris-ba/") + "meris-ba/".length();
        int year = Integer.parseInt(baInputPath.substring(yearIndex, yearIndex + 4));
        String lcYear = lcYear(year);
        int tileIndex = yearIndex + 4 + "/BA_PIX_MER_".length();
        String tile = baInputPath.substring(tileIndex, tileIndex + 6);
        return baInputPath.substring(0, baInputPath.indexOf("meris-ba") + "meris-ba".length()) + "/aux/lc/" + String.format("lc-%s-%s.nc", lcYear, tile);
    }

    private static String lcYear(int year) {
        // 2002 -> 2000
        // 2003 - 2007 -> 2005
        // 2008 - 2012 -> 2010
        switch (year) {
            case 2002:
                return "2000";
            case 2003:
            case 2004:
            case 2005:
            case 2006:
            case 2007:
                return "2005";
            case 2008:
            case 2009:
            case 2010:
            case 2011:
            case 2012:
                return "2010";
        }
        throw new IllegalArgumentException("Illegal year: " + year);
    }

    private FileStatus[] getFileStatuses(HdfsInventoryService inventoryService,
                                         String inputPathPatterns,
                                         Configuration conf) throws IOException {

        InputPathResolver inputPathResolver = new InputPathResolver();
        List<String> inputPatterns = inputPathResolver.resolve(inputPathPatterns);
        return inventoryService.globFileStatuses(inputPatterns, conf);
    }

    public RecordReader createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
        return new NoRecordReader();
    }
}
