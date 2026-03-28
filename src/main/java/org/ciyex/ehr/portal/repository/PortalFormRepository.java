package org.ciyex.ehr.portal.repository;

import org.ciyex.ehr.portal.entity.PortalForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortalFormRepository extends JpaRepository<PortalForm, Long> {

    List<PortalForm> findByOrgAliasOrderByPosition(String orgAlias);

    List<PortalForm> findByOrgAliasAndFormTypeOrderByPosition(String orgAlias, String formType);

    List<PortalForm> findByOrgAliasAndActiveOrderByPosition(String orgAlias, boolean active);

    List<PortalForm> findByOrgAliasAndFormTypeAndActiveOrderByPosition(String orgAlias, String formType, boolean active);

    Optional<PortalForm> findByOrgAliasAndFormKey(String orgAlias, String formKey);

    @Query("SELECT pf FROM PortalForm pf WHERE (pf.orgAlias = :orgAlias OR pf.orgAlias = '__DEFAULT__') " +
           "AND pf.formType = :formType AND pf.active = true ORDER BY pf.position")
    List<PortalForm> findActiveFormsWithDefaults(String orgAlias, String formType);

    @Query("SELECT pf FROM PortalForm pf WHERE (pf.orgAlias = :orgAlias OR pf.orgAlias = '__DEFAULT__') " +
           "AND pf.active = true ORDER BY pf.position")
    List<PortalForm> findAllActiveWithDefaults(String orgAlias);
}
