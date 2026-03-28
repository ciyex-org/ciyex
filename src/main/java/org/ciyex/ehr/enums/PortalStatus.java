package org.ciyex.ehr.enums;

/**
 * Status enum for portal user registration and approval workflow
 */
public enum PortalStatus {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    private final String value;

    PortalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static PortalStatus fromString(String value) {
        for (PortalStatus status : PortalStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PortalStatus: " + value);
    }
}