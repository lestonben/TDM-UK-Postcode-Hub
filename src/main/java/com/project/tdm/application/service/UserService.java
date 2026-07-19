package com.project.tdm.application.service;

import com.project.tdm.application.entity.UserEntity;

public interface UserService {

    void registerUser(UserEntity user);
    UserEntity loginUser(UserEntity user);
    UserEntity getUserByUsername(String username);
}
