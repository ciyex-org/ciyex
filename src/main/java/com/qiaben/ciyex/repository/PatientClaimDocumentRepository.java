package com.qiaben.ciyex.repository;





import com.qiaben.ciyex.entity.PatientClaim;
import com.qiaben.ciyex.entity.PatientClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientClaimDocumentRepository extends JpaRepository<PatientClaimDocument, Long> {
    List<PatientClaimDocument> findByClaimAndType(PatientClaim claim, PatientClaimDocument.Type type);
    long countByClaimAndType(PatientClaim claim, PatientClaimDocument.Type type);
}
