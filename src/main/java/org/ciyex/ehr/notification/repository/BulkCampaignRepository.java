package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.BulkCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BulkCampaignRepository extends JpaRepository<BulkCampaign, Long> {
    List<BulkCampaign> findByOrgAliasOrderByCreatedAtDesc(String orgAlias);

    Optional<BulkCampaign> findByIdAndOrgAlias(Long id, String orgAlias);

    List<BulkCampaign> findByOrgAliasAndStatus(String orgAlias, String status);

    long countByOrgAliasAndStatus(String orgAlias, String status);
}
