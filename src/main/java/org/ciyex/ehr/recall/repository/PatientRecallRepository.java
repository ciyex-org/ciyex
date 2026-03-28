package org.ciyex.ehr.recall.repository;

import org.ciyex.ehr.recall.entity.PatientRecall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PatientRecallRepository extends JpaRepository<PatientRecall, Long> {

    Page<PatientRecall> findByOrgAlias(String orgAlias, Pageable pageable);

    List<PatientRecall> findByOrgAliasAndPatientIdOrderByDueDateDesc(String orgAlias, Long patientId);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT COUNT(r) FROM PatientRecall r WHERE r.orgAlias = :org " +
           "AND r.status NOT IN ('COMPLETED', 'CANCELLED') AND r.dueDate < :today")
    long countOverdue(@Param("org") String orgAlias, @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM PatientRecall r WHERE r.orgAlias = :org " +
           "AND r.dueDate = :today AND r.status NOT IN ('COMPLETED', 'CANCELLED')")
    long countDueToday(@Param("org") String orgAlias, @Param("today") LocalDate today);

    @Query("SELECT COUNT(r) FROM PatientRecall r WHERE r.orgAlias = :org " +
           "AND r.status = 'COMPLETED' AND r.completedDate >= :startOfMonth AND r.completedDate <= :endOfMonth")
    long countCompletedThisMonth(@Param("org") String orgAlias,
                                 @Param("startOfMonth") LocalDate startOfMonth,
                                 @Param("endOfMonth") LocalDate endOfMonth);

    @Query("SELECT r FROM PatientRecall r WHERE r.orgAlias = :org " +
           "AND (CAST(:status AS string) IS NULL OR r.status = :status) " +
           "AND (:typeId IS NULL OR r.recallType.id = :typeId) " +
           "AND (:providerId IS NULL OR r.providerId = :providerId) " +
           "AND (CAST(:dueDateFrom AS date) IS NULL OR r.dueDate >= :dueDateFrom) " +
           "AND (CAST(:dueDateTo AS date) IS NULL OR r.dueDate <= :dueDateTo) " +
           "AND (CAST(:search AS string) IS NULL OR CAST(:search AS string) = '' OR " +
           "     LOWER(r.patientName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "     LOWER(r.providerName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "     LOWER(r.recallTypeName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "     CAST(r.patientId AS text) = :search)")
    Page<PatientRecall> findFiltered(@Param("org") String orgAlias,
                                     @Param("status") String status,
                                     @Param("typeId") Long typeId,
                                     @Param("providerId") Long providerId,
                                     @Param("dueDateFrom") LocalDate dueDateFrom,
                                     @Param("dueDateTo") LocalDate dueDateTo,
                                     @Param("search") String search,
                                     Pageable pageable);
}
