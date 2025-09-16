////package com.qiaben.ciyex.repository;
////
////import com.qiaben.ciyex.entity.ChiefComplaint;
////import org.springframework.data.jpa.repository.JpaRepository;
////import org.springframework.stereotype.Repository;
////
////import java.util.List;
////
////@Repository
////public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {
////    // Custom query to fetch Chief Complaints by encounter ID
////    List<ChiefComplaint> findByEncounterId(Long encounterId);
////}
//
//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.ChiefComplaint;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {
//    List<ChiefComplaint> findByEncounterId(Long encounterId);
//    List<ChiefComplaint> findByPatientId(Long patientId);
//    Optional<ChiefComplaint> findByIdAndPatientId(Long id, Long patientId);
//
//}
//
//
//
//

package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.ChiefComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChiefComplaintRepository extends JpaRepository<ChiefComplaint, Long> {
    List<ChiefComplaint> findByOrgIdAndPatientIdAndEncounterId(Long orgId, Long patientId, Long encounterId);
    Optional<ChiefComplaint> findByOrgIdAndPatientIdAndEncounterIdAndId(Long orgId, Long patientId, Long encounterId, Long id);
}
