package com.project.tdm.security.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class HashPassUtil {

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hashPassword(String password, String salt) {
        try {
            // Combine the raw password text and the unique salt sequence string
            String saltedPassword = password + salt;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(saltedPassword.getBytes());

            // Convert the binary byte array into a clean database-friendly String
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 encryption algorithm not found in environment.", e);
        }
    }

    public static boolean verifyPassword(String inputPassword, String storedPassword, String storedSalt) {
        return storedPassword.equals(hashPassword(inputPassword, storedSalt));
    }
}
