package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.GlobalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlobalCodeRepository extends JpaRepository<GlobalCode, Long> {

    List<GlobalCode> findByOrgId(Long orgId);

    @Query("""
      select c from GlobalCode c
       where c.orgId = :orgId
         and (:codeType is null or c.codeType = :codeType)
         and (:active is null or c.active = :active)
         and (
              :q is null or :q = '' or
              lower(c.code) like lower(concat('%', :q, '%')) or
              lower(c.description) like lower(concat('%', :q, '%')) or
              lower(c.shortDescription) like lower(concat('%', :q, '%')) or
              lower(c.category) like lower(concat('%', :q, '%'))
         )
       order by c.codeType, c.code
    """)
    List<GlobalCode> search(Long orgId, String codeType, Boolean active, String q);
}
