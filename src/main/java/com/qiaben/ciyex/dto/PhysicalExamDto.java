//package com.qiaben.ciyex.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class PhysicalExamDto {
//    private Long id;
//    private String externalId;   // optional FHIR id
//
//    private Long patientId;
//    private Long encounterId;
//
//    private List<SectionDto> sections;
//
//    private Audit audit;
//
//    @Data
//    public static class SectionDto {
//        // GENERAL, HEENT, NECK, BREASTS, CARDIOVASCULAR, THORAX_BACK, GASTROINTESTINAL,
//        // GU_FEMALE, GU_MALE, MUSCULOSKELETAL, SKIN, LYMPHATIC, NEUROLOGIC, PSYCHIATRIC, OTHER
//        private String sectionKey;
//        private Boolean allNormal;      // true if “All Normal” selected for the section
//        private String normalText;      // the standard normal sentence shown in UI
//        private String findings;        // free-text findings (optional)
//    }
//
//    @Data
//    public static class Audit {
//        private String createdDate;      // yyyy-MM-dd
//        private String lastModifiedDate; // yyyy-MM-dd
//    }
//}

package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PhysicalExamDto {
    private Long id;
    private String externalId;
    private Long patientId;
    private Long encounterId;

    // Request/Response: JSON array of sections
    private List<PhysicalExamSectionDto> sections;

    // Optional summary text (if your PEForm sets one)
    private String summary;

    // eSign / Print (server managed)
    private Boolean eSigned;
    private OffsetDateTime signedAt;
    private String signedBy;
    private OffsetDateTime printedAt;

    private Audit audit;
    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
