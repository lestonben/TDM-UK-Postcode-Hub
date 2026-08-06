package com.project.tdm.application.controller;

import com.project.tdm.application.dto.JobProgressDTO;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.PostcodeImportService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.application.utilities.util.JobProgressTrackerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
public class PostcodeImportController {

    private static final Logger logger = LoggerFactory.getLogger(PostcodeImportController.class);

    @Autowired
    private PostcodeImportService importService;

    @Autowired
    private JobProgressTrackerUtil progressTracker;

    @RequestMapping(value = "/api/postcodes/jobs/import", method = RequestMethod.POST)
    public ResponseEntity<?> jobImport(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Object principal) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String username = ((UserEntity) principal).getUsername();

        String jobId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        logger.info("jobImport(): received request for creating/updating postcodes from user: {}, jobId: {}, fileName: {}", username, jobId, originalFilename);

        try {
            File tempFile = File.createTempFile("postcode_job_" + jobId, ".xlsx");

            // 2. Transfer the uploaded data into our safe file
            file.transferTo(tempFile);

            // 3. Pass the java.io.File (NOT the MultipartFile) to the background thread
            importService.processFileAsync(username, jobId, tempFile, originalFilename);

            return ResponseEntity.ok(jobId);
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process file upload.");
        }
    }

    @RequestMapping(value = "/api/postcodes/jobs", method = RequestMethod.GET)
    public ResponseEntity<?> getRecentJobs() {
        logger.info("getRecentJobs(): received request of fetching recent created jobs.");

        try {
            Map<String, Object> responseMap = progressTracker.getRecentJobs();

            return ResponseEntity.ok(responseMap);
        }
        catch (Exception ex) {
            logger.error("getRecentJobs(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @GetMapping("/api/postcodes/jobs/status/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId, @AuthenticationPrincipal Object principal) {
        String username = ((UserEntity) principal).getUsername();
        try {
            JobProgressDTO status = progressTracker.getStatus(jobId, username);

            return ResponseEntity.ok(status);
        }
        catch (Exception ex) {
            logger.error("getJobStatus(): unexpected error", ex);
            return ResponseEntity.internalServerError().body("Error fetching status");
        }
    }
}
