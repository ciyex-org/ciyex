package org.ciyex.ehr.notification.repository;

import org.ciyex.ehr.notification.entity.PatientCommPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PatientCommPreferenceRepository extends JpaRepository<PatientCommPreference, Long> {
    Optional<PatientCommPreference> findByOrgAliasAndPatientId(String orgAlias, Long patientId);

    List<PatientCommPreference> findByOrgAlias(String orgAlias);

    List<PatientCommPreference> findByOrgAliasAndEmailOptInTrue(String orgAlias);

    List<PatientCommPreference> findByOrgAliasAndSmsOptInTrue(String orgAlias);
}
