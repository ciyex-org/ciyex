package org.ciyex.ehr.inventory.repository;

import org.ciyex.ehr.inventory.entity.InvSupplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvSupplierRepository extends JpaRepository<InvSupplier, Long> {

    List<InvSupplier> findByOrgAlias(String orgAlias);

    List<InvSupplier> findByOrgAliasAndActive(String orgAlias, Boolean active);

    Page<InvSupplier> findByOrgAlias(String orgAlias, Pageable pageable);

    long countByOrgAlias(String orgAlias);
}
