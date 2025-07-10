package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.UserRegisterRequest;
import com.qiaben.ciyex.model.User;

import java.util.List;

public interface UserService {
    User register(UserRegisterRequest request);
    List<User> getAllUsers();
    User getUserById(Long id);
    User updateUser(Long id, UserRegisterRequest request);
    void deleteUser(Long id);
}
