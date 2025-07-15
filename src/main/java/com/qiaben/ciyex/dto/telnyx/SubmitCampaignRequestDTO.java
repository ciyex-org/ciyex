package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class SubmitCampaignRequestDTO {
    private Boolean ageGated;
    private Boolean autoRenewal;
    private String brandId;
    private String description;
    private Boolean directLending;
    private Boolean embeddedLink;
    private Boolean embeddedPhone;
    private String helpKeywords;
    private String helpMessage;
    private String messageFlow;
    private List<Integer> mnoIds;
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
    private List<String> subUsecases;
    private Boolean subscriberHelp;
    private Boolean subscriberOptin;
    private Boolean subscriberOptout;
    private List<String> tag;
    private Boolean termsAndConditions;
    private String privacyPolicyLink;
    private String termsAndConditionsLink;
    private String embeddedLinkSample;
    private String usecase;
    private String webhookURL;
    private String webhookFailoverURL;
}
