package com.qiaben.ciyex.repository;

import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Patient;
import com.qiaben.ciyex.service.TenantSchemaInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class TenantAwarePatientRepository {
    
    @Autowired
    private PatientRepository patientRepository;
    
    @Autowired
    private TenantSchemaInitializer tenantSchemaInitializer;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public Patient saveWithTenantSchema(Patient patient) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Initialize tenant schema and create tables automatically using JPA
                tenantSchemaInitializer.initializeTenantSchema(context.getOrgId());
                
                // Set search path for this transaction
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.info("Set search_path to: {}, public for patient save", schemaName);
                
                // Flush to ensure schema is set before save
                entityManager.flush();
                
                // Save the patient
                Patient savedPatient = patientRepository.save(patient);
                
                // Flush again to ensure save happens in correct schema
                entityManager.flush();
                
                log.info("Successfully saved patient with id: {} in schema: {}", savedPatient.getId(), schemaName);
                
                return savedPatient;
                
            } catch (Exception e) {
                log.error("Failed to save patient in tenant schema: {}", schemaName, e);
                throw new RuntimeException("Failed to save patient in tenant schema", e);
            }
        } else {
            // No tenant context, save normally
            return patientRepository.save(patient);
        }
    }
    
    @Transactional(readOnly = true)
    public Optional<Patient> findByIdWithTenantSchema(Long id) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Set search path for this transaction
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.info("Set search_path to: {}, public for patient query", schemaName);
                
                // Flush to ensure schema is set before query
                entityManager.flush();
                
                // Find the patient
                Optional<Patient> patient = patientRepository.findById(id);
                
                log.info("Successfully queried patient with id: {} from schema: {}", id, schemaName);
                
                return patient;
                
            } catch (Exception e) {
                log.error("Failed to query patient from tenant schema: {}", schemaName, e);
                throw new RuntimeException("Failed to query patient from tenant schema", e);
            }
        } else {
            // No tenant context, query normally
            return patientRepository.findById(id);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Patient> findAllWithTenantSchema() {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Set search path for this transaction
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.info("Set search_path to: {}, public for patient list query", schemaName);
                
                // Flush to ensure schema is set before query
                entityManager.flush();
                
                // Find all patients
                List<Patient> patients = patientRepository.findAll();
                
                log.info("Successfully queried {} patients from schema: {}", patients.size(), schemaName);
                
                return patients;
                
            } catch (Exception e) {
                log.error("Failed to query patients from tenant schema: {}", schemaName, e);
                throw new RuntimeException("Failed to query patients from tenant schema", e);
            }
        } else {
            // No tenant context, query normally
            return patientRepository.findAll();
        }
    }
    
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Patient> findAllWithTenantSchema(org.springframework.data.domain.Pageable pageable) {
        RequestContext context = RequestContext.get();
        if (context != null && context.getOrgId() != null) {
            String schemaName = "practice_" + context.getOrgId();
            
            try {
                // Set search path for this transaction
                entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public").executeUpdate();
                
                log.info("Set search_path to: {}, public for paginated patient query", schemaName);
                
                // Flush to ensure schema is set before query
                entityManager.flush();
                
                // Find all patients with pagination
                org.springframework.data.domain.Page<Patient> patients = patientRepository.findAll(pageable);
                
                log.info("Successfully queried {} patients from schema: {} (page {} of {})", 
                    patients.getNumberOfElements(), schemaName, patients.getNumber() + 1, patients.getTotalPages());
                
                return patients;
                
            } catch (Exception e) {
                log.error("Failed to query paginated patients from tenant schema: {}", schemaName, e);
                throw new RuntimeException("Failed to query paginated patients from tenant schema", e);
            }
        } else {
            // No tenant context, query normally
            return patientRepository.findAll(pageable);
        }
    }
}
