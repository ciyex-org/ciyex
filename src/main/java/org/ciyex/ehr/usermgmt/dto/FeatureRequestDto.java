package org.ciyex.ehr.usermgmt.dto;

import lombok.Data;

@Data
public class FeatureRequestDto {
    private String category;     // bug_report, feature_request, improvement
    private String subject;
    private String description;
    private String userEmail;
    private String userName;
}
