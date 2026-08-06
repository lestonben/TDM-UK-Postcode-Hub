package com.project.tdm.application.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    @Column(name = "job_id", length = 36)
    private String jobId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "processed_rows")
    private int processedRows;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_by")
    private String submittedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getFileName() { return fileName; }
    public void setFileName(String filename) { this.fileName = filename; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getProcessedRows() { return processedRows; }
    public void setProcessedRows(int processedRows) { this.processedRows = processedRows; }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }
}