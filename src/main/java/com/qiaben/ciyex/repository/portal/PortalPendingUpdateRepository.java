package com.qiaben.ciyex.repository.portal;

import com.qiaben.ciyex.entity.portal.PortalPendingUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing portal pending updates (review queue)
 */
@Repository
public interface PortalPendingUpdateRepository extends JpaRepository<PortalPendingUpdate, Long> {

    /**
     * Find all pending updates by status, ordered by creation date (newest first)
     */
    List<PortalPendingUpdate> findByStatusOrderByCreatedDateDesc(String status);

    /**
     * Find all updates for a specific user, ordered by creation date (newest first)
     */
    List<PortalPendingUpdate> findByUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * Find pending updates by update type
     */
    List<PortalPendingUpdate> findByStatusAndUpdateTypeOrderByCreatedDateDesc(String status, String updateType);

    /**
     * Find pending updates by priority
     */
    List<PortalPendingUpdate> findByStatusAndPriorityOrderByCreatedDateDesc(String status, String priority);

    /**
     * Count pending updates for a user
     */
    long countByUserIdAndStatus(Long userId, String status);

    /**
     * Count all pending updates
     */
    long countByStatus(String status);

    /**
     * Find updates within date range
     */
    @Query("SELECT p FROM PortalPendingUpdate p WHERE p.createdDate BETWEEN :startDate AND :endDate ORDER BY p.createdDate DESC")
    List<PortalPendingUpdate> findByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find high priority pending updates
     */
    @Query("SELECT p FROM PortalPendingUpdate p WHERE p.status = 'PENDING' AND p.priority IN ('HIGH', 'URGENT') ORDER BY p.createdDate DESC")
    List<PortalPendingUpdate> findHighPriorityPending();

    /**
     * Find updates by approver
     */
    List<PortalPendingUpdate> findByApprovedByOrderByReviewedDateDesc(String approvedBy);
}