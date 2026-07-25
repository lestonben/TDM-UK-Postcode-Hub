package com.project.tdm.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.tdm.application.service.RoleService;
import com.project.tdm.application.service.UserService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(roleController).build();
    }

    // ==========================================
    // 1. getAllRolesList TESTS
    // ==========================================

    @Test
    void shouldGetAllRolesListSuccessfully() throws Exception {
        when(roleService.getAllRoleNamesList()).thenReturn(Set.of("Admin", "Viewer"));

        mockMvc.perform(get("/api/roles/getAllRolesList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetAllRolesFails() throws Exception {
        when(roleService.getAllRoleNamesList()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/roles/getAllRolesList"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 2. getRolesPagesList TESTS
    // ==========================================

    @Test
    void shouldGetRolesPagesListSuccessfully() throws Exception {
        Map<String, Object> mockMap = Map.of(
                "result", List.of(),
                "totalPages", 1,
                "totalItems", 0L
        );
        when(roleService.generateRolesPages(0, 10)).thenReturn(mockMap);

        mockMvc.perform(get("/api/roles/getRolesPagesList")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetRolesPagesListFails() throws Exception {
        when(roleService.generateRolesPages(anyInt(), anyInt())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/roles/getRolesPagesList"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 3. getUsersRolesList TESTS
    // ==========================================

    @Test
    void shouldGetUsersRolesListSuccessfully() throws Exception {
        Map<String, Object> mockMap = Map.of(
                "result", List.of(),
                "totalPages", 1,
                "totalItems", 0L
        );
        when(userService.generateUsersRoles(eq("test"), eq(0), eq(10))).thenReturn(mockMap);

        mockMvc.perform(get("/api/roles/getUsersRolesList")
                        .param("keyword", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetUsersRolesListFails() throws Exception {
        when(userService.generateUsersRoles(any(), anyInt(), anyInt())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/roles/getUsersRolesList"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 4. createRolePages TESTS
    // ==========================================

    @Test
    void shouldCreateRolePagesSuccessfully() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleName", "Manager",
                "roleDescription", "Manager Desc",
                "accessPagesUrl", List.of("/dashboard")
        );

        doNothing().when(roleService).createNewRole(anyString(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/createRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string(BaseConstants.CREATE_ROLE_SUCCESS_MSG));
    }

    @Test
    void shouldReturnBadRequestWhenCreateRoleHasIllegalArgumentException() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleName", "Manager",
                "roleDescription", "Manager Desc",
                "accessPagesUrl", List.of("/dashboard")
        );

        doThrow(new IllegalArgumentException("Duplicate role name"))
                .when(roleService).createNewRole(anyString(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/createRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Duplicate role name"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenCreateRoleFailsUnexpectedly() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleName", "Manager",
                "roleDescription", "Manager Desc",
                "accessPagesUrl", List.of("/dashboard")
        );

        doThrow(new RuntimeException("Unexpected")).when(roleService).createNewRole(anyString(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/createRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 5. updateRolePages TESTS
    // ==========================================

    @Test
    void shouldUpdateRolePagesSuccessfully() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleId", 1L,
                "roleDescription", "Updated Desc",
                "accessPagesUrl", List.of("/dashboard")
        );

        doNothing().when(roleService).updateRole(anyLong(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/updateRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string(BaseConstants.UPDATE_ROLE_SUCCESS_MSG));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateRoleThrowsIllegalArgumentException() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleId", 99L,
                "roleDescription", "Desc",
                "accessPagesUrl", List.of()
        );

        doThrow(new IllegalArgumentException("Role not available"))
                .when(roleService).updateRole(anyLong(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/updateRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Role not available"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdateRoleFailsUnexpectedly() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "roleId", 1L,
                "roleDescription", "Desc",
                "accessPagesUrl", List.of()
        );

        doThrow(new RuntimeException("Error")).when(roleService).updateRole(anyLong(), anyString(), anyList());

        mockMvc.perform(post("/api/roles/updateRolePages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 6. deleteRole TESTS
    // ==========================================

    @Test
    void shouldDeleteRoleSuccessfully() throws Exception {
        doNothing().when(roleService).deleteRole(1L);

        mockMvc.perform(delete("/api/roles/deleteRole")
                        .param("roleId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(BaseConstants.DELETE_ROLE_SUCCESS_MSG));
    }

    @Test
    void shouldReturnBadRequestWhenDeleteRoleThrowsIllegalArgumentException() throws Exception {
        doThrow(new IllegalArgumentException("Cannot delete record"))
                .when(roleService).deleteRole(1L);

        mockMvc.perform(delete("/api/roles/deleteRole")
                        .param("roleId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot delete record"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenDeleteRoleFailsUnexpectedly() throws Exception {
        doThrow(new RuntimeException("Error")).when(roleService).deleteRole(1L);

        mockMvc.perform(delete("/api/roles/deleteRole")
                        .param("roleId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 7. getRoleUserCount TESTS
    // ==========================================

    @Test
    void shouldGetRoleUserCountSuccessfully() throws Exception {
        when(roleService.getRoleUserCount(1L)).thenReturn(5);

        mockMvc.perform(get("/api/roles/getRoleUserCount")
                        .param("roleId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetRoleUserCountFails() throws Exception {
        when(roleService.getRoleUserCount(anyLong())).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/api/roles/getRoleUserCount")
                        .param("roleId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }

    // ==========================================
    // 8. updateUserRoles TESTS
    // ==========================================

    @Test
    void shouldUpdateUserRolesSuccessfully() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "userId", 1L,
                "roles", List.of("Admin")
        );

        doNothing().when(userService).updateUserRoles(anyLong(), anyList());

        mockMvc.perform(post("/api/roles/updateUserRoles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(content().string(BaseConstants.UPDATE_USER_ROLES_SUCCESS_MSG));
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdateUserRolesFails() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "userId", 1L,
                "roles", List.of("Admin")
        );

        doThrow(new RuntimeException("Error")).when(userService).updateUserRoles(anyLong(), anyList());

        mockMvc.perform(post("/api/roles/updateUserRoles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(BaseConstants.UNEXPECTED_ERROR_MSG));
    }
}