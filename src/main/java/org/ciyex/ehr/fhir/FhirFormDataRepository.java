package org.ciyex.ehr.fhir;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FhirFormDataRepository extends JpaRepository<FhirFormDataEntity, Long> {

    Optional<FhirFormDataEntity> findByResourceTypeAndResourceIdAndOrgAlias(
            String resourceType, String resourceId, String orgAlias);

    void deleteByResourceIdAndOrgAlias(String resourceId, String orgAlias);
}
