package com.project.tdm.application.service;

import com.project.tdm.application.dto.UserRolesDTO;
import com.project.tdm.application.entity.RoleEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RoleService {
    RoleEntity getRoleById(Long id);

    RoleEntity getDefaultRole(String name, String description);

    RoleEntity updateDefaultRole(RoleEntity roleEntity);

    Map<String, Object> generateRolesPages(int page, int size);

    void createNewRole(String name, String description, List<String> accessPagesUrl);

    void updateRole(Long id, String description, List<String> accessPagesUrl);

    void deleteRole(Long id);

    int getRoleUserCount(Long id);

    Set<String> getAllRoleNamesList();

    Set<RoleEntity> getRolesByRoleNames(Set<String> roleNames);
}
