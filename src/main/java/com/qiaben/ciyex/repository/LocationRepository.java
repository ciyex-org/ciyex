package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByExternalId(String externalId);

    @Query("SELECT l.externalId FROM Location l")
    List<String> findAllExternalIdsBy();

    @Query("""
            SELECT l FROM Location l
            WHERE LOWER(l.name) LIKE %:search%
               OR LOWER(l.address) LIKE %:search%
               OR LOWER(l.city) LIKE %:search%
               OR LOWER(l.state) LIKE %:search%
               OR LOWER(l.country) LIKE %:search%
            """)
    Page<Location> searchBy(String search, Pageable pageable);
}
 