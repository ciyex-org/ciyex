package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.WasteLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WasteLogRepository extends JpaRepository<WasteLog, Long> {

    Page<WasteLog> findByOrgAlias(String orgAlias, Pageable pageable);

    List<WasteLog> findByItemId(Long itemId);

    List<WasteLog> findByItemIdOrderByCreatedAtDesc(Long itemId);
}
