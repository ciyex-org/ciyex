package org.ciyex.ehr.lab.repository;

import org.ciyex.ehr.lab.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    List<LabResult> findByOrgAliasAndPatientIdOrderByCollectedDateDesc(String orgAlias, Long patientId);

    List<LabResult> findByOrgAliasAndLabOrderIdOrderByTestNameAsc(String orgAlias, Long labOrderId);

    List<LabResult> findByOrgAliasAndPatientIdAndLoincCodeOrderByCollectedDateAsc(String orgAlias, Long patientId, String loincCode);

    List<LabResult> findByOrgAliasAndPatientIdAndPanelNameOrderByCollectedDateDescTestNameAsc(String orgAlias, Long patientId, String panelName);

    List<LabResult> findByOrgAliasOrderByCreatedAtDesc(String orgAlias);

    long countByOrgAliasAndStatus(String orgAlias, String status);
}
