package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.security.SuperAdminConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/scopes")
@RequiredArgsConstructor
public class ScopeAdminController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.qiaben.ciyex.util.JwtTokenUtil jwtTokenUtil;

    private final UserScopeService userScopeService;
    private final ScopeRepository scopeRepository;
    private final ScopeSeeder scopeSeeder;
    private final UserRepository userRepository;       // ✅
    private final SuperAdminConfig superAdminConfig;   // ✅

    @PostMapping("/seed")
    public ApiResponse<Map<String, Object>> seedNow() {
        return ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("seeded")
                .data(scopeSeeder.seed())
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<Scope>> listAll() {
        return ApiResponse.<List<Scope>>builder()
                .success(true)
                .message("OK")
                .data(scopeRepository.findAll())
                .build();
    }

    @PostMapping("/users/{userId}/assign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignToUser(
            @PathVariable Long userId,
            @RequestBody com.qiaben.ciyex.auth.scope.dto.AssignUserScopesRequest req
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (superAdminConfig.isSuperAdmin(user)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Super admin has implicit full access; no scope rows are stored.")
                            .data(Map.of("userId", userId))
                            .build()
            );
        }

        // NEW: replace entire flags row from provided codes
        userScopeService.replaceUserScopes(userId, req.getScopeCodes());

        // Generate a fresh JWT for this user reflecting new scopes
        String token = jwtTokenUtil.generateToken(user);

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("assigned and token issued")
                        .data(Map.of(
                                "userId", userId,
                                "count", (req.getScopeCodes()==null?0:req.getScopeCodes().size()),
                                "token", token
                        ))
                        .build()
        );
    }
}
