package org.ciyex.ehr.scanning.repository;

import org.ciyex.ehr.scanning.entity.ScannedDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScannedDocumentRepository extends JpaRepository<ScannedDocument, Long> {

    @Query(value = """
            SELECT * FROM scanned_documents d
            WHERE d.org_alias = CAST(:orgAlias AS varchar)
              AND (CAST(:q AS varchar) IS NULL OR (LOWER(CAST(d.file_name AS text)) LIKE LOWER('%' || CAST(:q AS varchar) || '%')
                   OR LOWER(CAST(d.original_file_name AS text)) LIKE LOWER('%' || CAST(:q AS varchar) || '%')))
              AND (CAST(:category AS varchar) IS NULL OR d.category = CAST(:category AS varchar))
              AND (CAST(:ocrStatus AS varchar) IS NULL OR d.ocr_status = CAST(:ocrStatus AS varchar))
            ORDER BY d.created_at DESC
            """, nativeQuery = true)
    Page<ScannedDocument> search(
            @Param("orgAlias") String orgAlias,
            @Param("q") String q,
            @Param("category") String category,
            @Param("ocrStatus") String ocrStatus,
            Pageable pageable);
}
