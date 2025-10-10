package com.qiaben.ciyex.repository.portal;

import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    /**
     * Find portal user by email
     */
    Optional<PortalUser> findByEmail(String email);

    /**
     * Case-insensitive email search
     */
    Optional<PortalUser> findByEmailIgnoreCase(String email);

    /**
     * Find all users by status
     */
    List<PortalUser> findByStatus(PortalStatus status);

    /**
     * Find all users by status and orgId
     */
    List<PortalUser> findByStatusAndOrgId(PortalStatus status, Long orgId);

    /**
     * Find all pending users for approval
     */
    @Query("SELECT pu FROM PortalUser pu WHERE pu.status = 'PENDING' ORDER BY pu.createdDate ASC")
    List<PortalUser> findPendingUsers();

    /**
     * Find all pending users for a specific organization
     */
    @Query("SELECT pu FROM PortalUser pu WHERE pu.status = 'PENDING' AND pu.orgId = :orgId ORDER BY pu.createdDate ASC")
    List<PortalUser> findPendingUsersByOrgId(@Param("orgId") Long orgId);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Find by UUID
     */
    Optional<PortalUser> findByUuid(java.util.UUID uuid);

    /**
     * Count users by status
     */
    long countByStatus(PortalStatus status);

    /**
     * Count users by status and organization
     */
    long countByStatusAndOrgId(PortalStatus status, Long orgId);
}