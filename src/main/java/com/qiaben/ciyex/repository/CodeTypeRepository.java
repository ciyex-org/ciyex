package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.CodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodeTypeRepository extends JpaRepository<CodeType, Long> {

    List<CodeType> findByOrgIdAndPatientId(Long orgId, Long patientId);

    List<CodeType> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);

    @Query("""
      select ct from CodeType ct
       where ct.orgId = :orgId and ct.patientId = :patientId and ct.encounterId = :encounterId
         and (:codeTypeKey is null or ct.codeTypeKey = :codeTypeKey)
         and (:active is null or ct.active = :active)
         and (
              :q is null or :q = '' or
              lower(ct.label) like lower(concat('%', :q, '%')) or
              lower(ct.justification) like lower(concat('%', :q, '%')) or
              lower(ct.mask) like lower(concat('%', :q, '%'))
         )
       order by ct.codeTypeKey, ct.sequenceNumber
    """)
    List<CodeType> searchInEncounter(Long orgId,
                                     Long patientId,
                                     Long encounterId,
                                     String codeTypeKey,
                                     Boolean active,
                                     String q);
}
