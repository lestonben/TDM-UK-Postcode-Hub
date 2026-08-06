package com.project.tdm.application.service;

import com.project.tdm.application.entity.PostcodeEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BatchProcessorService {

    CompletableFuture<Void> processBatchAsync(List<PostcodeEntity> batch, String jobId);
}
