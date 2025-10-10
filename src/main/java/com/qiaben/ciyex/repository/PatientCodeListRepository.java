package com.qiaben.ciyex.repository;




import com.qiaben.ciyex.entity.PatientCodeList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PatientCodeListRepository extends JpaRepository<PatientCodeList, Long> {

    List<PatientCodeList> findAllByOrgIdOrderByOrderIndexAsc(Long orgId);

    Optional<PatientCodeList> findByIdAndOrgId(Long id, Long orgId);

    @Transactional
    @Modifying
    @Query("update PatientCodeList p set p.isDefault = false " +
            "where p.orgId = :orgId and p.isDefault = true and p.id <> :keepId")
    void clearDefaultsExcept(Long orgId, Long keepId);

    @Transactional
    @Modifying
    @Query("update PatientCodeList p set p.isDefault = false where p.orgId = :orgId")
    void clearAllDefaults(Long orgId);

    @Transactional
    void deleteByIdAndOrgId(Long id, Long orgId);
}
