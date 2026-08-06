package com.project.tdm.application.repository;

import com.project.tdm.application.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepo extends JpaRepository<JobEntity, String> {

}
