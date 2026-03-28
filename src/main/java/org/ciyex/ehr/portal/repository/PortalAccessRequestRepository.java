package org.ciyex.ehr.portal.repository;

import org.ciyex.ehr.portal.entity.PortalAccessRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalAccessRequestRepository extends JpaRepository<PortalAccessRequest, Long> {
    Page<PortalAccessRequest> findByOrgAlias(String orgAlias, Pageable pageable);
    List<PortalAccessRequest> findByOrgAliasAndStatus(String orgAlias, String status);
    long countByOrgAliasAndStatus(String orgAlias, String status);
}
