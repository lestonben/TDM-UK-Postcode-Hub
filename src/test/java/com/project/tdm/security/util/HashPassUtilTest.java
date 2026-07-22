package com.project.tdm.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashPassUtilTest {

    private HashPassUtil hashPassUtil;

    @BeforeEach
    void setUp() {
        hashPassUtil = new HashPassUtil();
    }

    @Test
    void shouldProduceValidHashedPassword() {
        // Arrange
        String password = "mySecurePassword123";

        // Act
        String hashedPassword = hashPassUtil.hashPassword(password);

        // Assert
        assertNotNull(hashedPassword);
        assertNotEquals(password, hashedPassword, "Hashed password should not match plain text");
        // BCrypt hashes typically start with $2a$ or $2b$ and are 60 characters long
        assertTrue(hashedPassword.startsWith("$2"), "BCrypt hash should start with version identifier");
    }

    @Test
    void shouldProduceDifferentHashesForSamePasswordDueToRandomSalts() {
        // Arrange
        String password = "samePassword";

        // Act
        String hash1 = hashPassUtil.hashPassword(password);
        String hash2 = hashPassUtil.hashPassword(password);

        // Assert
        assertNotNull(hash1);
        assertNotNull(hash2);
        // BCrypt automatically uses a fresh random salt every time, yielding different outputs
        assertNotEquals(hash1, hash2, "Hashing the same password twice must produce different hashes due to auto-salting");
    }

    @Test
    void shouldReturnTrueWhenVerifyingCorrectPassword() {
        // Arrange
        String password = "userPassword";
        String storedHash = hashPassUtil.hashPassword(password);

        // Act
        boolean isValid = hashPassUtil.verifyPassword(password, storedHash);

        // Assert
        assertTrue(isValid, "Verification should pass when the correct password is provided");
    }

    @Test
    void shouldReturnFalseWhenVerifyingWrongPassword() {
        // Arrange
        String correctPassword = "userPassword";
        String wrongPassword = "wrongPassword";
        String storedHash = hashPassUtil.hashPassword(correctPassword);

        // Act
        boolean isValid = hashPassUtil.verifyPassword(wrongPassword, storedHash);

        // Assert
        assertFalse(isValid, "Verification should fail if the input password does not match");
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        // Act & Assert
        assertFalse(hashPassUtil.verifyPassword(null, "someHash"), "Should return false if input password is null");
        assertFalse(hashPassUtil.verifyPassword("somePassword", null), "Should return false if stored hash is null");
        assertFalse(hashPassUtil.verifyPassword(null, null), "Should return false if both are null");
    }
}