package com.project.tdm.application.utilities.util;

import com.project.tdm.application.dto.JobProgressDTO;
import com.project.tdm.application.entity.JobEntity;
import com.project.tdm.application.repository.JobRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobProgressTrackerUtil {

    private static final Logger logger = LoggerFactory.getLogger(JobProgressTrackerUtil.class);

    private final ConcurrentHashMap<String, JobProgressDTO> progressMap = new ConcurrentHashMap<>();
    private final JobRepo jobRepo;

    public JobProgressTrackerUtil(JobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    public Map<String, Object> getRecentJobs() {
        List<JobEntity> recentJobs = jobRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        logger.info("getRecentJobs(): returning page, result size = {}", recentJobs.size());

        Map<String, Object> tempMap = new HashMap<>();
        tempMap.put("result", recentJobs);

        return tempMap;
    }

    public JobProgressDTO getStatus(String jobId, String username) {
        return progressMap.getOrDefault(jobId, new JobProgressDTO(jobId, "PROCESSING", username));
    }

    public void initializeJob(String username, String jobId, String filename) {
        progressMap.put(jobId, new JobProgressDTO(filename, "PROCESSING", username));

        JobEntity entity = new JobEntity();
        entity.setJobId(jobId);
        entity.setFileName(filename);
        entity.setStatus("PROCESSING");
        entity.setTotalRows(0);
        entity.setProcessedRows(0);
        entity.setSubmittedBy(username);

        jobRepo.save(entity);
    }

    public void incrementTotalRows(String jobId) {
        JobProgressDTO progress = progressMap.get(jobId);
        if (progress != null) {
            progress.totalRows.incrementAndGet();
        }
    }

    public void addProcessedRows(String jobId, int count) {
        JobProgressDTO progress = progressMap.get(jobId);
        if (progress != null) {
            progress.processedRows.addAndGet(count);
        }
    }

    public void markJobComplete(String jobId) {
        JobProgressDTO progress = progressMap.get(jobId);
        if (progress != null) {
            progress.status = "COMPLETED";
            updateDatabaseState(jobId, "COMPLETED", progress);
        }
    }

    public void markJobFailed(String jobId, String error) {
        JobProgressDTO progress = progressMap.get(jobId);

        String safeError = error != null ? error : "Unknown error";
        if (safeError.length() > 235) {
            safeError = safeError.substring(0, 235) + "...";
        }
        String finalStatus = "FAILED: " + safeError;

        if (progress != null) {
            progress.status = finalStatus;
            updateDatabaseState(jobId, finalStatus, progress);
        }
    }

    private void updateDatabaseState(String jobId, String status, JobProgressDTO progress) {
        jobRepo.findById(jobId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setTotalRows(progress.getTotalRows());
            entity.setProcessedRows(progress.getProcessedRows());
            jobRepo.save(entity);
        });
    }

    public boolean isJobFailed(String jobId) {
        JobProgressDTO progress = progressMap.get(jobId);
        return progress != null && progress.status.startsWith("FAILED");
    }

    public JobProgressDTO getJobProgress(String jobId) {
        return progressMap.get(jobId);
    }
}
