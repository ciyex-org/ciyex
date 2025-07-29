package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {
    private User user;
    private List<UserOrgRoleRequest> roles;

}
