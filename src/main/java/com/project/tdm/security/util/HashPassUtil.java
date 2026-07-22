package com.project.tdm.security.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashPassUtil {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hashPassword(String password) {
        return bcrypt.encode(password);
    }

    public boolean verifyPassword(String inputPassword, String storedPassword) {
        return (inputPassword != null && storedPassword != null) && bcrypt.matches(inputPassword, storedPassword);
    }
}
