package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxPhoneNumberStatusListResponseDTO {
    private List<TelnyxPhoneNumberStatusDTO> records;
}
