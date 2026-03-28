package org.ciyex.ehr.lab.repository;

import org.ciyex.ehr.lab.entity.LabOrderSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LabOrderSetRepository extends JpaRepository<LabOrderSet, Long> {
    List<LabOrderSet> findByOrgAliasInAndActiveTrue(List<String> orgAliases);

    Optional<LabOrderSet> findByCodeAndOrgAliasIn(String code, List<String> orgAliases);
}
