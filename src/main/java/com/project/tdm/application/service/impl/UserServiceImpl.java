package com.project.tdm.application.service.impl;

import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.repository.UserRepo;
import com.project.tdm.application.service.UserService;
import com.project.tdm.security.util.HashPassUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private HashPassUtil hashPassUtil;

    private UserRepo userRepo;

    @Autowired
    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    private Optional<String> checkDuplicateUsername(UserEntity user) {
        Optional<String> usernameFound = userRepo.existsByUsername(user.getUsername()) ?
                Optional.of("Username has been taken.") :
                Optional.empty();
        logger.info("checkDuplicateUsername(): username = {}, isFound = {}", user.getUsername(), usernameFound.isPresent());

        return usernameFound;
    }

    private Optional<String> checkDuplicateEmail(UserEntity user) {
        Optional<String> emailFound = userRepo.existsByEmail(user.getEmail()) ?
                Optional.of("Email has been taken.") :
                Optional.empty();
        logger.info("checkDuplicateEmail(): email = {}, isFound = {}", user.getEmail(), emailFound.isPresent());

        return emailFound;
    }

    @Transactional
    @Override
    public void registerUser(UserEntity user) {
        checkDuplicateEmail(user)
                .or(() -> checkDuplicateUsername(user))
                .ifPresent(errMsg -> { throw new IllegalArgumentException(errMsg); });

        user.setPassword(hashPassUtil.hashPassword(user.getPassword()));

        userRepo.save(user);
    }

    @Override
    public UserEntity loginUser(UserEntity user) {
        UserEntity matchUser = Optional.ofNullable(user.getUsername())
                        .flatMap(username -> userRepo.findByUsername(username))
                .or(() -> Optional.ofNullable(user.getEmail())
                        .flatMap(email -> userRepo.findByEmail(email)))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials. Please try again."));

        if (!hashPassUtil.verifyPassword(user.getPassword(), matchUser.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials. Please try again.");
        }

        return matchUser;
    }

    @Override
    public UserEntity getUserByUsername(String username) {
        UserEntity userEntity = userRepo.findByUsername(username).orElse(null);
        logger.info("getUserByUsername(): username = {}, resultFound = {}", username, (userEntity != null));

        return userEntity;
    }
}
