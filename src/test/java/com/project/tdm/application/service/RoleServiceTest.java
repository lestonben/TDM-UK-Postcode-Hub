package com.project.tdm.application.service;

import com.project.tdm.application.entity.PageEntity;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.RoleRepo;
import com.project.tdm.application.service.impl.RoleServiceImpl;
import com.project.tdm.application.utilities.constant.BaseConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepo roleRepo;

    @Mock
    private PageService pageService;

    @InjectMocks
    private RoleServiceImpl roleService;

    // ==========================================
    // 1. getRoleById TESTS
    // ==========================================

    @Test
    void shouldReturnRoleWhenIdExists() {
        RoleEntity role = new RoleEntity("Admin", "Admin Role");
        role.setId(1L);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));

        RoleEntity result = roleService.getRoleById(1L);

        assertNotNull(result);
        assertEquals("Admin", result.getName());
    }

    @Test
    void shouldReturnNullWhenIdDoesNotExist() {
        when(roleRepo.findById(99L)).thenReturn(Optional.empty());

        RoleEntity result = roleService.getRoleById(99L);

        assertNull(result);
    }

    // ==========================================
    // 2. getDefaultRole TESTS
    // ==========================================

    @Test
    void shouldReturnExistingDefaultRoleWhenFound() {
        RoleEntity role = new RoleEntity("Viewer", "Viewer Role");
        when(roleRepo.findByName("Viewer")).thenReturn(Optional.of(role));

        RoleEntity result = roleService.getDefaultRole("Viewer", "Viewer Role");

        assertNotNull(result);
        assertEquals("Viewer", result.getName());
        verify(roleRepo, never()).save(any());
    }

    @Test
    void shouldCreateAndReturnDefaultRoleWhenNotFound() {
        RoleEntity role = new RoleEntity("Viewer", "Viewer Role");
        when(roleRepo.findByName("Viewer")).thenReturn(Optional.empty());
        when(roleRepo.save(any(RoleEntity.class))).thenReturn(role);

        RoleEntity result = roleService.getDefaultRole("Viewer", "Viewer Role");

        assertNotNull(result);
        assertEquals("Viewer", result.getName());
        verify(roleRepo, times(1)).save(any(RoleEntity.class));
    }

    // ==========================================
    // 3. updateDefaultRole TESTS
    // ==========================================

    @Test
    void shouldUpdateDefaultRoleSuccessfully() {
        RoleEntity role = new RoleEntity("Admin", "Updated Desc");
        when(roleRepo.save(role)).thenReturn(role);

        RoleEntity result = roleService.updateDefaultRole(role);

        assertNotNull(result);
        assertEquals("Updated Desc", result.getDescription());
        verify(roleRepo, times(1)).save(role);
    }

    // ==========================================
    // 4. generateRolesPages TESTS
    // ==========================================

    @Test
    void shouldGenerateRolesPagesSuccessfully() {
        RoleEntity role = new RoleEntity("Admin", "Admin Desc");
        role.setId(1L);
        Page<RoleEntity> rolePage = new PageImpl<>(List.of(role));

        when(roleRepo.findAll(any(Pageable.class))).thenReturn(rolePage);

        Map<String, Object> result = roleService.generateRolesPages(0, 10);

        assertNotNull(result);
        assertEquals(1L, result.get("totalItems"));
        assertEquals(1, result.get("totalPages"));
        assertTrue(result.get("result") instanceof List);
    }

    // ==========================================
    // 5. createNewRole TESTS
    // ==========================================

    @Test
    void shouldCreateNewRoleSuccessfully() {
        when(roleRepo.existsByNameIgnoreCase("Manager")).thenReturn(false);
        when(pageService.getPagesByUrlPatterns(anyList())).thenReturn(Set.of(new PageEntity("Home", "/home")));
        when(roleRepo.save(any(RoleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> roleService.createNewRole("Manager", "Manager Desc", List.of("/home")));
        verify(roleRepo, times(1)).save(any(RoleEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenRoleNameIsDuplicateDuringCreation() {
        when(roleRepo.existsByNameIgnoreCase("Manager")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roleService.createNewRole("Manager", "Manager Desc", List.of("/home"))
        );

        assertEquals(BaseConstants.DUPLICATE_ROLE_NAME_MSG, ex.getMessage());
        verify(roleRepo, never()).save(any());
    }

    // ==========================================
    // 6. updateRole TESTS
    // ==========================================

    @Test
    void shouldUpdateRoleSuccessfully() {
        RoleEntity role = new RoleEntity("Editor", "Old Desc");
        role.setId(1L);
        PageEntity page = new PageEntity("Dashboard", "/dashboard");
        role.setAllowedPages(new HashSet<>(Set.of(page)));

        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));
        when(pageService.getPagesByUrlPatterns(anyList())).thenReturn(Set.of(new PageEntity("Update", "/update")));
        when(roleRepo.save(any(RoleEntity.class))).thenReturn(role);

        assertDoesNotThrow(() -> roleService.updateRole(1L, "New Desc", List.of("/update")));
        assertEquals("New Desc", role.getDescription());
        verify(roleRepo, times(1)).save(role);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentRole() {
        when(roleRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roleService.updateRole(99L, "Desc", List.of())
        );

        assertEquals(BaseConstants.ROLE_RECORD_NOT_AVAILABLE, ex.getMessage());
    }

    // ==========================================
    // 7. deleteRole TESTS
    // ==========================================

    @Test
    void shouldDeleteRoleSuccessfully() {
        RoleEntity role1 = new RoleEntity("Role1", "Desc1");
        role1.setId(1L);
        RoleEntity role2 = new RoleEntity("Role2", "Desc2");
        role2.setId(2L);

        when(roleRepo.count()).thenReturn(2L);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(role1));

        assertDoesNotThrow(() -> roleService.deleteRole(1L));
        verify(roleRepo, times(1)).delete(role1);
    }

    @Test
    void shouldThrowExceptionWhenOnlyOneRoleRemains() {
        when(roleRepo.count()).thenReturn(1L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roleService.deleteRole(1L)
        );

        assertEquals(BaseConstants.ONLY_ROLE_RECORD_CANNOT_DELETE, ex.getMessage());
        verify(roleRepo, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonexistentRole() {
        when(roleRepo.count()).thenReturn(2L);
        when(roleRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roleService.deleteRole(99L)
        );

        assertEquals(BaseConstants.ROLE_RECORD_NOT_AVAILABLE, ex.getMessage());
    }

    // ==========================================
    // 8. getRoleUserCount TESTS
    // ==========================================

    @Test
    void shouldReturnRoleUserCountFromDatabase() {
        RoleEntity role = new RoleEntity("Admin", "Admin");
        role.setId(1L);
        UserEntity user = new UserEntity();
        role.getUsers().add(user);

        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));

        int count = roleService.getRoleUserCount(1L);

        assertEquals(1, count);
    }

    @Test
    void shouldThrowExceptionWhenGettingUserCountForNonexistentRole() {
        when(roleRepo.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                roleService.getRoleUserCount(99L)
        );

        assertEquals(BaseConstants.ROLE_RECORD_NOT_AVAILABLE, ex.getMessage());
    }

    // ==========================================
    // 9. getAllRoleNamesList TESTS
    // ==========================================

    @Test
    void shouldReturnAllRoleNames() {
        RoleEntity role1 = new RoleEntity("Admin", "Desc");
        RoleEntity role2 = new RoleEntity("Viewer", "Desc");
        when(roleRepo.findAll()).thenReturn(List.of(role1, role2));

        Set<String> names = roleService.getAllRoleNamesList();

        assertEquals(2, names.size());
        assertTrue(names.contains("Admin"));
        assertTrue(names.contains("Viewer"));
    }

    // ==========================================
    // 10. getRolesByRoleNames TESTS
    // ==========================================

    @Test
    void shouldReturnRolesByNames() {
        RoleEntity role = new RoleEntity("Admin", "Desc");
        when(roleRepo.findByNameIn(anyList())).thenReturn(List.of(role));

        Set<RoleEntity> roles = roleService.getRolesByRoleNames(Set.of("Admin"));

        assertEquals(1, roles.size());
    }
}