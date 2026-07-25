package com.project.tdm.application.controller;

import com.project.tdm.application.service.RoleService;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;


    @RequestMapping(value = "/api/roles/getAllRolesList", method = RequestMethod.GET)
    public ResponseEntity<?> getAllRolesList() {
        logger.info("getAllRolesList(): received request to fetch all roles");
        try {
            Set<String> userRoles = roleService.getAllRoleNamesList();

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", userRoles);

            return ResponseEntity.ok(responseMap);
        }
        catch (Exception ex) {
            logger.error("getAllRolesList(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/getRolesPagesList", method = RequestMethod.GET)
    public ResponseEntity<?> getRolesPagesList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        logger.info("getRolesPagesList(): retrieving mapping list of roles and pages. Page: {}, Size: {}", page, size);

        try {
            Map<String, Object> rolesPagesMap = roleService.generateRolesPages(page, size);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("currentPage", page);
            responseMap.put("result", rolesPagesMap.get("result"));
            responseMap.put("totalPages", rolesPagesMap.get("totalPages"));
            responseMap.put("totalItems", rolesPagesMap.get("totalItems"));

            return ResponseEntity.ok(responseMap);
        }
        catch (Exception ex) {
            logger.error("getUsersRolesList(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/getUsersRolesList", method = RequestMethod.GET)
    public ResponseEntity<?> getUsersRolesList(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        logger.info("getUsersRolesList(): retrieving mapping list of users and roles. Page: {}, Size: {}", page, size);

        try {
            Map<String, Object> usersRolesMap = userService.generateUsersRoles(keyword, page, size);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("currentPage", page);
            responseMap.put("result", usersRolesMap.get("result"));
            responseMap.put("totalPages", usersRolesMap.get("totalPages"));
            responseMap.put("totalItems", usersRolesMap.get("totalItems"));

            return ResponseEntity.ok(responseMap);
        }
        catch (Exception ex) {
            logger.error("getUsersRolesList(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/createRolePages", method = RequestMethod.POST)
    public ResponseEntity<?> createRolePages(@RequestBody Map<String, Object> paramsMap) {
        String roleName = (String) paramsMap.get("roleName");
        String roleDescription = (String) paramsMap.get("roleDescription");
        List<String> accessPagesUrl = (List<String>) paramsMap.get("accessPagesUrl");
        logger.info("createRolePages(): received request to create a new role, name: {}, description: {}, accessPagesUrl size: {}", roleName, roleDescription, accessPagesUrl.size());

        try {
            roleService.createNewRole(roleName, roleDescription, accessPagesUrl);
            logger.info("createRolePages(): successfully created a new role.");

            return ResponseEntity.ok(BaseConstants.CREATE_ROLE_SUCCESS_MSG);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("createRolePages(): errorMessage = {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("createRolePages(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/updateRolePages", method = RequestMethod.POST)
    public ResponseEntity<?> updateRolePages(@RequestBody Map<String, Object> paramsMap) {
        Long roleId = Long.parseLong(paramsMap.get("roleId").toString());
        String roleDescription = (String) paramsMap.get("roleDescription");
        List<String> accessPagesUrl = (List<String>) paramsMap.get("accessPagesUrl");
        logger.info("updateRolePages(): received request to update a new role, roleId: {}, description: {}, accessPagesUrl size: {}", roleId, roleDescription, accessPagesUrl.size());

        try {
            roleService.updateRole(roleId, roleDescription, accessPagesUrl);
            logger.info("updateRolePages(): successfully updated a new role.");

            return ResponseEntity.ok(BaseConstants.UPDATE_ROLE_SUCCESS_MSG);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("updateRolePages(): errorMessage = {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("updateRolePages(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/deleteRole", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteRole(@RequestParam("roleId") Long roleId) {
        logger.info("deleteRole(): received request to delete a role, roleId: {}", roleId);
        try {
            roleService.deleteRole(roleId);
            logger.info("deleteRole(): successfully deleted an existing role.");

            return ResponseEntity.ok(BaseConstants.DELETE_ROLE_SUCCESS_MSG);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("deleteRole(): errorMessage = {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("deleteRole(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/getRoleUserCount", method = RequestMethod.GET)
    public ResponseEntity<?> getRoleUserCount(@RequestParam("roleId") Long roleId) {
        logger.info("getRoleUserCount(): received request to count number of users with this role, roleId: {}", roleId);
        try {
            int count = roleService.getRoleUserCount(roleId);
            logger.info("getRoleUserCount(): roleCount: {}", count);

            return ResponseEntity.ok(count);
        }
        catch (Exception ex) {
            logger.error("getRoleUserCount(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/roles/updateUserRoles", method = RequestMethod.POST)
    public ResponseEntity<?> updateUserRoles(@RequestBody Map<String, Object> paramsMap) {
        Long userId = Long.parseLong(paramsMap.get("userId").toString());
        List<String> roleNames = (List<String>)paramsMap.get("roles");
        logger.info("updateUserRoles(): received request to update user roles, userId: {}, roleNames: {}", userId, Arrays.asList(roleNames));

        try {
            userService.updateUserRoles(userId, roleNames);
            logger.info("updateUserRoles(): successfully updated the user roles.");

            return ResponseEntity.ok(BaseConstants.UPDATE_USER_ROLES_SUCCESS_MSG);
        }
        catch (Exception ex) {
            logger.error("updateUserRoles(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }
}
