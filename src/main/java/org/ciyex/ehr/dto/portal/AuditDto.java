package org.ciyex.ehr.dto.portal;

import lombok.Data;
import java.time.Instant;

@Data
public class AuditDto {
    private Instant createdDate;
    private Instant lastModifiedDate;
}
