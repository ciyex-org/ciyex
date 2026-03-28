package org.ciyex.ehr.recall.repository;

import org.ciyex.ehr.recall.entity.RecallOutreachLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecallOutreachLogRepository extends JpaRepository<RecallOutreachLog, Long> {

    List<RecallOutreachLog> findByRecallIdOrderByAttemptNumberDesc(Long recallId);

    long countByRecallId(Long recallId);
}
