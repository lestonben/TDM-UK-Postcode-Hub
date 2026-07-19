package com.project.tdm.security.util;

import com.project.tdm.application.entity.UserEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // The signing key needs to be at least 256 bits (32 characters) for HMAC-SHA256
    private final String testSecret = "mySuperSecureAndVeryLongSecretKeyForTesting123!";
    private final long testExpiration = 60000; // 1 minute in milliseconds

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(testSecret, testExpiration);
    }

    @Test
    void shouldGenerateValidTokenWithCorrectSubject() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUsername("testUser");

        // Act
        String token = jwtUtil.generateToken(user);

        // Assert
        assertNotNull(token);

        // Extract claims manually via the utility to verify the payload
        String subject = jwtUtil.extractClaim(token, Claims::getSubject);
        assertEquals("testUser", subject);
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUsername("activeUser");
        String token = jwtUtil.generateToken(user);

        // Act
        boolean isValid = jwtUtil.validateToken(token, user);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void shouldFailValidationWhenUsernameDoesNotMatch() {
        // Arrange
        UserEntity tokenUser = new UserEntity();
        tokenUser.setUsername("userA");
        String token = jwtUtil.generateToken(tokenUser);

        UserEntity validationUser = new UserEntity();
        validationUser.setUsername("userB");

        // Act
        boolean isValid = jwtUtil.validateToken(token, validationUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void shouldIdentifyThatTokenIsNotExpired() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUsername("someUser");
        String token = jwtUtil.generateToken(user);

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    void shouldIdentifyExpiredToken() throws InterruptedException {
        // Arrange: Create a short-lived utility instance (1ms expiration)
        JwtUtil shortLivedJwtUtil = new JwtUtil(testSecret, 1);
        UserEntity user = new UserEntity();
        user.setUsername("quickUser");

        String token = shortLivedJwtUtil.generateToken(user);

        // Wait 5 milliseconds to guarantee expiration
        Thread.sleep(5);

        // Act
        boolean isExpired = shortLivedJwtUtil.isTokenExpired(token);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    void shouldExtractCustomClaimsCorrectly() {
        // Arrange
        UserEntity user = new UserEntity();
        user.setUsername("claimUser");
        String token = jwtUtil.generateToken(user);

        // Act
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);

        // Assert
        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(expiration.after(issuedAt));
    }
}