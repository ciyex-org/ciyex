package com.qiaben.ciyex.dto;

import java.util.List;

public record PatientInsurancePaymentRequestDto(
        List<PatientInsuranceRemitLineDto> lines,
        String chequeNumber,
        String bankBranch
) {}
