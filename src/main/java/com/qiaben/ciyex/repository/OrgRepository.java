package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Org;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrgRepository extends JpaRepository<Org, Long> {
    Optional<Org> findByOrgName(String orgName);
    Optional<Org> findById(Long id);
}
