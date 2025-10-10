package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.entity.RoleName;
import com.qiaben.ciyex.entity.User;
import com.qiaben.ciyex.entity.UserOrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrgRoleRepository extends JpaRepository<UserOrgRole, Long> {

    // Find roles for a given user and org
    List<UserOrgRole> findByUserAndOrg(User user, Org org);

    // Find all roles for a given user (across all orgs)
    List<UserOrgRole> findByUser(User user);

    // Find all users with a specific role
    List<UserOrgRole> findByRole(RoleName role);

    List<UserOrgRole> findByUserId(Long id);
    
    @Query("SELECT DISTINCT uor.role FROM UserOrgRole uor")
    List<RoleName> findDistinctRoles();
}

