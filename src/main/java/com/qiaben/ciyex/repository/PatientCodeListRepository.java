package com.qiaben.ciyex.repository;




import com.qiaben.ciyex.entity.PatientCodeList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface PatientCodeListRepository extends JpaRepository<PatientCodeList, Long> {

    @Query("SELECT p FROM PatientCodeList p ORDER BY p.orderIndex ASC")
    List<PatientCodeList> findAllOrderByOrderIndexAsc();

    Optional<PatientCodeList> findById(Long id);

    @Transactional
    @Modifying
    @Query("update PatientCodeList p set p.isDefault = false " +
            "where p.isDefault = true and p.id <> :keepId")
    void clearDefaultsExcept(Long keepId);

    @Transactional
    @Modifying
    @Query("update PatientCodeList p set p.isDefault = false where 1=1")
    void clearAllDefaults();

    @Transactional
    void deleteById(Long id);
}
