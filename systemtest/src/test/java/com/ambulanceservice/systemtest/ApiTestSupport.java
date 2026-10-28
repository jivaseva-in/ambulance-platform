package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApiTestSupport {

    private ApiTestSupport() {
    }

    public static Response registerUser(ApiClient apiClient, String email, String phone, String password, String role) {
        Map<String, Object> request = ApiTestDataFactory.registerRequest(email, phone, password, role);
        return apiClient.postJson("/api/v1/auth/register", request);
    }

    public static Response registerDriver(ApiClient apiClient, String email, String phone, String password) {
        return registerUser(apiClient, email, phone, password, "ROLE_DRIVER");
    }

    public static String loginToken(ApiClient apiClient, String username, String password) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("username", username);
        request.put("password", password);

        Response response = apiClient.postJson("/api/v1/auth/login", request);
        Assertions.assertEquals(200, response.getStatusCode(), "Login should succeed for user " + username);

        String token = response.jsonPath().getString("token");
        Assertions.assertNotNull(token, "Auth token should be present");
        return token;
    }

    public static Long createdBookingId(Response createResponse) {
        Long bookingId = createResponse.jsonPath().getLong("id");
        Assertions.assertNotNull(bookingId, "Created booking id should be present");
        return bookingId;
    }

    public static Long userIdFrom(Response response) {
        Long userId = response.jsonPath().getLong("userId");
        if (userId != null) {
            return userId;
        }
        return response.jsonPath().getLong("id");
    }

    public static Long findUserIdByEmail(Response usersResponse, String email) {
        List<Map<String, Object>> users = usersResponse.jsonPath().getList("$");
        if (users == null) {
            return null;
        }

        for (Map<String, Object> user : users) {
            Object userEmail = user.get("email");
            if (email.equalsIgnoreCase(String.valueOf(userEmail))) {
                Object id = user.get("id");
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }
        }
        return null;
    }

    public static Response approveUser(ApiClient apiClient, String adminToken, Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        return apiClient.putJsonWithAuth("/api/v1/admin/users/" + userId + "/approve", payload, adminToken);
    }

    public static Response updateBookingStatus(ApiClient apiClient, String driverToken, Long bookingId, String status, String cancellationReason) {
        Map<String, Object> payload = ApiTestDataFactory.statusUpdateRequest(status, cancellationReason);
        return apiClient.putJsonWithAuth("/api/v1/bookings/" + bookingId + "/status", payload, driverToken);
    }

    public static Response assignDriver(ApiClient apiClient, String adminToken, Long bookingId, Long driverId) {
        Map<String, Object> payload = ApiTestDataFactory.assignDriverRequest(driverId);
        return apiClient.putJsonWithAuth("/api/v1/dispatch/bookings/" + bookingId + "/assign", payload, adminToken);
    }

    public static Response acceptBooking(ApiClient apiClient, String driverToken, Long bookingId) {
        Response heldResponse = updateBookingStatus(apiClient, driverToken, bookingId, "DRIVER_HELD", null);
        if (heldResponse.getStatusCode() == 200 || heldResponse.getStatusCode() == 201 || heldResponse.getStatusCode() == 204) {
            return heldResponse;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        return apiClient.putJsonWithAuth("/api/v1/bookings/" + bookingId + "/accept", payload, driverToken);
    }

    public static Response setDriverAvailability(ApiClient apiClient, String driverToken, String availabilityStatus, Double latitude, Double longitude) {
        Map<String, Object> payload = ApiTestDataFactory.driverAvailabilityRequest(availabilityStatus, latitude, longitude);
        return apiClient.putJsonWithAuth("/api/v1/drivers/me/availability", payload, driverToken);
    }

    public static Response updateDriverLocation(ApiClient apiClient, String driverToken, double latitude, double longitude) {
        Map<String, Object> payload = ApiTestDataFactory.driverLocationUpdateRequest(latitude, longitude);
        return apiClient.putJsonWithAuth("/api/v1/drivers/me/location", payload, driverToken);
    }

    public static Response getNearestAvailableDrivers(ApiClient apiClient, String adminToken,
                                                     double pickupLatitude, double pickupLongitude,
                                                     double destinationLatitude, double destinationLongitude) {
        Map<String, Object> query = ApiTestDataFactory.nearestDriverQuery(
            pickupLatitude, pickupLongitude, destinationLatitude, destinationLongitude
        );
        return apiClient.getWithAuthAndQuery("/api/v1/dispatch/drivers/nearest", adminToken, query);
    }

    public static void assertSuccess(Response response, String message) {
        int status = response.getStatusCode();
        Assertions.assertTrue(status == 200 || status == 201 || status == 204, message + " [status=" + status + "]");
    }

    public static Response requeueBooking(ApiClient apiClient, String driverToken, Long bookingId) {
        return apiClient.putWithAuth("/api/v1/bookings/" + bookingId + "/requeue", driverToken);
    }

    public static Response failNegotiation(ApiClient apiClient, String driverToken, Long bookingId, String reason) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            query.put("reason", reason);
        }
        return apiClient.putWithAuthAndQuery("/api/v1/bookings/" + bookingId + "/negotiation-failed", driverToken, query);
    }

    public static Response confirmBooking(ApiClient apiClient, String driverToken, Long bookingId) {
        return apiClient.putWithAuth("/api/v1/bookings/" + bookingId + "/confirm", driverToken);
    }

    public static Response cancelBookingRequest(ApiClient apiClient, String bearerToken, Long bookingId, String reason) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (reason != null && !reason.isBlank()) {
            query.put("reason", reason);
        }
        return apiClient.putWithAuthAndQuery("/api/v1/bookings/" + bookingId + "/cancel-request", bearerToken, query);
    }

    public static Response acceptBookingById(ApiClient apiClient, String bearerToken, Long bookingId) {
        return apiClient.putWithAuth("/api/v1/bookings/" + bookingId + "/accept", bearerToken);
    }

    public static Response createPhoneBooking(ApiClient apiClient, String adminToken, String phoneNumber, String callerName) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("callerName", callerName);
        payload.put("callerPhone", phoneNumber);
        payload.put("pickupAddress", "Phone booking dispatch test address");
        payload.put("pickupLatitude", 12.9716);
        payload.put("pickupLongitude", 77.5946);
        payload.put("destinationHospital", "City General Hospital");
        payload.put("destinationLatitude", 12.9780);
        payload.put("destinationLongitude", 77.6000);
        payload.put("triageNotes", "Chest pain - dispatch request created from system test");
        payload.put("ambulanceType", "ADVANCED_LIFE_SUPPORT");
        payload.put("dispatcherInternalNotes", "System test dispatch call");
        return apiClient.postJsonWithAuth("/api/v1/dispatch/phone-call", payload, adminToken);
    }
}
