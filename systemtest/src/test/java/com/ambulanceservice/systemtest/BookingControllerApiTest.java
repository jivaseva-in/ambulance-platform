package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class BookingControllerApiTest extends AbstractApiTest {

    @Test
    void shouldCreateBookingAndFetchById() {
        Map<String, Object> bookingRequest = ApiTestDataFactory.bookingRequest(
            "Guest Emergency Test",
            ApiTestDataFactory.uniquePhone("booking-read")
        );

        Response createResponse = apiClient.postJson("/api/v1/bookings", bookingRequest);
        ApiTestSupport.assertSuccess(createResponse, "Booking creation should succeed");

        Long bookingId = ApiTestSupport.createdBookingId(createResponse);
        Response fetchResponse = apiClient.get("/api/v1/bookings/" + bookingId);
        Assertions.assertEquals(200, fetchResponse.getStatusCode(), "Created booking should be fetchable by id");
        Assertions.assertEquals(bookingId, fetchResponse.jsonPath().getLong("id"), "Fetched booking id should match");
    }

    @Test
    void shouldListMyBookingsForAuthenticatedUser() {
        String email = ApiTestDataFactory.uniqueEmail("booking-my");
        String phone = ApiTestDataFactory.uniquePhone("booking-my");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerUser(apiClient, email, phone, password, "ROLE_USER");
        ApiTestSupport.assertSuccess(registerResponse, "User should be able to register");

        String token = ApiTestSupport.loginToken(apiClient, email, password);
        Response myBookingsResponse = apiClient.getWithAuth("/api/v1/bookings/my", token);
        Assertions.assertEquals(200, myBookingsResponse.getStatusCode(), "User should list their bookings");
    }

    @Test
    void shouldListAvailableBookings() {
        String email = ApiTestDataFactory.uniqueEmail("booking-available");
        String phone = ApiTestDataFactory.uniquePhone("booking-available");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerUser(apiClient, email, phone, password, "ROLE_USER");
        ApiTestSupport.assertSuccess(registerResponse, "User registration should succeed");

        String token = ApiTestSupport.loginToken(apiClient, email, password);
        Response response = apiClient.getWithAuth("/api/v1/bookings/available", token);
        Assertions.assertEquals(403, response.getStatusCode(), "Regular user should not be allowed to list available bookings");
    }

    @Test
    void shouldExercisePublicBookingLifecycleTransitions() {
        String userEmail = ApiTestDataFactory.uniqueEmail("booking-lifecycle");
        String userPhone = ApiTestDataFactory.uniquePhone("booking-lifecycle");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerUser(apiClient, userEmail, userPhone, password, "ROLE_USER");
        ApiTestSupport.assertSuccess(registerResponse, "User registration should succeed");

        String userToken = ApiTestSupport.loginToken(apiClient, userEmail, password);
        Response createResponse = apiClient.postJson("/api/v1/bookings", ApiTestDataFactory.bookingRequest(
            "Lifecycle Guest",
            ApiTestDataFactory.uniquePhone("booking-lifecycle-guest")
        ));
        ApiTestSupport.assertSuccess(createResponse, "Booking creation should succeed");

        Long bookingId = ApiTestSupport.createdBookingId(createResponse);
        Response fetchById = apiClient.get("/api/v1/bookings/" + bookingId);
        Assertions.assertEquals(200, fetchById.getStatusCode(), "Created booking should be retrievable by id");

        Response myBookingsResponse = apiClient.getWithAuth("/api/v1/bookings/my", userToken);
        Assertions.assertEquals(200, myBookingsResponse.getStatusCode(), "Authenticated user should list own bookings");

        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        Response openBookings = apiClient.getWithAuth("/api/v1/dispatch/bookings/open", adminToken);
        Assertions.assertEquals(200, openBookings.getStatusCode(), "Admin should be able to list open dispatch bookings");
    }

    @Test
    void shouldSupportDriverPublicLifecycleTransitions() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        String driverEmail = ApiTestDataFactory.uniqueEmail("driver-lifecycle");
        String driverPhone = ApiTestDataFactory.uniquePhone("driver-lifecycle");
        String driverPassword = "Password123!";

        Response registerResponse = ApiTestSupport.registerDriver(apiClient, driverEmail, driverPhone, driverPassword);
        ApiTestSupport.assertSuccess(registerResponse, "Driver registration should succeed");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        Assertions.assertEquals(200, pendingUsers.getStatusCode(), "Admin should list pending users");

        Long driverId = ApiTestSupport.findUserIdByEmail(pendingUsers, driverEmail);
        Assertions.assertNotNull(driverId, "New driver should be visible in pending users list");

        Response approveResponse = ApiTestSupport.approveUser(apiClient, adminToken, driverId);
        Assertions.assertTrue(approveResponse.getStatusCode() == 200 || approveResponse.getStatusCode() == 201 || approveResponse.getStatusCode() == 204,
            "Admin should approve the newly registered driver");

        String driverToken = ApiTestSupport.loginToken(apiClient, driverEmail, driverPassword);
        Response availabilityResponse = ApiTestSupport.setDriverAvailability(apiClient, driverToken, "AVAILABLE", 12.9716, 77.5946);
        Assertions.assertTrue(availabilityResponse.getStatusCode() == 200 || availabilityResponse.getStatusCode() == 201,
            "Driver should be able to set AVAILABLE status");

        Response createBooking = apiClient.postJson("/api/v1/bookings", ApiTestDataFactory.bookingRequest(
            "Driver Lifecycle Guest",
            ApiTestDataFactory.uniquePhone("driver-lifecycle-booking")
        ));
        ApiTestSupport.assertSuccess(createBooking, "Booking should be created for driver lifecycle acceptance");

        Long bookingId = ApiTestSupport.createdBookingId(createBooking);
        Response availableBookings = apiClient.getWithAuth("/api/v1/bookings/available", driverToken);
        Assertions.assertEquals(200, availableBookings.getStatusCode(), "Driver should be able to fetch available bookings");

        Response acceptResponse = ApiTestSupport.acceptBookingById(apiClient, driverToken, bookingId);
        Assertions.assertTrue(acceptResponse.getStatusCode() == 200 || acceptResponse.getStatusCode() == 201 || acceptResponse.getStatusCode() == 204,
            "Driver should be able to accept an available booking");
    }
}
