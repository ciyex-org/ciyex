package org.ciyex.ehr.prescription.repository;

import org.ciyex.ehr.prescription.entity.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    List<DrugInteraction> findByDrugACodeOrDrugBCode(String drugACode, String drugBCode);

    List<DrugInteraction> findByDrugACodeAndDrugBCode(String drugACode, String drugBCode);

    @Query("SELECT di FROM DrugInteraction di WHERE " +
           "(di.orgAlias = '__GLOBAL__' OR di.orgAlias = :org) AND " +
           "di.drugACode = :codeA AND di.drugBCode = :codeB")
    List<DrugInteraction> findInteraction(
            @Param("org") String orgAlias,
            @Param("codeA") String drugACode,
            @Param("codeB") String drugBCode);
}
