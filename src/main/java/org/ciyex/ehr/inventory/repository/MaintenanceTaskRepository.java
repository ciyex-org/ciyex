package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.MaintenanceTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {

    Page<MaintenanceTask> findByOrgAlias(String orgAlias, Pageable pageable);

    List<MaintenanceTask> findByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAliasAndStatusAndDueDateBefore(String orgAlias, String status, LocalDate date);
}
