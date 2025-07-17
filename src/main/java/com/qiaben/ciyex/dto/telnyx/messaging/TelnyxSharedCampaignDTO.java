package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxSharedCampaignDTO {
    private Boolean ageGated;
    private Integer assignedPhoneNumbersCount;
    private String brandDisplayName;
    private String campaignStatus;
    private String description;
    private Boolean directLending;
    private Boolean embeddedLink;
    private String embeddedLinkSample;
    private Boolean embeddedPhone;
    private Object failureReasons;
    private String helpKeywords;
    private String helpMessage;
    private Boolean isNumberPoolingEnabled;
    private String messageFlow;
    private Boolean numberPool;
    private String optinKeywords;
    private String optinMessage;
    private String optoutKeywords;
    private String optoutMessage;
    private String privacyPolicyLink;
    private String usecase;
    private String sample1;
    private String sample2;
    private String sample3;
    private String sample4;
    private String sample5;
    private List<String> subUsecases;
    private Boolean subscriberOptin;
    private Boolean subscriberOptout;
    private String tcrBrandId;
    private String tcrCampaignId;
    private Boolean termsAndConditions;
    private String termsAndConditionsLink;
    private String webhookURL;
    private String webhookFailoverURL;
    private String createdAt;
    private String updatedAt;
}
