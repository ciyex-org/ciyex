package com.qiaben.ciyex.auth.scope.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignUserScopesRequest {
    private List<String> scopeCodes;
    private boolean active = true;
}
