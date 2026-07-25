package com.project.tdm.application.repository;

import com.project.tdm.application.entity.PageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PageRepo extends JpaRepository<PageEntity, Long> {

    PageEntity findByUrlPattern(String urlPattern);
    Set<PageEntity> findByUrlPatternIn(Set<String> urlPatterns);
}
