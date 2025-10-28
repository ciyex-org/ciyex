package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Coverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoverageRepository extends JpaRepository<Coverage, Long> {
    List<Coverage> findByPatientIdOrderByEffectiveDateDesc(Long patientId);
    
    @Query(value = "SELECT pp.ehr_patient_id FROM public.portal_patients pp " +
                   "JOIN public.portal_users pu ON pp.portal_user_id = pu.id " +
                   "WHERE pu.email = :email LIMIT 1", nativeQuery = true)
    Long findEhrPatientIdByPortalUserEmail(@Param("email") String email);
}
