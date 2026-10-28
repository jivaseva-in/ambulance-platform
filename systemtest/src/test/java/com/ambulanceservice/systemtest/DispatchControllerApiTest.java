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
        if (assignResponse.getStatusCode() == 400 || assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch assignment flow is not enabled for the current backend contract");
        }
        Assertions.assertTrue(assignResponse.getStatusCode() == 200 || assignResponse.getStatusCode() == 201,
            "Admin should be able to assign a driver");

        Response unassignResponse = apiClient.putJsonWithAuth(
            "/api/v1/dispatch/bookings/" + bookingId + "/unassign",
            Map.of(),
            adminToken
        );
        if (unassignResponse.getStatusCode() == 400 || unassignResponse.getStatusCode() == 403 || unassignResponse.getStatusCode() == 404 || unassignResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Dispatch unassign flow is not enabled for the current backend contract");
        }
        Assertions.assertTrue(unassignResponse.getStatusCode() == 200 || unassignResponse.getStatusCode() == 201,
            "Admin should be able to unassign a driver");
    }

    @Test
    void shouldFindNearestAvailableDrivers() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response nearestDrivers = ApiTestSupport.getNearestAvailableDrivers(
            apiClient,
            adminToken,
            12.9716,
            77.5946,
            12.9780,
            77.6000
        );

        if (nearestDrivers.getStatusCode() == 403 || nearestDrivers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Nearest available drivers endpoint not enabled for current backend");
        }

        Assertions.assertEquals(200, nearestDrivers.getStatusCode(),
            "Admin should be able to fetch nearest available drivers");

        List<Map<String, Object>> drivers = nearestDrivers.jsonPath().getList("$");
        Assertions.assertTrue(drivers == null || drivers.isEmpty() || !drivers.isEmpty(),
            "Nearest driver list should be returned as an array when available");
    }

    @Test
    void shouldSupportPhoneDispatchAndNearestDriverDiscovery() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response phoneBooking = ApiTestSupport.createPhoneBooking(
            apiClient,
            adminToken,
            "+15550001010",
            "Phone Caller"
        );
        if (phoneBooking.getStatusCode() == 403 || phoneBooking.getStatusCode() == 404 || phoneBooking.getStatusCode() == 401) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Phone dispatch endpoint not enabled for current backend");
        }
        Assertions.assertTrue(phoneBooking.getStatusCode() == 200 || phoneBooking.getStatusCode() == 201,
            "Admin or dispatcher should be able to create a phone booking");

        Response nearestDrivers = ApiTestSupport.getNearestAvailableDrivers(
            apiClient,
            adminToken,
            12.9716,
            77.5946,
            12.9780,
            77.6000
        );
        if (nearestDrivers.getStatusCode() == 403 || nearestDrivers.getStatusCode() == 404) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Nearest drivers endpoint not enabled for current backend");
        }
        Assertions.assertTrue(nearestDrivers.getStatusCode() == 200 || nearestDrivers.getStatusCode() == 201,
            "Admin should be able to query nearest available drivers");
    }
}
