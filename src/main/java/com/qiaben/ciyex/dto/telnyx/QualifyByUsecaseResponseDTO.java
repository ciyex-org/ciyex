package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.Map;

@Data
public class QualifyByUsecaseResponseDTO {
    private Double annualFee;
    private Integer maxSubUsecases;
    private Integer minSubUsecases;
    private Map<String, Object> mnoMetadata; // MNO network ID as key
    private Double monthlyFee;
    private Double quarterlyFee;
    private String usecase;
}
