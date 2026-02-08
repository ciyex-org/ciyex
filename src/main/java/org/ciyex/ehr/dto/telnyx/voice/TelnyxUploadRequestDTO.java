// src/main/java/com/ciyex/ciyex/dto/telnyx/UploadRequestDTO.java
package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUploadRequestDTO {
    private List<String> number_ids;
    private String usage; // calling_user_assignment OR first_party_app_assignment
    private List<String> additional_usages;
    private String location_id;       // optional, UUID
    private String civic_address_id;  // optional, UUID
}
