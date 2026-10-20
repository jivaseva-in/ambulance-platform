package com.ambulanceservice.systemtest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class SystemTestProperties {

    private static final String DEFAULT_RESOURCE = "systemtest.properties";

    private final Properties properties = new Properties();

    private SystemTestProperties() {
        loadDefaults();
    }

    public static SystemTestProperties load() {
        return new SystemTestProperties();
    }

    private void loadDefaults() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load systemtest.properties", e);
        }
    }

    public String getBaseUrl() {
        return getValue("systemtest.base-url", "http://localhost:8080");
    }

    public String getAdminEmail() {
        return getValue("systemtest.admin-email", "superadmin@example.com");
    }

    public String getAdminPassword() {
        return getValue("systemtest.admin-password", "change-me");
    }

    public String getDriverEmail() {
        return getValue("systemtest.driver-email", "dave.driver@example.com");
    }

    public String getDriverPassword() {
        return getValue("systemtest.driver-password", "change-me");
    }

    public boolean isCleanupEnabled() {
        return Boolean.parseBoolean(getValue("systemtest.cleanup-enabled", "false"));
    }

    public String getTestUserPrefix() {
        return getValue("systemtest.test-user-prefix", "DELETABLE_");
    }

    public int getTimeoutMs() {
        return Integer.parseInt(getValue("systemtest.timeout-ms", "15000"));
    }

    private String getValue(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return defaultValue;
    }
}
