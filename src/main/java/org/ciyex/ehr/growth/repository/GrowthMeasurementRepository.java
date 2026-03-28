package org.ciyex.ehr.growth.repository;

import org.ciyex.ehr.growth.entity.GrowthMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GrowthMeasurementRepository extends JpaRepository<GrowthMeasurement, Long> {

    List<GrowthMeasurement> findByOrgAliasAndPatientIdOrderByMeasurementDateDesc(
            String orgAlias, Long patientId);

    List<GrowthMeasurement> findByOrgAliasAndPatientIdAndGenderOrderByAgeMonthsAsc(
            String orgAlias, Long patientId, String gender);

    Optional<GrowthMeasurement> findByIdAndOrgAlias(Long id, String orgAlias);
}
