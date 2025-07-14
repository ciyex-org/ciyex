package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.UserFacilityRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFacilityRoleRepository extends JpaRepository<UserFacilityRole, Long> {

    // All facilities and roles for a user
    List<UserFacilityRole> findByUser_Id(Long userId);

    // All users and roles in a facility
    List<UserFacilityRole> findByFacility_Id(Long facilityId);

    // All assignments in a facility for a role
    List<UserFacilityRole> findByFacility_IdAndRole(Long facilityId, RoleName roleName);

    // All assignments in an org (via facility)
    List<UserFacilityRole> findByFacility_Org_Id(Long orgId);

    // All assignments for a user in an org
    List<UserFacilityRole> findByUser_IdAndFacility_Org_Id(Long userId, Long orgId);

    // Unique assignment: for assign/remove logic
    Optional<UserFacilityRole> findByUser_IdAndFacility_IdAndRole(Long userId, Long facilityId, RoleName roleName);
}
