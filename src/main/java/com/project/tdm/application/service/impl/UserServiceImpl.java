package com.project.tdm.application.service.impl;

import com.project.tdm.application.dto.UserRolesDTO;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.service.PageService;
import com.project.tdm.application.service.RoleService;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.security.util.HashPassUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private PageService pageService;

    @Autowired
    private HashPassUtil hashPassUtil;

    private UserRepo userRepo;

    @Autowired
    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    private Optional<String> checkDuplicateUsername(UserEntity user) {
        Optional<String> usernameFound = userRepo.existsByUsernameIgnoreCase(user.getUsername()) ?
                Optional.of(BaseConstants.USERNAME_USED_MSG) :
                Optional.empty();
        logger.info("checkDuplicateUsername(): username = {}, isFound = {}", user.getUsername(), usernameFound.isPresent());

        return usernameFound;
    }

    private Optional<String> checkDuplicateEmail(UserEntity user) {
        Optional<String> emailFound = userRepo.existsByEmailIgnoreCase(user.getEmail()) ?
                Optional.of(BaseConstants.EMAIL_USED_MSG) :
                Optional.empty();
        logger.info("checkDuplicateEmail(): email = {}, isFound = {}", user.getEmail(), emailFound.isPresent());

        return emailFound;
    }

    @Override
    @Transactional
    public void registerUser(UserEntity user) {
        logger.info("registerUser(): Attempting to register user with username: {} and email: {}", user.getUsername(), user.getEmail());

        checkDuplicateEmail(user)
                .or(() -> checkDuplicateUsername(user))
                .ifPresent(errMsg -> {
                    logger.warn("registerUser(): Registration rejected due to duplicate credentials for user '{}'. Reason: {}", user.getUsername(), errMsg);
                    throw new IllegalArgumentException(errMsg);
                });

        user.setPassword(hashPassUtil.hashPassword(user.getPassword()));

        RoleEntity viewerRole = roleService.getDefaultRole(BaseConstants.VIEWER_NAME, BaseConstants.VIEWER_DESC);
        user.addRole(viewerRole);
        logger.info("registerUser(): Assigned default role '{}' to new user: {}", BaseConstants.VIEWER_NAME, user.getUsername());

        userRepo.save(user);
        logger.info("registerUser(): Successfully registered and saved user: {}", user.getUsername());
    }

    @Override
    public UserEntity loginUser(UserEntity user) {
        logger.info("loginUser(): Login attempt initiated for identifier: {}", (user.getUsername() != null ? user.getUsername() : user.getEmail()));

        UserEntity matchUser = Optional.ofNullable(user.getUsername())
                        .flatMap(username -> userRepo.findByUsernameIgnoreCase(username))
                .or(() -> Optional.ofNullable(user.getEmail())
                        .flatMap(email -> userRepo.findByEmailIgnoreCase(email)))
                .orElseThrow(() -> {
                    logger.warn("loginUser(): Authentication failed - User record not found for provided credentials.");
                    return new IllegalArgumentException(BaseConstants.INVALID_CREDENTIAL_MSG);
                });

        if (!hashPassUtil.verifyPassword(user.getPassword(), matchUser.getPassword())) {
            logger.warn("loginUser(): Authentication failed - User record not found for provided credentials.");
            throw new IllegalArgumentException(BaseConstants.INVALID_CREDENTIAL_MSG);
        }

        return matchUser;
    }

    @Override
    public UserEntity getUserByUsername(String username) {
        UserEntity userEntity = userRepo.findByUsernameIgnoreCase(username).orElse(null);
        logger.info("getUserByUsername(): username = {}, resultFound = {}", username, (userEntity != null));

        return userEntity;
    }

    @Override
    public Map<String, Object> generateUsersRoles(String keyword, int page, int size) {
        // 1. Fetch the list of users based on the specific page and size, instead of all records
        logger.info("generateUsersRoles(): Requesting users roles pagination - keyword: '{}', page: {}, size: {}", keyword, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> users = StringUtils.hasText(keyword)
                ? userRepo.findByUsernameContainingIgnoreCase(keyword.trim(), pageable)
                : userRepo.findAll(pageable);

        // 2. Do variables mapping for the list of UserRolesDTO objects
        List<UserRolesDTO> usersRoles = users.stream().map(user -> {
            Set<String> roles = user.getUserRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());
            return new UserRolesDTO(user.getUserId(), user.getUsername(), roles);
        }).toList();
        logger.info("generateUsersRoles(): returning page {} of {}, result size = {}", users.getNumber(), users.getTotalPages(), usersRoles.size());

        Map<String, Object> usersRolesMap = new HashMap<>();
        usersRolesMap.put("result", usersRoles);
        usersRolesMap.put("totalPages", users.getTotalPages());
        usersRolesMap.put("totalItems", users.getTotalElements());

        return usersRolesMap;
    }

    @Override
    @Transactional
    public void updateUserRoles(Long id, List<String> roleNames) {
        // 1. Fetch the existing current user
        logger.info("updateUserRoles(): Updating roles for userId: {} with requested roles: {}", id, roleNames);
        UserEntity currentUser = userRepo.findById(id).orElseThrow(() -> {
            logger.warn("updateUserRoles(): User record not found for ID: {}", id);
            return new IllegalArgumentException(BaseConstants.ROLE_RECORD_NOT_AVAILABLE);
        });
        Set<String> currentUserRoleNames = currentUser.getUserRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());

        // 2. Find roles to add & roles to be removed
        Set<String> rolesNotFoundFromUser = roleNames.stream().filter(role -> !currentUserRoleNames.contains(role)).collect(Collectors.toSet());
        Set<String> rolesToRemoveFromUser = currentUserRoleNames.stream().filter(role -> !roleNames.contains(role)).collect(Collectors.toSet());

        logger.info("updateUserRoles(): Roles to add count: {}, Roles to remove count: {}", rolesNotFoundFromUser.size(), rolesToRemoveFromUser.size());

        // 3. Iterate the user roles list and filter the roles needed to keep
        //      Fetch the roles to add from database, then combine them
        Set<RoleEntity> roleList = currentUser.getUserRoles().stream().filter(role -> !rolesToRemoveFromUser.contains(role.getName())).collect(Collectors.toSet());
        roleList.addAll(roleService.getRolesByRoleNames(rolesNotFoundFromUser));

        // 4. Map the new values to the current user
        currentUser.getUserRoles().clear();
        currentUser.setUserRoles(roleList);

        userRepo.save(currentUser);
        logger.info("updateUserRoles(): Successfully updated roles for user ID: {}", id);
    }
}
