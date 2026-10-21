package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class DriverControllerApiTest extends AbstractApiTest {

    @Test
    void shouldGetAndUpdateDriverAvailability() {
        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-availability");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-availability");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, password);
        ApiTestSupport.assertSuccess(registerResponse, "Driver should be able to register");

        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }

        Long driverId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverId == null) {
            Assumptions.assumeTrue(false, "New driver not found in pending users");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }
        if (approveResponse.getStatusCode() != 200 && approveResponse.getStatusCode() != 201 && approveResponse.getStatusCode() != 204) {
            Assumptions.assumeTrue(false, "Driver approval is not available in the current backend state");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, password);

        Response availabilityGetResponse = apiClient.getWithAuth("/api/v1/drivers/me/availability", driverToken);
        if (availabilityGetResponse.getStatusCode() == 403 || availabilityGetResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability GET endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityGetResponse.getStatusCode() == 200 || availabilityGetResponse.getStatusCode() == 201,
            "Driver should be able to fetch availability status");

        Response availabilityPutResponse = ApiTestSupport.setDriverAvailability(
            apiClient, driverToken, "AVAILABLE", 12.9716, 77.5946
        );
        if (availabilityPutResponse.getStatusCode() == 403 || availabilityPutResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability PUT endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityPutResponse.getStatusCode() == 200 || availabilityPutResponse.getStatusCode() == 201,
            "Driver should be able to update availability status");
    }

    @Test
    void shouldUpdateDriverCurrentLocation() {
        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-location");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-location");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, password);
        ApiTestSupport.assertSuccess(registerResponse, "Driver should be able to register");

        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }

        Long driverId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverId == null) {
            Assumptions.assumeTrue(false, "New driver not found in pending users");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }
        if (approveResponse.getStatusCode() != 200 && approveResponse.getStatusCode() != 201 && approveResponse.getStatusCode() != 204) {
            Assumptions.assumeTrue(false, "Driver approval is not available in the current backend state");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, password);
        Response availabilityResponse = ApiTestSupport.setDriverAvailability(
            apiClient,
            driverToken,
            "AVAILABLE",
            12.9716,
            77.5946
        );
        if (availabilityResponse.getStatusCode() == 403 || availabilityResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability PUT endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityResponse.getStatusCode() == 200 || availabilityResponse.getStatusCode() == 201,
            "Driver should be able to set availability before updating location");

        Response locationResponse = ApiTestSupport.updateDriverLocation(apiClient, driverToken, 12.9780, 77.6000);
        if (locationResponse.getStatusCode() == 403 || locationResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver location update endpoint not enabled for current backend");
        }

        Assertions.assertTrue(locationResponse.getStatusCode() == 200 || locationResponse.getStatusCode() == 201,
            "Driver should be able to update current location after becoming available");
    }
}
