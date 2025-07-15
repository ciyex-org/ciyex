package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class BulkUpdateRequestDTO {
    private String messagingProfileId;
    private List<String> numbers;
}

