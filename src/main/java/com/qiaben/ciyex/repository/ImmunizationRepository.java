//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Immunization;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
//}


package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Immunization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
    List<Immunization> findByEncounter_Id(Long encounterId);
}

//package com.qiaben.ciyex.repository;
//
//import com.qiaben.ciyex.entity.Immunization;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface ImmunizationRepository extends JpaRepository<Immunization, Long> {
//
//    // Find immunizations by encounter ID
//    List<Immunization> findByEncounter_Id(Long encounterId);
//
//    // Find immunizations by encounter ID and org ID
//    List<Immunization> findByEncounter_IdAndOrgId(Long encounterId, Long orgId);
//}
