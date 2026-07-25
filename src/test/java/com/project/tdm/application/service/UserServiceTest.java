package com.project.tdm.application.service;

import com.project.tdm.application.dto.UserRolesDTO;
import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.service.impl.UserServiceImpl;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.security.util.HashPassUtil;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private HashPassUtil hashPassUtil;

    @Mock
    private RoleService roleService;

    @Mock
    private PageService pageService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService.setUserRepo(userRepo);
    }

    // ==========================================
    // registerUser TESTS
    // ==========================================

    @Test
    void shouldRegisterUserSuccessfullyWhenNoDuplicatesExist() {
        // Arrange
        UserEntity inputUser = new UserEntity();
        inputUser.setUsername("newuser");
        inputUser.setEmail("new@example.com");
        inputUser.setPassword("plainPassword");

        RoleEntity viewerRole = new RoleEntity(BaseConstants.VIEWER_NAME, BaseConstants.VIEWER_DESC);

        when(userRepo.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepo.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(hashPassUtil.hashPassword("plainPassword")).thenReturn("hashedPassword");
        when(roleService.getDefaultRole(BaseConstants.VIEWER_NAME, BaseConstants.VIEWER_DESC)).thenReturn(viewerRole);

        // Act
        assertDoesNotThrow(() -> userService.registerUser(inputUser));

        // Assert
        assertEquals("hashedPassword", inputUser.getPassword());
        verify(userRepo, times(1)).save(inputUser);
    }

    @Test
    void shouldThrowExceptionWhenEmailIsDuplicateDuringRegistration() {
        // Arrange
        UserEntity inputUser = new UserEntity();
        inputUser.setUsername("user1");
        inputUser.setEmail("duplicate@example.com");

        when(userRepo.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(inputUser)
        );

        assertEquals("Email has been taken.", exception.getMessage());
        verify(userRepo, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsDuplicateDuringRegistration() {
        // Arrange
        UserEntity inputUser = new UserEntity();
        inputUser.setUsername("duplicateUser");
        inputUser.setEmail("unique@example.com");

        when(userRepo.existsByEmailIgnoreCase("unique@example.com")).thenReturn(false);
        when(userRepo.existsByUsernameIgnoreCase("duplicateUser")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.registerUser(inputUser)
        );

        assertEquals("Username has been taken.", exception.getMessage());
        verify(userRepo, never()).save(any(UserEntity.class));
    }

    // ==========================================
    // loginUser TESTS
    // ==========================================

    @Test
    void shouldLoginSuccessfullyWithValidUsernameAndPassword() {
        // Arrange
        UserEntity loginAttempt = new UserEntity();
        loginAttempt.setUsername("testuser");
        loginAttempt.setPassword("correctPassword");

        UserEntity databaseUser = new UserEntity();
        databaseUser.setUsername("testuser");
        databaseUser.setPassword("hashedPassword");

        when(userRepo.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(databaseUser));
        when(hashPassUtil.verifyPassword("correctPassword", "hashedPassword")).thenReturn(true);

        // Act
        UserEntity result = userService.loginUser(loginAttempt);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void shouldLoginSuccessfullyWithValidEmailAndPassword() {
        // Arrange
        UserEntity loginAttempt = new UserEntity();
        loginAttempt.setEmail("test@example.com");
        loginAttempt.setPassword("correctPassword");

        UserEntity databaseUser = new UserEntity();
        databaseUser.setEmail("test@example.com");
        databaseUser.setPassword("hashedPassword");

        when(userRepo.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(databaseUser));
        when(hashPassUtil.verifyPassword("correctPassword", "hashedPassword")).thenReturn(true);

        // Act
        UserEntity result = userService.loginUser(loginAttempt);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByUsernameOrEmail() {
        // Arrange
        UserEntity loginAttempt = new UserEntity();
        loginAttempt.setUsername("nonexistent");

        when(userRepo.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.loginUser(loginAttempt)
        );

        assertEquals("Invalid credentials. Please try again.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPasswordVerificationFails() {
        // Arrange
        UserEntity loginAttempt = new UserEntity();
        loginAttempt.setUsername("testuser");
        loginAttempt.setPassword("wrongPassword");

        UserEntity databaseUser = new UserEntity();
        databaseUser.setUsername("testuser");
        databaseUser.setPassword("hashedPassword");

        when(userRepo.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(databaseUser));
        when(hashPassUtil.verifyPassword("wrongPassword", "hashedPassword")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.loginUser(loginAttempt)
        );

        assertEquals("Invalid credentials. Please try again.", exception.getMessage());
    }

    // ==========================================
    // getUserByUsername TESTS
    // ==========================================

    @Test
    void shouldReturnUserWhenUsernameExists() {
        // Arrange
        UserEntity expectedUser = new UserEntity();
        expectedUser.setUsername("existingUser");

        when(userRepo.findByUsernameIgnoreCase("existingUser")).thenReturn(Optional.of(expectedUser));

        // Act
        UserEntity result = userService.getUserByUsername("existingUser");

        // Assert
        assertNotNull(result);
        assertEquals("existingUser", result.getUsername());
    }

    @Test
    void shouldReturnNullWhenUsernameDoesNotExist() {
        // Arrange
        when(userRepo.findByUsernameIgnoreCase("unknownUser")).thenReturn(Optional.empty());

        // Act
        UserEntity result = userService.getUserByUsername("unknownUser");

        // Assert
        assertNull(result);
    }

    // ==========================================
    // generateUsersRoles TESTS
    // ==========================================

    @Test
    void shouldGenerateUsersRolesWithoutKeyword() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUserId(1L);
        user.setUsername("user1");
        Page<UserEntity> userPage = new PageImpl<>(List.of(user));

        when(userRepo.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        Map<String, Object> result = userService.generateUsersRoles(null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.get("totalItems"));
        assertEquals(1, result.get("totalPages"));
        assertTrue(result.get("result") instanceof List);
    }

    @Test
    void shouldGenerateUsersRolesWithKeyword() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUserId(1L);
        user.setUsername("searchUser");
        Page<UserEntity> userPage = new PageImpl<>(List.of(user));

        when(userRepo.findByUsernameContainingIgnoreCase(eq("search"), any(Pageable.class))).thenReturn(userPage);

        // Act
        Map<String, Object> result = userService.generateUsersRoles("search", 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.get("totalItems"));
    }

    // ==========================================
    // updateUserRoles TESTS
    // ==========================================

    @Test
    void shouldUpdateUserRolesSuccessfully() {
        // Arrange
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setUsername("testuser");

        RoleEntity oldRole = new RoleEntity("Viewer", "Viewer desc");
        user.addRole(oldRole);

        RoleEntity newRole = new RoleEntity("Admin", "Admin desc");

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(roleService.getRolesByRoleNames(any())).thenReturn(Set.of(newRole));
        when(userRepo.save(any(UserEntity.class))).thenReturn(user);

        // Act
        assertDoesNotThrow(() -> userService.updateUserRoles(userId, List.of("Admin")));

        // Assert
        verify(userRepo, times(1)).save(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingRolesForNonexistentUser() {
        // Arrange
        Long userId = 99L;
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                userService.updateUserRoles(userId, List.of("Admin"))
        );

        assertEquals(BaseConstants.ROLE_RECORD_NOT_AVAILABLE, exception.getMessage());
        verify(userRepo, never()).save(any(UserEntity.class));
    }
}