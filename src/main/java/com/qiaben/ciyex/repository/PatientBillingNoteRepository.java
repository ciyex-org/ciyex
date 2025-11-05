package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.NoteTargetType;
import com.qiaben.ciyex.entity.PatientBillingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface PatientBillingNoteRepository extends JpaRepository<PatientBillingNote, Long> {
    List<PatientBillingNote> findByPatientIdAndTargetTypeAndTargetIdOrderByIdAsc(
            Long patientId, NoteTargetType type, Long targetId
    );

    List<PatientBillingNote> findByPatientIdAndTargetTypeAndTargetIdOrderByCreatedDateAsc(Long patientId, NoteTargetType targetType, Long targetId);

    List<PatientBillingNote> findByPatientId(Long patientId);
}