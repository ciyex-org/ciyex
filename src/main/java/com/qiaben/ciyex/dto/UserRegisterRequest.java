package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.model.User.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;
}
