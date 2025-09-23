package com.qiaben.ciyex.repository.portal;

import com.qiaben.ciyex.entity.portal.PortalDemographics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortalDemographicsRepository extends JpaRepository<PortalDemographics, Long> {

    /**
     * Find demographics record by linked patient ID
     */
    Optional<PortalDemographics> findByPatient_Id(Long patientId);

    /**
     * Check if a demographics record exists for the given patient
     */
    boolean existsByPatient_Id(Long patientId);

    /**
     * 🔹 Direct lookup: find demographics by PortalUser.id (via patient.user.id)
     */
    Optional<PortalDemographics> findByPatient_User_Id(Long userId);

    /**
     * 🔹 Check existence by PortalUser.id
     */
    boolean existsByPatient_User_Id(Long userId);
}
