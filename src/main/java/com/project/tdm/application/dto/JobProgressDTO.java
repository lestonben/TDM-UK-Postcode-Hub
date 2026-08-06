package com.project.tdm.application.dto;

import java.util.concurrent.atomic.AtomicInteger;

public class JobProgressDTO {

    private final String fileName;
    public volatile String status;
    public final AtomicInteger totalRows;
    public final AtomicInteger processedRows;
    private final String submittedBy;

    public JobProgressDTO(String fileName, String status, String username) {
        this.fileName = fileName;
        this.status = status;
        this.totalRows = new AtomicInteger(0);
        this.processedRows = new AtomicInteger(0);
        this.submittedBy = username;
    }

    // Getters used by Jackson to serialize to JSON
    public String getFileName() { return fileName; }
    public String getStatus() { return status; }
    public int getTotalRows() { return totalRows.get(); }
    public int getProcessedRows() { return processedRows.get(); }
    public String getSubmittedBy() { return submittedBy; }

    // Setter for status since it changes
    public void setStatus(String status) { this.status = status; }

    // Package-private or public getters for the Atomic variables for the Tracker to manipulate
    public AtomicInteger getTotalRowsAtomic() { return totalRows; }
    public AtomicInteger getProcessedRowsAtomic() { return processedRows; }
}
