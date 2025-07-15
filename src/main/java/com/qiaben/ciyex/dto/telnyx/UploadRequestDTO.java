// src/main/java/com/qiaben/ciyex/dto/telnyx/UploadRequestDTO.java
package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class UploadRequestDTO {
    private List<String> number_ids;
    private String usage; // calling_user_assignment OR first_party_app_assignment
    private List<String> additional_usages;
    private String location_id;       // optional, UUID
    private String civic_address_id;  // optional, UUID
}
