package com.project.tdm.application.service.impl;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.service.UserService;
import com.project.tdm.security.util.HashPassUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepo userRepo;

    @Autowired
    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    private Optional<String> checkDuplicateUsername(UserEntity user) {
        return userRepo.existsByUsername(user.getUsername()) ? Optional.of("Username has been taken.") : Optional.empty();
    }

    private Optional<String> checkDuplicateEmail(UserEntity user) {
        return userRepo.existsByEmail(user.getEmail()) ? Optional.of("Email has been taken.") : Optional.empty();
    }

    @Transactional
    @Override
    public void registerUser(UserEntity user) {
        checkDuplicateEmail(user)
                .or(() -> checkDuplicateUsername(user))
                .ifPresent(errMsg -> { throw new IllegalArgumentException(errMsg); });

        String salt = HashPassUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(HashPassUtil.hashPassword(user.getPassword(), salt));

        userRepo.save(user);
    }

    @Override
    public UserEntity loginUser(UserEntity user) {
        UserEntity matchUser = Optional.ofNullable(user.getUsername())
                        .flatMap(username -> userRepo.findByUsername(username))
                .or(() -> Optional.ofNullable(user.getEmail())
                        .flatMap(email -> userRepo.findByEmail(email)))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials. Please try again."));

        if (!HashPassUtil.verifyPassword(user.getPassword(), matchUser.getPassword(), matchUser.getSalt())) {
            throw new IllegalArgumentException("Invalid credentials. Please try again.");
        }

        return matchUser;
    }

    @Override
    public UserEntity getUserByUsername(String username) {
        return userRepo.findByUsername(username).orElse(null);
    }
}
