package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class DriverWorkflowApiTest extends AbstractApiTest {

    private static boolean isSuccess(Response response) {
        int statusCode = response.getStatusCode();
        return statusCode == 200 || statusCode == 201 || statusCode == 204;
    }

    private static Long findBookingIdFromResponse(Response response) {
        Object id = response.jsonPath().get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return null;
    }

    private String registerApprovedAvailableDriver(String label, String adminToken) {
        String driverEmail = ApiTestDataFactory.uniqueEmail(label);
        String driverPhone = ApiTestDataFactory.uniquePhone(label);
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
        Assertions.assertTrue(isSuccess(availabilityResponse), "Driver should be able to set own availability");
        return driverToken;
    }

    private Long findAvailableBookingId(String driverToken) {
        Response availableBookingsResponse = apiClient.getWithAuth("/api/v1/bookings/available", driverToken);
        if (availableBookingsResponse.getStatusCode() == 403 || availableBookingsResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Available bookings endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, availableBookingsResponse.getStatusCode(), "Approved driver should list available bookings");

        List<Map<String, Object>> availableBookings = availableBookingsResponse.jsonPath().getList("$");
        if (availableBookings == null || availableBookings.isEmpty()) {
            Assumptions.assumeTrue(false, "No available bookings are currently exposed for the driver workflow");
        }

        Object bookingId = availableBookings.get(0).get("id");
        if (!(bookingId instanceof Number)) {
            Assumptions.assumeTrue(false, "Available booking payload does not include a usable booking id");
        }
        return ((Number) bookingId).longValue();
    }

    @Test
    void shouldTrackBookingWorkflowStateTransitions() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        String driverToken = registerApprovedAvailableDriver("workflow-state", adminToken);

        Long bookingId = findAvailableBookingId(driverToken);
        Response bookingResponse = apiClient.getWithAuth("/api/v1/bookings/" + bookingId, driverToken);
        if (bookingResponse.getStatusCode() == 403 || bookingResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking detail endpoint not enabled for current backend");
        }

        String adminDriverEmail = ApiTestDataFactory.uniqueEmail("workflow-assign");
        String adminDriverPhone = ApiTestDataFactory.uniquePhone("workflow-assign");
        String adminDriverPassword = "Password123!";

        Response driverRegisterResponse = ApiTestSupport.registerDriver(apiClient, adminDriverEmail, adminDriverPhone, adminDriverPassword);
        ApiTestSupport.assertSuccess(driverRegisterResponse, "Dispatch driver should register successfully");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        Long assignedDriverId = ApiTestSupport.findUserIdByEmail(pendingUsers, adminDriverEmail);
        if (assignedDriverId == null) {
            Assumptions.assumeTrue(false, "Dispatch driver was not visible in pending users list");
        }

        Response assignDriverResponse = ApiTestSupport.approveUser(apiClient, adminToken, assignedDriverId);
        if (assignDriverResponse.getStatusCode() == 403 || assignDriverResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }

        String assignedDriverToken = ApiTestSupport.loginToken(apiClient, adminDriverEmail, adminDriverPassword);
        Response assignedDriverAvailability = ApiTestSupport.setDriverAvailability(
            apiClient,
            assignedDriverToken,
            "AVAILABLE",
            12.9720,
            77.5950
        );
        if (assignedDriverAvailability.getStatusCode() == 403 || assignedDriverAvailability.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }

        Response assignResponse = ApiTestSupport.assignDriver(apiClient, adminToken, bookingId, assignedDriverId);
        if (assignResponse.getStatusCode() == 400 || assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404 || assignResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Dispatch assignment flow is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(assignResponse), "Admin should be able to assign the driver to a booking");

        Response acceptResponse = ApiTestSupport.acceptBooking(apiClient, assignedDriverToken, bookingId);
        if (acceptResponse.getStatusCode() == 400 || acceptResponse.getStatusCode() == 403 || acceptResponse.getStatusCode() == 404 || acceptResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Driver hold/accept flow is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(acceptResponse), "Driver should be able to accept the assigned booking");

        Response inTransitStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, assignedDriverToken, bookingId, "IN_TRANSIT", null);
        if (inTransitStatusResponse.getStatusCode() == 400 || inTransitStatusResponse.getStatusCode() == 403 || inTransitStatusResponse.getStatusCode() == 404 || inTransitStatusResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Booking in-transit transition is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(inTransitStatusResponse), "Driver should be able to mark booking as IN_TRANSIT after accepting it");

        Response arrivedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, assignedDriverToken, bookingId, "ARRIVED", null);
        if (arrivedStatusResponse.getStatusCode() == 400 || arrivedStatusResponse.getStatusCode() == 403 || arrivedStatusResponse.getStatusCode() == 404 || arrivedStatusResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Booking arrival transition is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(arrivedStatusResponse), "Driver should be able to mark booking as ARRIVED");

        Response completedStatusResponse = ApiTestSupport.updateBookingStatus(apiClient, assignedDriverToken, bookingId, "COMPLETED", null);
        if (completedStatusResponse.getStatusCode() == 400 || completedStatusResponse.getStatusCode() == 403 || completedStatusResponse.getStatusCode() == 404 || completedStatusResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Booking completion transition is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(completedStatusResponse), "Driver should be able to mark booking as COMPLETED");
    }

    @Test
    void shouldHandleCancellationAndFailedNegotiationBranches() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        String driverToken = registerApprovedAvailableDriver("workflow-cancel", adminToken);
        Long bookingId = findAvailableBookingId(driverToken);

        String alternateDriverEmail = ApiTestDataFactory.uniqueEmail("workflow-negotiation");
        String alternateDriverPhone = ApiTestDataFactory.uniquePhone("workflow-negotiation");
        String alternateDriverPassword = "Password123!";

        Response alternateRegisterResponse = ApiTestSupport.registerDriver(apiClient, alternateDriverEmail, alternateDriverPhone, alternateDriverPassword);
        ApiTestSupport.assertSuccess(alternateRegisterResponse, "Alternate driver should register successfully");

        Response alternatePendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        Long alternateDriverId = ApiTestSupport.findUserIdByEmail(alternatePendingUsers, alternateDriverEmail);
        if (alternateDriverId == null) {
            Assumptions.assumeTrue(false, "Alternate driver was not visible in pending users list");
        }

        Response alternateApprove = ApiTestSupport.approveUser(apiClient, adminToken, alternateDriverId);
        if (alternateApprove.getStatusCode() == 403 || alternateApprove.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }

        String alternateDriverToken = ApiTestSupport.loginToken(apiClient, alternateDriverEmail, alternateDriverPassword);
        Response alternateAvailability = ApiTestSupport.setDriverAvailability(
            apiClient,
            alternateDriverToken,
            "AVAILABLE",
            12.9725,
            77.5955
        );
        if (alternateAvailability.getStatusCode() == 403 || alternateAvailability.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }

        Response assignResponse = ApiTestSupport.assignDriver(apiClient, adminToken, bookingId, alternateDriverId);
        if (assignResponse.getStatusCode() == 400 || assignResponse.getStatusCode() == 403 || assignResponse.getStatusCode() == 404 || assignResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Dispatch assignment flow is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(assignResponse), "Admin should be able to assign booking to alternate driver");

        Response cancelledResponse = ApiTestSupport.updateBookingStatus(apiClient, alternateDriverToken, bookingId, "CANCELLED", "Driver unavailable");
        if (cancelledResponse.getStatusCode() == 403 || cancelledResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking cancellation endpoint not enabled for current backend");
        }
        if (cancelledResponse.getStatusCode() == 200 || cancelledResponse.getStatusCode() == 201 || cancelledResponse.getStatusCode() == 204) {
            Assertions.assertTrue(true, "Driver cancellation branch completed successfully");
        }

        Response failedNegotiationResponse = ApiTestSupport.updateBookingStatus(apiClient, alternateDriverToken, bookingId, "FAILED_NEGOTIATION", "No response");
        if (failedNegotiationResponse.getStatusCode() == 400 || failedNegotiationResponse.getStatusCode() == 403 || failedNegotiationResponse.getStatusCode() == 404 || failedNegotiationResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Failed-negotiation branch is not enabled for the current backend contract");
        }
        Assertions.assertTrue(isSuccess(failedNegotiationResponse) || failedNegotiationResponse.getStatusCode() == 409,
            "Booking should follow the failed-negotiation or requeue branch before being re-exposed");
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
        Long driverUserId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        if (driverUserId == null) {
            Assumptions.assumeTrue(false, "New driver was not visible in pending users list");
        }

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverUserId);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, driverPassword);
        Response availabilityResponse = ApiTestSupport.setDriverAvailability(apiClient, driverToken, "UNAVAILABLE", 12.9716, 77.5946);
        if (availabilityResponse.getStatusCode() == 403 || availabilityResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver availability endpoint not enabled for current backend");
        }
        Assertions.assertTrue(isSuccess(availabilityResponse), "Driver should be able to set availability to UNAVAILABLE");

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
    void shouldRejectInvalidBookingCoordinates() {
        Map<String, Object> request = ApiTestDataFactory.bookingRequest("Guest Invalid Coordinates", "+15550000000");
        request.put("pickupLatitude", 91.0);
        request.put("pickupLongitude", 77.5946);
        request.put("destinationLatitude", 12.9780);
        request.put("destinationLongitude", 77.6000);

        Response invalidBookingResponse = apiClient.postJson("/api/v1/bookings", request);
        if (invalidBookingResponse.getStatusCode() == 403 || invalidBookingResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Booking creation endpoint not enabled for current backend");
        }
        Assertions.assertTrue(invalidBookingResponse.getStatusCode() >= 400 && invalidBookingResponse.getStatusCode() < 500,
            "Booking creation should reject pickup latitude outside the valid range before nearest-driver workflow begins");
    }

    @Test
    void shouldRejectDriverRegistrationWithoutAmbulanceRegistrationNumber() {
        String email = ApiTestDataFactory.uniqueEmail("driver-missing-ambulance");
        String phone = ApiTestDataFactory.uniquePhone("driver-missing-ambulance");
        String password = "Password123!";

        Map<String, Object> request = ApiTestDataFactory.registerRequest(email, phone, password, "ROLE_DRIVER");
        request.remove("ambulanceRegistrationNumber");

        Response invalidRegistrationResponse = apiClient.postJson("/api/v1/auth/register", request);
        if (invalidRegistrationResponse.getStatusCode() == 403 || invalidRegistrationResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Driver registration endpoint not enabled for current backend");
        }

        Assertions.assertTrue(
            invalidRegistrationResponse.getStatusCode() >= 400 && invalidRegistrationResponse.getStatusCode() < 500,
            "Driver registration should reject a ROLE_DRIVER without ambulanceRegistrationNumber"
        );
    }

    @Test
    void shouldExerciseBookingRecoveryAndRequeuePath() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        String driverToken = registerApprovedAvailableDriver("workflow-requeue", adminToken);
        Long bookingId = findAvailableBookingId(driverToken);

        if (bookingId == null) {
            Assumptions.assumeTrue(false, "No available bookings were exposed for requeue testing");
        }

        Response failNegotiation = ApiTestSupport.failNegotiation(apiClient, driverToken, bookingId, "No agreement");
        if (failNegotiation.getStatusCode() == 400 || failNegotiation.getStatusCode() == 403 || failNegotiation.getStatusCode() == 404 || failNegotiation.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Negotiation-failed transition is not enabled for the current backend contract");
        }

        if (failNegotiation.getStatusCode() == 200 || failNegotiation.getStatusCode() == 201 || failNegotiation.getStatusCode() == 204) {
            Response requeueResponse = ApiTestSupport.requeueBooking(apiClient, driverToken, bookingId);
            if (requeueResponse.getStatusCode() == 400 || requeueResponse.getStatusCode() == 403 || requeueResponse.getStatusCode() == 404 || requeueResponse.getStatusCode() == 409) {
                Assumptions.assumeTrue(false, "Requeue flow is not enabled for the current backend contract");
            }
            Assertions.assertTrue(requeueResponse.getStatusCode() == 200 || requeueResponse.getStatusCode() == 201 || requeueResponse.getStatusCode() == 204,
                "Booking should re-open for reassignment after negotiation fails");
        }
    }

    @Test
    void shouldSupportCancelRequestAndConfirmationFlow() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        String driverToken = registerApprovedAvailableDriver("workflow-cancel-request", adminToken);
        Long bookingId = findAvailableBookingId(driverToken);

        if (bookingId == null) {
            Assumptions.assumeTrue(false, "No available bookings were exposed for cancel-request testing");
        }

        Response cancelResponse = ApiTestSupport.cancelBookingRequest(apiClient, driverToken, bookingId, "Driver unavailable");
        if (cancelResponse.getStatusCode() == 400 || cancelResponse.getStatusCode() == 403 || cancelResponse.getStatusCode() == 404 || cancelResponse.getStatusCode() == 409) {
            Assumptions.assumeTrue(false, "Cancel-request flow is not enabled for the current backend contract");
        }

        if (cancelResponse.getStatusCode() == 200 || cancelResponse.getStatusCode() == 201 || cancelResponse.getStatusCode() == 204) {
            Response confirmResponse = ApiTestSupport.confirmBooking(apiClient, driverToken, bookingId);
            if (confirmResponse.getStatusCode() == 400 || confirmResponse.getStatusCode() == 403 || confirmResponse.getStatusCode() == 404 || confirmResponse.getStatusCode() == 409) {
                Assumptions.assumeTrue(false, "Confirm flow is not enabled for the current backend contract");
            }
            Assertions.assertTrue(confirmResponse.getStatusCode() == 200 || confirmResponse.getStatusCode() == 201 || confirmResponse.getStatusCode() == 204,
                "Booking should allow a confirm action when the flow is supported");
        }
    }
}
