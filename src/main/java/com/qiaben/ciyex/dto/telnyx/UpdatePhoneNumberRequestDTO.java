// src/main/java/com/qiaben/ciyex/dto/telnyx/UpdatePhoneNumberRequestDTO.java
package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdatePhoneNumberRequestDTO {
    private String messagingProfileId;  // "" to un‑assign
    private String messagingProduct;    // quoted product id
}
