package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class UpdateCampaignResponseDTO
{
    private Boolean ageGated;
    private Boolean autoRenewal;
    private String billedDate;
    private String brandId;
    private String brandDisplayName;
    private String campaignId;
    private String tcrBrandId;
    private String tcrCampaignId;
    private String createDate;
    private String cspId;
    private String description;
    private Boolean directLending;
    private Boolean embeddedLink;
    private Boolean embeddedPhone;
    private String helpKeywords;
    private String helpMessage;
    private String messageFlow;
    private Boolean mock;
    private String nextRenewalOrExpirationDate;
    private Boolean numberPool;
    private String optinKeywords;
    private String optinMessage;
    private String optoutKeywords;
    private String optoutMessage;
    private String referenceId;
    private String resellerId;
    private String sample1;
    private String sample2;
    private String sample3;
    private String sample4;
    private String sample5;
    private String status;
    private List<String> subUsecases;
    private Boolean subscriberHelp;
    private Boolean subscriberOptin;
    private Boolean subscriberOptout;
    private Boolean termsAndConditions;
    private String usecase;
    private String vertical;
    private String webhookURL;
    private String webhookFailoverURL;
    private Boolean isTMobileRegistered;
    private Boolean isTMobileSuspended;
    private Boolean isTMobileNumberPoolingEnabled;
    private List<String> failureReasons;
    private String submissionStatus;
    private String campaignStatus;
    private String privacyPolicyLink;
    private String termsAndConditionsLink;
    private String embeddedLinkSample;
}
