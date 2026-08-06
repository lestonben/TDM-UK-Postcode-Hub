package com.project.tdm.application.service;


import java.io.File;

public interface PostcodeImportService {

    void processFileAsync(String username, String jobId, File file, String fileName);
}
