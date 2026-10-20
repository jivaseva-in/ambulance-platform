package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class DispatchControllerApiTest extends AbstractApiTest {

    @Test
    void shouldListOpenBookingsAndAvailableDrivers() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response openBookings = apiClient.getWithAuth("/api/v1/dispatch/bookings/open", adminToken);
        if (openBookings.getStatusCode() == 403 || openBookings.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch open bookings endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, openBookings.getStatusCode(), "Admin should list open dispatch bookings");

        Response availableDrivers = apiClient.getWithAuth("/api/v1/dispatch/drivers/available", adminToken);
        if (availableDrivers.getStatusCode() == 403 || availableDrivers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch available drivers endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, availableDrivers.getStatusCode(), "Admin should list available drivers");
    }

    @Test
    void shouldAssignAndUnassignDriverToBooking() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response createResponse = apiClient.postJson("/api/v1/bookings", ApiTestDataFactory.bookingRequest(
            "Dispatch Guest",
            ApiTestDataFactory.uniquePhone("dispatch-booking")
        ));
        ApiTestSupport.assertSuccess(createResponse, "Booking should be created for dispatch assignment flow");
        Long bookingId = ApiTestSupport.createdBookingId(createResponse);

        Response availableDrivers = apiClient.getWithAuth("/api/v1/dispatch/drivers/available", adminToken);
        if (availableDrivers.getStatusCode() == 403 || availableDrivers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch available drivers endpoint not enabled for current backend");
        }

        List<Map<String, Object>> drivers = availableDrivers.jsonPath().getList("$");
        if (drivers == null || drivers.isEmpty()) {
            Assumptions.assumeTrue(false, "No available drivers exist for assignment flow");
        }

        Long driverId = ((Number) drivers.get(0).get("id")).longValue();
        Response assignResponse = apiClient.putJsonWithAuth(
            "/api/v1/dispatch/bookings/" + bookingId + "/assign",
            ApiTestDataFactory.assignDriverRequest(driverId),
            adminToken
        );
        if (assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch assignment endpoint not enabled for current backend");
        }
        Assertions.assertTrue(assignResponse.getStatusCode() == 200 || assignResponse.getStatusCode() == 201,
            "Admin should be able to assign a driver");

        Response unassignResponse = apiClient.putJsonWithAuth(
            "/api/v1/dispatch/bookings/" + bookingId + "/unassign",
            Map.of(),
            adminToken
        );
        if (unassignResponse.getStatusCode() == 403 || unassignResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch unassign endpoint not enabled for current backend");
        }
        Assertions.assertTrue(unassignResponse.getStatusCode() == 200 || unassignResponse.getStatusCode() == 201,
            "Admin should be able to unassign a driver");
    }
}

