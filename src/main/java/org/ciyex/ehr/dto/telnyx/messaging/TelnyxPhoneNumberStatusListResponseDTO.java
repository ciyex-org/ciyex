package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TelnyxPhoneNumberStatusListResponseDTO {
    private List<TelnyxPhoneNumberStatusDTO> records;
}
