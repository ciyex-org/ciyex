package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.PatientHistory;
import com.qiaben.ciyex.repository.PatientHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientHistoryService {
    
    private final PatientHistoryRepository repository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public Object saveHistory(Long patientId, Object historyData) {
        try {
            PatientHistory history = repository.findByPatientId(patientId)
                .orElse(new PatientHistory());
            
            history.setPatientId(patientId);
            history.setHistoryData(objectMapper.writeValueAsString(historyData));
            

            
            repository.save(history);
            return historyData;
        } catch (Exception e) {
            log.error("Failed to save history for patient {}", patientId, e);
            throw new RuntimeException("Failed to save patient history", e);
        }
    }
    
    @Transactional(readOnly = true)
    public Object getHistory(Long patientId) {
        try {
            PatientHistory history = repository.findByPatientId(patientId).orElse(null);
            
            if (history == null || history.getHistoryData() == null) {
                return null;
            }
            
            return objectMapper.readValue(history.getHistoryData(), Object.class);
        } catch (Exception e) {
            log.error("Failed to retrieve history for patient {}", patientId, e);
            throw new RuntimeException("Failed to retrieve patient history", e);
        }
    }

}