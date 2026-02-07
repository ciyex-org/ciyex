package com.qiaben.ciyex.eligibility.service;

import com.qiaben.ciyex.eligibility.client.ClearinghouseClient;
import com.qiaben.ciyex.eligibility.dto.EligibilityRequestDto;
import com.qiaben.ciyex.eligibility.dto.EligibilityResponseDto;
import com.qiaben.ciyex.eligibility.edi.X12_270Builder;
import com.qiaben.ciyex.eligibility.edi.X12_271Parser;
import com.qiaben.ciyex.eligibility.entity.EligibilityTransaction;
import com.qiaben.ciyex.eligibility.repository.EligibilityRepository;
import com.qiaben.ciyex.service.PracticeContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Slf4j
public class EligibilityService {
    
    private final X12_270Builder x12Builder;
    private final X12_271Parser x12Parser;
    private final ClearinghouseClient clearinghouseClient;
    private final EligibilityRepository repository;
    private final PracticeContextService practiceContextService;
    
    public EligibilityService(
            X12_270Builder x12Builder,
            X12_271Parser x12Parser,
            ClearinghouseClient clearinghouseClient,
            EligibilityRepository repository,
            PracticeContextService practiceContextService) {
        this.x12Builder = x12Builder;
        this.x12Parser = x12Parser;
        this.clearinghouseClient = clearinghouseClient;
        this.repository = repository;
        this.practiceContextService = practiceContextService;
    }
    
    public EligibilityResponseDto checkEligibility(EligibilityRequestDto request) {
        log.info("Starting eligibility check for member: {}", request.getMemberId());
        
        // Step 1: Build X12 270 request
        String x12Request = x12Builder.build(
            request,
            clearinghouseClient.getSenderId(),
            clearinghouseClient.getReceiverId()
        );
        log.debug("Generated X12 270 request:\n{}", x12Request);
        
        // Step 2: Send to clearinghouse
        String x12Response = clearinghouseClient.sendEligibilityRequest(x12Request);
        log.debug("Received X12 271 response:\n{}", x12Response);
        
        // Step 3: Parse X12 271 response
        EligibilityResponseDto response = x12Parser.parse(x12Response);
        
        // Step 4: Store transaction in database
        EligibilityTransaction transaction = new EligibilityTransaction();
        transaction.setTransactionId(response.getTransactionId());
        transaction.setMemberId(request.getMemberId());
        transaction.setPayerId(request.getPayerId());
        transaction.setPayerName(response.getPayerName());
        transaction.setStatus(response.getStatus());
        transaction.setPlanName(response.getPlanName());
        transaction.setCoverageStartDate(response.getCoverageStartDate());
        transaction.setCoverageEndDate(response.getCoverageEndDate());
        transaction.setCopayAmount(response.getCopayAmount());
        transaction.setDeductibleAmount(response.getDeductibleAmount());
        transaction.setOutOfPocketMax(response.getOutOfPocketMax());
        transaction.setX12Request(x12Request);
        transaction.setX12Response(x12Response);
        transaction.setRequestTimestamp(LocalDateTime.now());
        transaction.setResponseTimestamp(LocalDateTime.now());
        
        try {
            repository.save(transaction, practiceContextService.getPracticeId());
        } catch (Exception e) {
            log.error("Failed to save eligibility transaction: {}", e.getMessage());
        }
        
        log.info("Eligibility check completed. Status: {}", response.getStatus());
        return response;
    }
    
    public String generateX12Request(EligibilityRequestDto request) {
        return x12Builder.build(
            request,
            clearinghouseClient.getSenderId(),
            clearinghouseClient.getReceiverId()
        );
    }
    
    public EligibilityResponseDto parseX12Response(String x12Response) {
        return x12Parser.parse(x12Response);
    }
}
