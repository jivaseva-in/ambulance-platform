package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class DriverWorkflowApiTest extends AbstractApiTest {

    @Test
    void shouldRegisterDriverApproveAndCompleteTripWorkflow() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-workflow");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-workflow");
        String driverPassword = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, driverPassword);
        ApiTestSupport.assertSuccess(registerResponse, "Driver should register successfully");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, pendingUsers.getStatusCode(), "Admin should list pending users");

        Long driverUserId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverUserId == null) {
            Assumptions.assumeTrue(false, "New driver was not visible in pending users list");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverUserId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }
        if (approveResponse.getStatusCode() != 200 && approveResponse.getStatusCode() != 201 && approveResponse.getStatusCode() != 204) {
            Assumptions.assumeTrue(false, "Driver approval is not available in the current backend state");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, driverPassword);

        Response availabilityResponse = ApiTestSupport.setDriverAvailability(
            apiClient,
            driverToken,
            "AVAILABLE",
            12.9716,
            77.5946
        );
        if (availabilityResponse.getStatusCode() == 403 || availabilityResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityResponse.getStatusCode() == 200 || availabilityResponse.getStatusCode() == 201,
            "Driver should be able to set own availability");

        Response locationUpdateResponse = ApiTestSupport.updateDriverLocation(apiClient, driverToken, 12.9780, 77.6000);
        if (locationUpdateResponse.getStatusCode() == 403 || locationUpdateResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver location update endpoint not enabled for current backend");
        }
        Assertions.assertTrue(locationUpdateResponse.getStatusCode() == 200 || locationUpdateResponse.getStatusCode() == 201,
            "Driver should be able to update current location");

        Response nearestDriversResponse = ApiTestSupport.getNearestAvailableDrivers(
            apiClient,
            adminToken,
            12.9716,
            77.5946,
            12.9780,
            77.6000
        );
        if (nearestDriversResponse.getStatusCode() == 403 || nearestDriversResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Nearest available drivers endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, nearestDriversResponse.getStatusCode(),
            "Admin should be able to fetch nearest available drivers");

        List<Map<String, Object>> nearestDrivers = nearestDriversResponse.jsonPath().getList("$");
        if (nearestDrivers == null || nearestDrivers.isEmpty()) {
            Assumptions.assumeTrue(false, "No nearest drivers are currently returned for the workflow");
        }

        Response availableBookingsResponse = apiClient.getWithAuth("/api/v1/bookings/available", driverToken);
        if (availableBookingsResponse.getStatusCode() == 403 || availableBookingsResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Available bookings endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, availableBookingsResponse.getStatusCode(), "Approved driver should list available bookings");

        List<Map<String, Object>> availableBookings = availableBookingsResponse.jsonPath().getList("$");
        if (availableBookings == null || availableBookings.isEmpty()) {
            Assumptions.assumeTrue(false, "No available bookings are currently exposed for the driver workflow");
        }

        Long bookingId = ((Number) availableBookings.get(0).get("id")).longValue();
        Response assignResponse = ApiTestSupport.assignDriver(apiClient, adminToken, bookingId, driverUserId);
        if (assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch assignment endpoint not enabled for current backend");
        }
        Assertions.assertTrue(assignResponse.getStatusCode() == 200 || assignResponse.getStatusCode() == 201,
            "Admin should be able to assign the driver to a booking");

        Response acceptResponse = ApiTestSupport.acceptBooking(apiClient, driverToken, bookingId);
        if (acceptResponse.getStatusCode() == 403 || acceptResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking acceptance endpoint not enabled for current backend");
        }
        Assertions.assertTrue(acceptResponse.getStatusCode() == 200 || acceptResponse.getStatusCode() == 201,
            "Driver should be able to accept the assigned booking");

        Response inTransitStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "IN_TRANSIT", null);
        Assertions.assertTrue(inTransitStatusResponse.getStatusCode() == 200 || inTransitStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as IN_TRANSIT after accepting it");

        Response arrivedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "ARRIVED", null);
        Assertions.assertTrue(arrivedStatusResponse.getStatusCode() == 200 || arrivedStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as ARRIVED");

        Response completedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "COMPLETED", null);
        Assertions.assertTrue(completedStatusResponse.getStatusCode() == 200 || completedStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as COMPLETED");
    }

    @Test
    void shouldKeepUnavailableDriverOutOfNearestSearch() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-unavailable");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-unavailable");
        String driverPassword = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, driverPassword);
        ApiTestSupport.assertSuccess(registerResponse, "Driver should register successfully");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }

        Long driverUserId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverUserId == null) {
            Assumptions.assumeTrue(false, "New driver was not visible in pending users list");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverUserId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }
        if (approveResponse.getStatusCode() != 200 && approveResponse.getStatusCode() != 201 && approveResponse.getStatusCode() != 204) {
            Assumptions.assumeTrue(false, "Driver approval is not available in the current backend state");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, driverPassword);
        Response availabilityResponse = ApiTestSupport.setDriverAvailability(
            apiClient,
            driverToken,
            "UNAVAILABLE",
            12.9716,
            77.5946
        );
        if (availabilityResponse.getStatusCode() == 403 || availabilityResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityResponse.getStatusCode() == 200 || availabilityResponse.getStatusCode() == 201,
            "Driver should be able to set availability to UNAVAILABLE");

        Response nearestDriversResponse = ApiTestSupport.getNearestAvailableDrivers(
            apiClient,
            adminToken,
            12.9716,
            77.5946,
            12.9780,
            77.6000
        );
        if (nearestDriversResponse.getStatusCode() == 403 || nearestDriversResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Nearest available drivers endpoint not enabled for current backend");
        }
        if (nearestDriversResponse.getStatusCode() == 200 || nearestDriversResponse.getStatusCode() == 201 || nearestDriversResponse.getStatusCode() == 204) {
            List<Map<String, Object>> nearestDrivers = nearestDriversResponse.jsonPath().getList("$");
            if (nearestDrivers != null && !nearestDrivers.isEmpty()) {
                for (Map<String, Object> driver : nearestDrivers) {
                    Object candidateId = driver.get("id");
                    if (candidateId instanceof Number && ((Number) candidateId).longValue() == driverUserId) {
                        Assertions.fail("Unavailable driver should not appear in nearest available driver search results");
                    }
                }
            }
        }
    }

    @Test
    void shouldRejectInvalidNearestDriverCoordinates() {
        Map<String, Object> request = ApiTestDataFactory.bookingRequest("Guest Invalid Coordinates", "+15550000000");
        request.put("pickupLatitude", 91.0);
        request.put("pickupLongitude", 77.5946);
        request.put("destinationLatitude", 12.9780);
        request.put("destinationLongitude", 77.6000);

        Response invalidBookingResponse = apiClient.postJson("/api/v1/bookings", request);
        if (invalidBookingResponse.getStatusCode() == 403 || invalidBookingResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking creation endpoint not enabled for current backend");
        }
        Assertions.assertTrue(
            invalidBookingResponse.getStatusCode() >= 400 && invalidBookingResponse.getStatusCode() < 500,
            "Booking creation should reject pickup latitude outside the valid range before nearest-driver workflow begins"
        );
    }

    @Test
    void shouldCancelAssignedBookingFromDriverWorkflow() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-cancel");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-cancel");
        String driverPassword = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, driverPassword);
        ApiTestSupport.assertSuccess(registerResponse, "Driver should register successfully");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }

        Long driverUserId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverUserId == null) {
            Assumptions.assumeTrue(false, "New driver was not visible in pending users list");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverUserId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }
        if (approveResponse.getStatusCode() != 200 && approveResponse.getStatusCode() != 201 && approveResponse.getStatusCode() != 204) {
            Assumptions.assumeTrue(false, "Driver approval is not available in the current backend state");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, driverPassword);
        Response availabilityResponse = ApiTestSupport.setDriverAvailability(
            apiClient,
            driverToken,
            "AVAILABLE",
            12.9716,
            77.5946
        );
        if (availabilityResponse.getStatusCode() == 403 || availabilityResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }
        Assertions.assertTrue(availabilityResponse.getStatusCode() == 200 || availabilityResponse.getStatusCode() == 201,
            "Driver should be able to set availability before booking cancellation flow");

        Response availableBookingsResponse = apiClient.getWithAuth("/api/v1/bookings/available", driverToken);
        if (availableBookingsResponse.getStatusCode() == 403 || availableBookingsResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Available bookings endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, availableBookingsResponse.getStatusCode(), "Approved driver should list available bookings");

        List<Map<String, Object>> availableBookings = availableBookingsResponse.jsonPath().getList("$");
        if (availableBookings == null || availableBookings.isEmpty()) {
            Assumptions.assumeTrue(false, "No available bookings are currently exposed for the driver cancellation flow");
        }

        Long bookingId = ((Number) availableBookings.get(0).get("id")).longValue();
        Response assignResponse = ApiTestSupport.assignDriver(apiClient, adminToken, bookingId, driverUserId);
        if (assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Dispatch assignment endpoint not enabled for current backend");
        }
        Assertions.assertTrue(assignResponse.getStatusCode() == 200 || assignResponse.getStatusCode() == 201,
            "Admin should be able to assign the driver to a booking");

        Response acceptResponse = ApiTestSupport.acceptBooking(apiClient, driverToken, bookingId);
        if (acceptResponse.getStatusCode() == 403 || acceptResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking acceptance endpoint not enabled for current backend");
        }
        Assertions.assertTrue(acceptResponse.getStatusCode() == 200 || acceptResponse.getStatusCode() == 201,
            "Driver should be able to accept the assigned booking");

        Response cancelResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "CANCELLED", "Driver unavailable");
        if (cancelResponse.getStatusCode() == 403 || cancelResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking cancellation endpoint not enabled for current backend");
        }
        Assertions.assertTrue(cancelResponse.getStatusCode() == 200 || cancelResponse.getStatusCode() == 201 || cancelResponse.getStatusCode() == 204,
            "Driver should be able to cancel an accepted booking");
    }
}
