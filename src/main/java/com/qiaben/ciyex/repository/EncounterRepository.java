




package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Encounter;
import com.qiaben.ciyex.entity.EncounterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    List<Encounter> findByPatientId(Long patientId);

    Optional<Encounter> findByIdAndPatientId(Long id, Long patientId);

    // Delete with scoping
    long deleteByIdAndPatientId(Long id, Long patientId);

    Page<Encounter> findAll(Pageable pageable);

    Page<Encounter> findByStatus(EncounterStatus status, Pageable pageable);

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
//    List<Encounter> findByPatientIdOrderByIdDesc(Long patientId);
//    List<Encounter> findByPatientIdAndStatusOrderByIdDesc(Long patientId, EncounterStatus status);
//
//    Optional<Encounter> findByIdAndPatientId(Long id, Long patientId);
//
//    long deleteByIdAndPatientId(Long id, Long patientId);
//
//    // ---- Review queries (filters are optional) ----
//    @Query("""
//      select e from Encounter e
//       where 1=1
//         and (:status is null or e.status = :status)
//         and (:provider is null or e.encounterProvider = :provider)
//         and (:from  is null or e.encounterDate >= :from)
//         and (:to    is null or e.encounterDate <  :to)
//       order by e.encounterDate desc nulls last, e.id desc
//    """)
//    Page<Encounter> reviewList(//                               @Param("status") EncounterStatus status,
//                               @Param("provider") String provider,
//                               @Param("from") Instant from,
//                               @Param("to") Instant to,
//                               Pageable pageable);
//
//    @Query("""
//      select e.status as status, count(e) as c
//        from Encounter e
//       where 1=1
//         and (:provider is null or e.encounterProvider = :provider)
//         and (:from  is null or e.encounterDate >= :from)
//         and (:to    is null or e.encounterDate <  :to)
//       group by e.status
//    """)
//    List<Object[]> countByStatus(//                                 @Param("provider") String provider,
//                                 @Param("from") Instant from,
//                                 @Param("to") Instant to);
//}
