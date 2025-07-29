package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.User;
import lombok.Data;

import java.util.List;

@Data

public class UserCreateRequest {
    private User user;
    private List<UserOrgRoleRequest> roles; // DTO with orgId and role as string
}

