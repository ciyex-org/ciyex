package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class FhirPractitionerSearchParamsDto {
    private String _id;
    private String _lastUpdated;
    private String name;
    private String active;
    private String address;
    private String addressCity;
    private String addressPostalcode;
    private String addressState;
    private String email;

    private String family;
    private String given;
    private String phone;
    private String telecom;

    public void set_id(String _id) {
        this._id = _id;
    }

    public void set_lastUpdated(String _lastUpdated) {
        this._lastUpdated = _lastUpdated;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public void setAddressPostalcode(String addressPostalcode) {
        this.addressPostalcode = addressPostalcode;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setGiven(String given) {
        this.given = given;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setTelecom(String telecom) {
        this.telecom = telecom;
    }

    public String get_id() {
        return _id;
    }

    public String get_lastUpdated() {
        return _lastUpdated;
    }

    public String getName() {
        return name;
    }

    public String getActive() {
        return active;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public String getAddressPostalcode() {
        return addressPostalcode;
    }

    public String getAddressState() {
        return addressState;
    }

    public String getEmail() {
        return email;
    }

    public String getFamily() {
        return family;
    }

    public String getGiven() {
        return given;
    }

    public String getPhone() {
        return phone;
    }

    public String getTelecom() {
        return telecom;
    }

}