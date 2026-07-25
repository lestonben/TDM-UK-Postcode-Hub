package com.project.tdm.application.service;

import com.project.tdm.application.entity.PageEntity;

import java.util.List;
import java.util.Set;

public interface PageService {
    Set<PageEntity> getDefaultRolePages();
    Set<PageEntity> getPagesByUrlPatterns(List<String> urlPatterns);
}
