package com.qiaben.ciyex.auth.scope;

import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import com.qiaben.ciyex.repository.UserRepository;
import com.qiaben.ciyex.security.SuperAdminConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserScopeService {

    private static final String SEP = "\\|";    // regex for split
    private static final String JOIN = "|";     // joiner
    private static final String ON = "1";       // granted flag

    private final ScopeRepository scopeRepo;
    private final RoleScopeTemplateRepository templateRepo;
    private final UserScopeFlagsRepository flagsRepo;
    private final UserRepository userRepo;
    private final SuperAdminConfig superAdminConfig;

    /**
     * Read effective scopes for a user (normal users from packed flags; super admin -> all active).
     */
    @Transactional(readOnly = true)
    public List<String> getActiveScopeCodesByUserId(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (superAdminConfig.isSuperAdmin(user)) {
            return scopeRepo.findAllByActiveTrueOrderByIdAsc()
                    .stream().map(Scope::getCode).toList();
        }

        List<Scope> master = scopeRepo.findAllByActiveTrueOrderByIdAsc();
        var opt = flagsRepo.findByUserId(userId);

        if (opt.isEmpty()) {
            // No row yet: derive defaults from role templates, then persist packed row
            Set<String> defaults = defaultsFromRoles(user);
            String packed = pack(defaults, scopeRepo.findAllByOrderByIdAsc()); // pack against full master list
            UserScopeFlags f = new UserScopeFlags();
            f.setUserId(userId);
            f.setFlags(packed);
            f.setUpdatedAt(Instant.now());
            flagsRepo.save(f);
            return defaults.stream().filter(code ->
                    master.stream().anyMatch(s -> s.getCode().equals(code))
            ).toList();
        }

        // Row exists: unpack (tolerant to master changes)
        UserScopeFlags f = opt.get();
        return unpack(f.getFlags(), master);
    }

    /**
     * Explicitly assign a set of codes (replaces flags for the user).
     * Missing codes become empty slots in the packed string.
     */
    @Transactional
    public void replaceUserScopes(Long userId, Collection<String> desiredCodes) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (superAdminConfig.isSuperAdmin(user)) {
            // Super admin is implicit; do not store row
            return;
        }

        // Validate codes against existing master (ignore unknowns)
        Set<String> granted = new LinkedHashSet<>(desiredCodes == null ? List.of() : desiredCodes);
        Set<String> valid = scopeRepo.findAllByOrderByIdAsc().stream()
                .map(Scope::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
        granted.retainAll(valid);

        String packed = pack(granted, scopeRepo.findAllByOrderByIdAsc());
        UserScopeFlags flags = flagsRepo.findByUserId(userId).orElseGet(() -> {
            UserScopeFlags nf = new UserScopeFlags();
            nf.setUserId(userId);
            return nf;
        });
        flags.setFlags(packed);
        flags.setUpdatedAt(Instant.now());
        flagsRepo.save(flags);
    }

    /**
     * Assign defaults based on roles (called after user creation).
     */
    @Transactional
    public void assignDefaultScopesFromUserOrgRoles(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (superAdminConfig.isSuperAdmin(user)) return;

        Set<String> defaults = defaultsFromRoles(user);
        String packed = pack(defaults, scopeRepo.findAllByOrderByIdAsc());
        UserScopeFlags flags = flagsRepo.findByUserId(userId).orElseGet(() -> {
            UserScopeFlags nf = new UserScopeFlags();
            nf.setUserId(userId);
            return nf;
        });
        flags.setFlags(packed);
        flags.setUpdatedAt(Instant.now());
        flagsRepo.save(flags);
    }

    // ---------- helpers ----------

    private Set<String> defaultsFromRoles(User user) {
        Set<RoleName> roles = user.getUserOrgRoles().stream()
                .map(UserOrgRole::getRole)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        roles.removeIf(r -> "SUPER_ADMIN".equals(r.name()));

        Set<String> out = new LinkedHashSet<>();
        for (RoleName role : roles) {
            templateRepo.findScopesByRole(role).forEach(s -> out.add(s.getCode()));
        }
        return out;
    }

    /**
     * Pack a granted set into a '|' delimited string against the full master list (ordered by id).
     * Example: master = [A,B,C,D,E], granted = {B,E} => flags = "|1|||1"
     */
    private String pack(Set<String> granted, List<Scope> masterByIdAsc) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < masterByIdAsc.size(); i++) {
            index.put(masterByIdAsc.get(i).getCode(), i);
        }
        String[] slots = new String[masterByIdAsc.size()];
        Arrays.fill(slots, "");
        for (String code : granted) {
            Integer i = index.get(code);
            if (i != null) slots[i] = ON;
        }
        return String.join(JOIN, slots);
    }

    /**
     * Unpack flags string to granted codes, tolerant to length mismatches.
     * If flags shorter than master, remaining scopes are treated as NOT granted.
     */
    private List<String> unpack(String flags, List<Scope> activeMasterByIdAsc) {
        if (flags == null) flags = "";
        String[] parts = flags.split(SEP, -1); // keep trailing empties
        List<String> out = new ArrayList<>();
        for (int i = 0; i < activeMasterByIdAsc.size(); i++) {
            String slot = (i < parts.length ? parts[i] : "");
            if (ON.equals(slot)) {
                out.add(activeMasterByIdAsc.get(i).getCode());
            }
        }
        return out;
    }
}
