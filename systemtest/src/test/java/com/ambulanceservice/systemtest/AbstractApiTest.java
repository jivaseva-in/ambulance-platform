package com.ambulanceservice.systemtest;

import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractApiTest {

    protected static SystemTestProperties properties;
    protected static ApiClient apiClient;

    @BeforeAll
    static void setUpApiTestContext() {
        properties = SystemTestProperties.load();
        apiClient = new ApiClient(properties.getBaseUrl());
    }
}

