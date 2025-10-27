package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByPatientId(Long patientId);
    // findById is inherited from JpaRepository
}