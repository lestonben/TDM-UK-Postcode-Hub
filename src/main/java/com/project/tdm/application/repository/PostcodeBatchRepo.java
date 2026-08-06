package com.project.tdm.application.repository;

import com.project.tdm.application.entity.PostcodeEntity;

import java.util.List;

public interface PostcodeBatchRepo {
    void batchUpsertWithFallback(List<PostcodeEntity> records);
}
