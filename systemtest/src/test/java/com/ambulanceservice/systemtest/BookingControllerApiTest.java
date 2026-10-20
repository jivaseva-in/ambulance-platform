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
}
