package com.project.tdm.application.service;

import com.project.tdm.application.entity.RoleEntity;
import com.project.tdm.application.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserService {

    void registerUser(UserEntity user);

    UserEntity loginUser(UserEntity user);

    UserEntity getUserByUsername(String username);

    Map<String, Object> generateUsersRoles(String keyword, int page, int size);

    void updateUserRoles(Long id, List<String> roleNames);
}
