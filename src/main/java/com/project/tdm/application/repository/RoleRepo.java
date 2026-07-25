package com.project.tdm.application.repository;

import com.project.tdm.application.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findById(Long roleId);

    Optional<RoleEntity> findByName(String roleName);

    List<RoleEntity> findByNameIn(List<String> roleNames);

    boolean existsByNameIgnoreCase(String roleName);
}
