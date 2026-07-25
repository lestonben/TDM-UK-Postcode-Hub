package com.project.tdm.application.service.impl;

import com.project.tdm.application.dto.RolePagesDTO;
import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.RoleRepo;
import com.project.tdm.application.service.PageService;
import com.project.tdm.application.service.RoleService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    Map<Long, List<UserEntity>> deleteRoleUsersMap = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Autowired
    private PageService pageService;

    @Autowired
    private RoleRepo roleRepo;

    private Optional<String> checkDuplicateRoleName(String name) {
        Optional<String> usernameFound = roleRepo.existsByNameIgnoreCase(name) ?
                Optional.of(BaseConstants.DUPLICATE_ROLE_NAME_MSG) :
                Optional.empty();
        logger.info("checkDuplicateRoleName(): roleName = {}, isFound = {}", name, usernameFound.isPresent());

        return usernameFound;
    }

    @Override
    public RoleEntity getRoleById(Long id) {
        RoleEntity roleEntity = roleRepo.findById(id).orElse(null);
        logger.info("getRoleById(): roleId = {}, resultFound = {}", id, (roleEntity != null));

        return roleEntity;
    }

    @Override
    @Transactional
    public RoleEntity getDefaultRole(String name, String description) {
        logger.info("getDefaultRole(): Attempting to find default role by name: {}", name);
        return roleRepo.findByName(name)
            .orElseGet(() -> {
                logger.info("getDefaultRole(): Default role not found. Creating new default role: {}", name);
                RoleEntity newDefaultRole = new RoleEntity(name, description);
                RoleEntity savedRole = roleRepo.save(newDefaultRole);
                logger.info("getDefaultRole(): Successfully created new default role with id: {}", savedRole.getId());

                return savedRole;
        });
    }

    @Override
    @Transactional
    public RoleEntity updateDefaultRole(RoleEntity roleEntity) {
        logger.info("updateDefaultRole(): Updating default role id: {}, name: {}", roleEntity.getId(), roleEntity.getName());
        RoleEntity updateRole = roleRepo.save(roleEntity);
        logger.info("updateDefaultRole(): Successfully updated default role id: {}", updateRole.getId());

        return updateRole;
    }

    @Override
    public Map<String, Object> generateRolesPages(int page, int size) {
        // 1. Fetch the list of roles based on the specific page and size
        logger.info("generateRolesPages(): Requesting role pages pagination - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<RoleEntity> roles = roleRepo.findAll(pageable);

        // 2. Do variables mapping for the list of RolePagesDTO objects
        List<RolePagesDTO> rolesPages = roles.stream().map(role -> {
            Set<String> accessPages = role.getAllowedPages().stream().map(PageEntity::getName).collect(Collectors.toSet());
            return new RolePagesDTO(role.getId(), role.getName(), role.getDescription(), accessPages);
        }).toList();
        logger.info("generateRolesPages(): returning page {} of {}, result size = {}", roles.getNumber(), roles.getTotalPages(), rolesPages.size());

        Map<String, Object> rolesPagesMap = new HashMap<>();
        rolesPagesMap.put("result", rolesPages);
        rolesPagesMap.put("totalPages", roles.getTotalPages());
        rolesPagesMap.put("totalItems", roles.getTotalElements());

        return rolesPagesMap;
    }

    @Override
    @Transactional
    public void createNewRole(String name, String description, List<String> accessPagesUrl) {
        logger.info("createNewRole(): name: {}, description: {}, accessPageUrl: {}", name, description, accessPagesUrl);
        checkDuplicateRoleName(name).ifPresent(errMsg -> { throw new IllegalArgumentException(errMsg); });

        // 1. Fetch the match pages access based on urlPatterns
        Set<PageEntity> matchPages = pageService.getPagesByUrlPatterns(accessPagesUrl);
        logger.info("createNewRole(): Resolved {} matching pages out of {} requested URLs", matchPages.size(), (accessPagesUrl != null ? accessPagesUrl.size() : 0));

        // 2. RoleEntity mapping before the role creation
        RoleEntity newRole = new RoleEntity(name, description);
        newRole.setAllowedPages(matchPages);

        RoleEntity savedRole = roleRepo.save(newRole);
        logger.info("createNewRole(): Successfully created new role with ID: {} and Name: {}", savedRole.getId(), savedRole.getName());
    }

    @Override
    @Transactional
    public void updateRole(Long id, String description, List<String> accessPagesUrl) {
        // 1. Fetch the existing role record
        logger.info("updateRole(): Fetching role record for roleId: {}", id);
        RoleEntity matchRole = getRoleById(id);
        if (matchRole == null) {
            logger.warn("updateRole(): Role record not available for id: {}", id);
            throw new IllegalArgumentException(BaseConstants.ROLE_RECORD_NOT_AVAILABLE);
        }
        List<String> matchRoleUrls = matchRole.getAllowedPages().stream().map(PageEntity::getUrlPattern).toList();

        // 2. Find URLs to add & URLs to remove
        List<String> urlsNotFoundFromMatchRole = accessPagesUrl.stream().filter(url -> !matchRoleUrls.contains(url)).toList();
        List<String> urlsToRemoveFromMatchRole = matchRoleUrls.stream().filter(url -> !accessPagesUrl.contains(url)).toList();

        logger.info("updateRole(): URLs to add count: {}, URLs to remove count: {}", urlsNotFoundFromMatchRole.size(), urlsToRemoveFromMatchRole.size());

        // 3. Iterate the current role URLs to keep the pages that are not in the list of URLs to remove
        Set<PageEntity> pageList = matchRole.getAllowedPages().stream()
                .filter(page -> !urlsToRemoveFromMatchRole.contains(page.getUrlPattern()))
                .collect(Collectors.toSet());

        // 4. Fetch the URLs to add, and combine them
        pageList.addAll(pageService.getPagesByUrlPatterns(urlsNotFoundFromMatchRole));

        // 5. Map the new values to the match role
        matchRole.setDescription(description);
        matchRole.getAllowedPages().clear();
        matchRole.setAllowedPages(pageList);

        roleRepo.save(matchRole);
        logger.info("updateRole(): Successfully updated role id: {}", id);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        logger.info("deleteRole(): Attempting to delete role with ID: {}", id);
        long roleCount = roleRepo.count();
        if (roleCount <= 1) {
            logger.warn("deleteRole(): Deletion rejected. Only one role record remains in the system.");
            throw new IllegalArgumentException(BaseConstants.ONLY_ROLE_RECORD_CANNOT_DELETE);
        }

        RoleEntity matchRole = getRoleById(id);
        if (matchRole == null) {
            logger.warn("deleteRole(): Role record not available for ID: {}", id);
            throw new IllegalArgumentException(BaseConstants.ROLE_RECORD_NOT_AVAILABLE);
        }

        // 1. Disassociate this role from all users to prevent foreign key constraint violations
        logger.info("deleteRole(): Disassociating role ID {} from {} assigned users", id, matchRole.getUsers().size());
        for (UserEntity user : matchRole.getUsers()) {
            user.getUserRoles().remove(matchRole);
        }
        matchRole.getUsers().clear();

        // 2. Clear allowed pages mapping (optional, depending on cascade rules, but clean)
        matchRole.getAllowedPages().clear();

        // 3. Delete the selected role & update the concurrentHashMap if there is existing record
        roleRepo.delete(matchRole);
        deleteRoleUsersMap.remove(id);
        logger.info("deleteRole(): Successfully deleted role ID: {}", id);
    }

    @Override
    public int getRoleUserCount(Long id) {
        if (deleteRoleUsersMap.containsKey(id)) {
            int cachedCount = deleteRoleUsersMap.get(id).size();
            logger.debug("getRoleUserCount(): Retrieved user count from cache for roleId {}: {}", id, cachedCount);
            return cachedCount;
        }

        RoleEntity matchRole = getRoleById(id);
        if (matchRole == null) {
            logger.warn("getRoleUserCount(): Role record not available for ID: {}", id);
            throw new IllegalArgumentException(BaseConstants.ROLE_RECORD_NOT_AVAILABLE);
        }

        deleteRoleUsersMap.computeIfAbsent(id, list -> new ArrayList<>()).addAll(matchRole.getUsers());
        logger.info("getRoleUserCount(): Fetched the user count from database for roleId {}: {}", id, matchRole.getUsers().size());

        return matchRole.getUsers().size();
    }

    @Override
    public Set<String> getAllRoleNamesList() {
        List<RoleEntity> rolesEntity = roleRepo.findAll();
        Set<String> roleNames = rolesEntity.stream().map(RoleEntity::getName).collect(Collectors.toSet());
        logger.info("getAllRoleNamesList(): Retrieved {} role names", roleNames.size());

        return roleNames;
    }

    @Override
    public Set<RoleEntity> getRolesByRoleNames(Set<String> roleNames) {
        logger.info("getRolesByRoleNames(): Fetching roles for names: {}", roleNames);
        Set<RoleEntity> roles = new HashSet<>(roleRepo.findByNameIn(roleNames.stream().toList()));
        logger.info("getRolesByRoleNames(): Found {} matching role entities", roles.size());

        return roles;
    }
}
