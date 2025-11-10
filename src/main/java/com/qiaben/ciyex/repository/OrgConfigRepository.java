package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.OrgConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OrgConfig entity operations (simple key-value pairs)
 */
@Repository
public interface OrgConfigRepository extends JpaRepository<OrgConfig, Long> {

    /**
     * Find configuration by key
     */
    Optional<OrgConfig> findByKey(String key);

    /**
     * Find all configurations with keys starting with prefix
     * Example: findByKeyStartingWith("fhir_") returns all FHIR-related configs
     */
    List<OrgConfig> findByKeyStartingWith(String prefix);

    /**
     * Check if key exists
     */
    boolean existsByKey(String key);

    /**
     * Delete configuration by key
     */
    @Modifying
    void deleteByKey(String key);


    /**
     * Update value for a specific key
     */
    @Modifying
    @Query("UPDATE OrgConfig c SET c.value = :value WHERE c.key = :key")
    int updateValueByKey(@Param("key") String key, @Param("value") String value);
}
