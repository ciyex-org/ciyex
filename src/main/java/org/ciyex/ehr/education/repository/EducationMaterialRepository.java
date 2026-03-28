package org.ciyex.ehr.education.repository;

import org.ciyex.ehr.education.entity.EducationMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EducationMaterialRepository extends JpaRepository<EducationMaterial, Long> {

    Page<EducationMaterial> findByOrgAliasOrderByCreatedAtDesc(String orgAlias, Pageable pageable);

    List<EducationMaterial> findByOrgAliasAndCategoryOrderByTitleAsc(String orgAlias, String category);

    List<EducationMaterial> findByOrgAliasAndIsActiveTrueOrderByTitleAsc(String orgAlias);

    @Query("SELECT m FROM EducationMaterial m WHERE m.orgAlias = :org AND (" +
           "LOWER(m.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(m.category) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(m.source) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(m.author) LIKE LOWER(CONCAT('%',:q,'%'))" +
           ") ORDER BY m.createdAt DESC")
    List<EducationMaterial> search(@Param("org") String orgAlias, @Param("q") String query);
}
