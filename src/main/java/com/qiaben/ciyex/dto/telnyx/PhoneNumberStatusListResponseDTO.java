package com.qiaben.ciyex.dto.telnyx;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhoneNumberStatusListResponseDTO {
    private List<PhoneNumberStatusDTO> records;
}
