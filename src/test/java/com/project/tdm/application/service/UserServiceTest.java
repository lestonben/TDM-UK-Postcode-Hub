package com.project.tdm.application.service;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.service.impl.UserServiceImpl;
import com.project.tdm.security.util.HashPassUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private HashPassUtil hashPassUtil; // Injected as a mock bean instead of static

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // Explicitly set the repository via the setter method used in your implementation
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

        when(userRepo.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepo.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(hashPassUtil.hashPassword("plainPassword")).thenReturn("hashedPassword");

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
}