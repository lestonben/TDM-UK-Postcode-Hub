package com.project.tdm.application.service.impl;

import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.service.BatchProcessorService;
import com.project.tdm.application.service.PostcodeImportService;
import com.project.tdm.application.utilities.util.ExcelStreamParserUtil;
import com.project.tdm.application.utilities.util.JobProgressTrackerUtil;
import com.project.tdm.application.utilities.util.PostcodeValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class PostcodeImportServiceImpl implements PostcodeImportService {

    private static final Logger logger = LoggerFactory.getLogger(PostcodeImportServiceImpl.class);

    private final JobProgressTrackerUtil progressTracker;
    private final ExcelStreamParserUtil excelStreamParser;
    private final BatchProcessorService batchProcessor;

    @Autowired
    public PostcodeImportServiceImpl(JobProgressTrackerUtil progressTracker, ExcelStreamParserUtil excelStreamParser, BatchProcessorService batchProcessor) {
        this.progressTracker = progressTracker;
        this.excelStreamParser = excelStreamParser;
        this.batchProcessor = batchProcessor;
    }


    @Async("taskExecutor")
    public void processFileAsync(String username, String jobId, File file, String originalFilename) {
        logger.info("Started background job {}. File: {}", jobId, originalFilename);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            progressTracker.initializeJob(username, jobId, originalFilename);

            List<PostcodeEntity> batch = new ArrayList<>();
            int batchSize = 1000;

            try (InputStream is = new FileInputStream(file)) {
                // Using your preferred, clean Consumer callback pattern
                excelStreamParser.parseExcelFile(is, record -> {
                    progressTracker.incrementTotalRows(jobId);

                    boolean validationPassed = PostcodeValidationUtil.isValidPostcode(record.getPostcode()) &&
                            PostcodeValidationUtil.isValidCoordinates(record.getLatitude(), record.getLongitude());

                    if (validationPassed) {
                        batch.add(record);
                    }
                    else {
                        logger.warn("Skipping validation failed record: Postcode='{}', Latitude='{}', Longitude='{}'", record.getPostcode(), record.getLatitude(), record.getLongitude());
                    }

                    if (batch.size() >= batchSize) {
                        logger.debug("{} postcode objects have been parsed from excel streaming, waiting for batch job to accept processing.", batch.size());
                        List<PostcodeEntity> currentBatch = new ArrayList<>(batch);
                        batch.clear();

                        futures.add(batchProcessor.processBatchAsync(currentBatch, jobId));
                    }
                });
            }

            // Flush remaining rows
            if (!batch.isEmpty()) {
                futures.add(batchProcessor.processBatchAsync(new ArrayList<>(batch), jobId));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        if (!progressTracker.isJobFailed(jobId)) {
                            progressTracker.markJobComplete(jobId);
                            logger.info("Job {} successfully completed.", jobId);
                        }
                    });
        }
        catch (Exception e) {
            logger.error("Parsing failed for Job ID: {}", jobId, e);
            progressTracker.markJobFailed(jobId, e.getMessage());
        }
        finally {
            // ALWAYS clean up the temporary file
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }
}
