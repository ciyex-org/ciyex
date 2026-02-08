

package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class PhysicalExamSectionDto {
    private String sectionKey;
    private Boolean allNormal;
    private String findings;
    private String normalText;
}
