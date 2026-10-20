package com.ambulanceservice.systemtest;

import java.util.UUID;

public final class TestDataCleanup {

    private TestDataCleanup() {
    }

    public static String prefixedEmail(String label) {
        return "DELETABLE_" + sanitize(label) + "_" + shortUuid() + "@example.test";
    }

    public static String prefixedFullName(String label) {
        String base = sanitizeForDisplay(label);
        return "DELETABLE_" + base + "_" + shortUuid();
    }

    public static String prefixedPhone(String suffix) {
        String digits = (suffix == null ? "" : suffix).replaceAll("[^0-9]", "");
        String baseDigits = digits;
        if (baseDigits.isBlank() || baseDigits.length() < 6) {
            baseDigits = String.format("%09d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000L);
        }
        if (baseDigits.length() > 9) {
            baseDigits = baseDigits.substring(0, 9);
        }
        return "+1000" + baseDigits;
    }

    public static String sanitizedName(String value) {
        return sanitize(value).replace(".", "-");
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "test";
        }
        String normalized = value.trim().toLowerCase().replaceAll("[^a-z0-9]", "-");
        return normalized.replaceAll("-+", "-");
    }

    private static String sanitizeForDisplay(String value) {
        if (value == null || value.isBlank()) {
            return "test";
        }
        String normalized = value.trim().toLowerCase().replaceAll("[^a-z0-9]", "-");
        return normalized.replaceAll("-+", "-");
    }

    private static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
