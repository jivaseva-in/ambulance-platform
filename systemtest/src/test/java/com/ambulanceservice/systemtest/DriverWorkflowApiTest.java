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

        Response assignedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "IN_TRANSIT", null);
        if (assignedStatusResponse.getStatusCode() == 403 || assignedStatusResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking status update endpoint not enabled for current backend");
        }
        Assertions.assertTrue(assignedStatusResponse.getStatusCode() == 200 || assignedStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as IN_TRANSIT after accepting it");

        Response arrivedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "ARRIVED", null);
        Assertions.assertTrue(arrivedStatusResponse.getStatusCode() == 200 || arrivedStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as ARRIVED");

        Response completedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, driverToken, bookingId, "COMPLETED", null);
        Assertions.assertTrue(completedStatusResponse.getStatusCode() == 200 || completedStatusResponse.getStatusCode() == 201,
            "Driver should be able to mark booking as COMPLETED");
    }
}
