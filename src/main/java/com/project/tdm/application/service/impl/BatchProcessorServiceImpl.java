package com.project.tdm.application.service.impl;

import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.repository.impl.PostcodeBatchRepoImpl;
import com.project.tdm.application.service.BatchProcessorService;
import com.project.tdm.application.utilities.util.JobProgressTrackerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BatchProcessorServiceImpl implements BatchProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(BatchProcessorServiceImpl.class);

    private final PostcodeBatchRepoImpl postcodeBatchRepo;
    private final JobProgressTrackerUtil progressTracker;

    public BatchProcessorServiceImpl(PostcodeBatchRepoImpl postcodeBatchRepo, JobProgressTrackerUtil progressTracker) {
        this.postcodeBatchRepo = postcodeBatchRepo;
        this.progressTracker = progressTracker;
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> processBatchAsync(List<PostcodeEntity> batch, String jobId) {
        if (batch == null || batch.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        String currentThread = Thread.currentThread().getName();
        logger.debug("[{}] Started processing batch of {} rows for Job ID: {}", currentThread, batch.size(), jobId);

        // Arrange sequence lexicographically to prevent locking deadlocks
        batch.sort(Comparator.comparing(PostcodeEntity::getPostcode));

        // Execute batch DB update with failure fallback
        postcodeBatchRepo.batchUpsertWithFallback(batch);

        // Update progress tracker safely
        progressTracker.addProcessedRows(jobId, batch.size());
        logger.debug("[{}] Successfully completed batch for Job ID: {}", currentThread, jobId);

        return CompletableFuture.completedFuture(null);
    }
}
