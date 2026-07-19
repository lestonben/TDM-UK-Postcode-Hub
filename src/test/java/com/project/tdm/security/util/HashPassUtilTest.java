package com.project.tdm.security.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashPassUtilTest {

    @Test
    void shouldGenerateUniqueRandomSalts() {
        // Act
        String salt1 = HashPassUtil.generateSalt();
        String salt2 = HashPassUtil.generateSalt();

        // Assert
        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2, "Salts should be unique and random on every call");

        // Verify it is a valid Base64 string by decoding it without exceptions
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(salt1));
    }

    @Test
    void shouldProduceConsistentHashedPasswordForSameInputAndSalt() {
        // Arrange
        String password = "mySecurePassword123";
        String salt = HashPassUtil.generateSalt();

        // Act
        String hash1 = HashPassUtil.hashPassword(password, salt);
        String hash2 = HashPassUtil.hashPassword(password, salt);

        // Assert
        assertNotNull(hash1);
        assertEquals(hash1, hash2, "Hashing the same password with the same salt must produce the exact same string");
    }

    @Test
    void shouldProduceDifferentHashesForSamePasswordWithDifferentSalts() {
        // Arrange
        String password = "samePassword";
        String salt1 = HashPassUtil.generateSalt();
        String salt2 = HashPassUtil.generateSalt();

        // Act
        String hash1 = HashPassUtil.hashPassword(password, salt1);
        String hash2 = HashPassUtil.hashPassword(password, salt2);

        // Assert
        assertNotEquals(hash1, hash2, "Different salts must produce completely distinct hash outputs");
    }

    @Test
    void shouldReturnTrueWhenVerifyingCorrectPasswordAndSalt() {
        // Arrange
        String password = "userPassword";
        String salt = HashPassUtil.generateSalt();
        String storedHash = HashPassUtil.hashPassword(password, salt);

        // Act
        boolean isValid = HashPassUtil.verifyPassword(password, storedHash, salt);

        // Assert
        assertTrue(isValid, "Verification should pass when credentials match the stored hash");
    }

    @Test
    void shouldReturnFalseWhenVerifyingWrongPassword() {
        // Arrange
        String correctPassword = "userPassword";
        String wrongPassword = "wrongPassword";
        String salt = HashPassUtil.generateSalt();
        String storedHash = HashPassUtil.hashPassword(correctPassword, salt);

        // Act
        boolean isValid = HashPassUtil.verifyPassword(wrongPassword, storedHash, salt);

        // Assert
        assertFalse(isValid, "Verification should fail if the input password does not match");
    }
}