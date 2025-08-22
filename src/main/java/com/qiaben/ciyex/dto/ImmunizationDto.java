package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ImmunizationDto {
    private Long id;
    private String vaccine;
    private String dose;
    private String dateAdministered;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVaccine() {
        return vaccine;
    }

    public void setVaccine(String vaccine) {
        this.vaccine = vaccine;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getDateAdministered() {
        return dateAdministered;
    }

    public void setDateAdministered(String dateAdministered) {
        this.dateAdministered = dateAdministered;
    }

    public String getAmountAdministered() {
        return amountAdministered;
    }

    public void setAmountAdministered(String amountAdministered) {
        this.amountAdministered = amountAdministered;
    }

    public String getImmunizationExpirationDate() {
        return immunizationExpirationDate;
    }

    public void setImmunizationExpirationDate(String immunizationExpirationDate) {
        this.immunizationExpirationDate = immunizationExpirationDate;
    }

    public String getImmunizationManufacturer() {
        return immunizationManufacturer;
    }

    public void setImmunizationManufacturer(String immunizationManufacturer) {
        this.immunizationManufacturer = immunizationManufacturer;
    }

    public String getImmunizationLotNumber() {
        return immunizationLotNumber;
    }

    public void setImmunizationLotNumber(String immunizationLotNumber) {
        this.immunizationLotNumber = immunizationLotNumber;
    }

    public String getAdministratorName() {
        return administratorName;
    }

    public void setAdministratorName(String administratorName) {
        this.administratorName = administratorName;
    }

    public String getDateInformationGiven() {
        return dateInformationGiven;
    }

    public void setDateInformationGiven(String dateInformationGiven) {
        this.dateInformationGiven = dateInformationGiven;
    }

    public String getDateVISStatement() {
        return dateVISStatement;
    }

    public void setDateVISStatement(String dateVISStatement) {
        this.dateVISStatement = dateVISStatement;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getAdministrationSite() {
        return administrationSite;
    }

    public void setAdministrationSite(String administrationSite) {
        this.administrationSite = administrationSite;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInformationSource() {
        return informationSource;
    }

    public void setInformationSource(String informationSource) {
        this.informationSource = informationSource;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }

    public String getSubstanceRefusalReason() {
        return substanceRefusalReason;
    }

    public void setSubstanceRefusalReason(String substanceRefusalReason) {
        this.substanceRefusalReason = substanceRefusalReason;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getImmunizationOrderingProvider() {
        return immunizationOrderingProvider;
    }

    public void setImmunizationOrderingProvider(String immunizationOrderingProvider) {
        this.immunizationOrderingProvider = immunizationOrderingProvider;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    private String amountAdministered;
    private String immunizationExpirationDate;
    private String immunizationManufacturer;
    private String immunizationLotNumber;
    private String administratorName;
    private String dateInformationGiven;
    private String dateVISStatement;
    private String route;
    private String administrationSite;
    private String notes;
    private String informationSource;
    private String completionStatus;
    private String substanceRefusalReason;
    private String reasonCode;
    private String immunizationOrderingProvider;

    private Long patientId;
    private Long orgId; // Organization ID passed in header
}
