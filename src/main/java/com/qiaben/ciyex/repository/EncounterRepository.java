
package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByOrgId(Long orgId);
    List<Encounter> findByPatientIdAndOrgId(Long patientId, Long orgId);

    Optional<Encounter> findByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);

    // Delete with scoping
    long deleteByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);
    // Additional query methods can be added here if necessary
}

//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Encounter;
//import com.qiaben.ciyex.entity.EncounterStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.*;
//import org.springframework.data.repository.query.Param;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//public interface EncounterRepository extends JpaRepository<Encounter, Long> {
//
//    // Patient-scoped list
//    List<Encounter> findByPatientIdAndOrgIdOrderByIdDesc(Long patientId, Long orgId);
//    List<Encounter> findByPatientIdAndOrgIdAndStatusOrderByIdDesc(Long patientId, Long orgId, EncounterStatus status);
//
//    Optional<Encounter> findByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);
//
//    long deleteByIdAndPatientIdAndOrgId(Long id, Long patientId, Long orgId);
//
//    // ---- Review queries (filters are optional) ----
//    @Query("""
//      select e from Encounter e
//       where e.orgId = :orgId
//         and (:status is null or e.status = :status)
//         and (:provider is null or e.encounterProvider = :provider)
//         and (:from  is null or e.encounterDate >= :from)
//         and (:to    is null or e.encounterDate <  :to)
//       order by e.encounterDate desc nulls last, e.id desc
//    """)
//    Page<Encounter> reviewList(@Param("orgId") Long orgId,
//                               @Param("status") EncounterStatus status,
//                               @Param("provider") String provider,
//                               @Param("from") Instant from,
//                               @Param("to") Instant to,
//                               Pageable pageable);
//
//    @Query("""
//      select e.status as status, count(e) as c
//        from Encounter e
//       where e.orgId = :orgId
//         and (:provider is null or e.encounterProvider = :provider)
//         and (:from  is null or e.encounterDate >= :from)
//         and (:to    is null or e.encounterDate <  :to)
//       group by e.status
//    """)
//    List<Object[]> countByStatus(@Param("orgId") Long orgId,
//                                 @Param("provider") String provider,
//                                 @Param("from") Instant from,
//                                 @Param("to") Instant to);
//}
