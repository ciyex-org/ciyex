package org.ciyex.ehr.exception;

/**
 * Thrown when a user lacks the required permission for an operation.
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String permissionCategory) {
        super("Insufficient permissions for: " + permissionCategory);
    }
}
