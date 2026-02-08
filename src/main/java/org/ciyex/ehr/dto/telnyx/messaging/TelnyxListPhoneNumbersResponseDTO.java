
// src/main/java/com/ciyex/ciyex/dto/telnyx/ListPhoneNumbersResponseDTO.java
package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxListPhoneNumbersResponseDTO {
    private List<TelnyxMessagingPhoneNumberDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private Integer totalPages;
        private Integer totalResults;
        private Integer pageNumber;
        private Integer pageSize;
    }
}

