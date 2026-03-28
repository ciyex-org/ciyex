package org.ciyex.ehr.util;

/**
 * Small helper to safely quote SQL identifiers for Postgres.
 * It does minimal validation and wraps identifiers in double-quotes.
 * Use only for identifiers (schema, table, column), not for values.
 */
public final class SqlIdentifier {

    private SqlIdentifier() {}

    /**
     * Quote an identifier using double quotes and escape any existing double quotes.
     * If the input is null/empty, returns it unchanged.
     */
    public static String quote(String identifier) {
        if (identifier == null || identifier.isEmpty()) return identifier;
        // Escape double quotes by doubling them
        String escaped = identifier.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
