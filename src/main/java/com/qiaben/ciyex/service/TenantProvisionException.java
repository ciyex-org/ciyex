package com.qiaben.ciyex.service;

public class TenantProvisionException extends RuntimeException {
    public TenantProvisionException(String message) {
        super(message);
    }

    public TenantProvisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
