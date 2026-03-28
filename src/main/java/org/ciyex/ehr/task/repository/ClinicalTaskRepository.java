package org.ciyex.ehr.task.repository;

import org.ciyex.ehr.task.entity.ClinicalTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClinicalTaskRepository extends JpaRepository<ClinicalTask, Long> {

    Page<ClinicalTask> findByOrgAlias(String orgAlias, Pageable pageable);

    List<ClinicalTask> findByOrgAliasAndStatusOrderByDueDateAsc(String orgAlias, String status);

    List<ClinicalTask> findByOrgAliasAndAssignedToOrderByDueDateAsc(String orgAlias, String assignedTo);

    List<ClinicalTask> findByOrgAliasAndPatientIdOrderByCreatedAtDesc(String orgAlias, Long patientId);

    long countByOrgAliasAndStatus(String orgAlias, String status);

    @Query("SELECT COUNT(t) FROM ClinicalTask t WHERE t.orgAlias = :org " +
           "AND t.status NOT IN ('completed', 'cancelled') AND t.dueDate < :today")
    long countOverdue(@Param("org") String orgAlias, @Param("today") LocalDate today);

    @Query("SELECT t FROM ClinicalTask t WHERE t.orgAlias = :org " +
           "AND t.status NOT IN ('completed', 'cancelled') AND t.dueDate < :today " +
           "ORDER BY t.dueDate ASC")
    List<ClinicalTask> findOverdue(@Param("org") String orgAlias, @Param("today") LocalDate today);

    @Query("SELECT t FROM ClinicalTask t WHERE t.orgAlias = :org AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.assignedTo) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(t.patientName) LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "ORDER BY t.createdAt DESC")
    List<ClinicalTask> search(@Param("org") String org, @Param("q") String q);

    @Query(value = "SELECT t.* FROM clinical_task t WHERE t.org_alias = :org " +
           "AND (CAST(:q AS varchar) IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%')) " +
           "    OR LOWER(t.assigned_to) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%')) " +
           "    OR LOWER(t.patient_name) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%'))) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:taskType AS varchar) IS NULL OR t.task_type = CAST(:taskType AS varchar)) " +
           "AND (CAST(:overdue AS boolean) = false OR (t.status NOT IN ('completed', 'cancelled') AND t.due_date < CAST(:today AS date))) " +
           "ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM clinical_task t WHERE t.org_alias = :org " +
           "AND (CAST(:q AS varchar) IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%')) " +
           "    OR LOWER(t.assigned_to) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%')) " +
           "    OR LOWER(t.patient_name) LIKE LOWER(CONCAT('%', CAST(:q AS varchar), '%'))) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:taskType AS varchar) IS NULL OR t.task_type = CAST(:taskType AS varchar)) " +
           "AND (CAST(:overdue AS boolean) = false OR (t.status NOT IN ('completed', 'cancelled') AND t.due_date < CAST(:today AS date)))",
           nativeQuery = true)
    Page<ClinicalTask> findFiltered(@Param("org") String org,
                                     @Param("q") String q,
                                     @Param("status") String status,
                                     @Param("priority") String priority,
                                     @Param("taskType") String taskType,
                                     @Param("overdue") boolean overdue,
                                     @Param("today") LocalDate today,
                                     Pageable pageable);
}
