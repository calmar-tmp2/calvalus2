package com.bc.calvalus.production.hadoop;

import com.bc.calvalus.production.Production;
import com.bc.calvalus.production.ProductionState;
import com.bc.calvalus.production.ProductionStatus;
import org.apache.hadoop.mapred.JobStatus;
import org.apache.hadoop.mapreduce.JobID;

/**
 * A Hadoop workflow.
 *
 * @author Norman
 */
class HadoopProduction extends Production {

    public static enum Action {
        NONE,
        CANCEL,
        DELETE,
        RESTART,
    }

    private final JobID jobId;
    private String outputFormat;
    private boolean outputStaging;

    private JobStatus jobStatus;
    private ProductionStatus stagingStatus;

    private Action action;

    public HadoopProduction(String id,
                            String name,
                            JobID jobId,
                            String outputPath,
                            String outputFormat,
                            boolean outputStaging) {
        super(id, name, outputPath);
        this.jobId = jobId;
        this.outputFormat = outputFormat;
        this.outputStaging = outputStaging;
        this.action = Action.NONE;
        this.stagingStatus = new ProductionStatus();
    }

    public JobID getJobId() {
        return jobId;
    }

    public boolean getOutputStaging() {
        return outputStaging;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
        setStatus(getProductionStatus(jobStatus));
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public ProductionStatus getStagingStatus() {
        return stagingStatus;
    }

    public void setStagingStatus(ProductionStatus stagingStatus) {
        this.stagingStatus = stagingStatus;
    }

    public static ProductionStatus getProductionStatus(JobStatus job) {
        if (job != null) {
            // todo - use message that shows current 'action' value from 'HadoopProduction'
            float progress = 0.5f * (job.mapProgress() + job.reduceProgress());
            if (job.getRunState() == JobStatus.FAILED) {
                return new ProductionStatus(ProductionState.ERROR, progress);
            } else if (job.getRunState() == JobStatus.KILLED) {
                return new ProductionStatus(ProductionState.CANCELLED, progress);
            } else if (job.getRunState() == JobStatus.PREP) {
                return new ProductionStatus(ProductionState.WAITING, progress);
            } else if (job.getRunState() == JobStatus.RUNNING) {
                return new ProductionStatus(ProductionState.IN_PROGRESS, progress);
            } else if (job.getRunState() == JobStatus.SUCCEEDED) {
                return new ProductionStatus(ProductionState.COMPLETED, 1.0f);
            }
        }
        return new ProductionStatus(ProductionState.UNKNOWN);
    }
}
