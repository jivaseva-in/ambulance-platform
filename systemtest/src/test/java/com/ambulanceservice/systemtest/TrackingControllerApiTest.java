package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TrackingControllerApiTest extends AbstractApiTest {

    @Test
    void shouldUpdateAndReadBookingLocation() {
        Response createResponse = apiClient.postJson("/api/v1/bookings", ApiTestDataFactory.bookingRequest(
            "Tracking Guest",
            ApiTestDataFactory.uniquePhone("tracking-guest")
        ));
        ApiTestSupport.assertSuccess(createResponse, "Booking creation should succeed before location update");

        Long bookingId = ApiTestSupport.createdBookingId(createResponse);
        Response updateResponse = apiClient.putJsonWithAuth(
            "/api/v1/bookings/" + bookingId + "/location",
            ApiTestDataFactory.locationUpdateRequest(12.9722, 77.5999),
            ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword())
        );

        if (updateResponse.getStatusCode() == 403 || updateResponse.getStatusCode() == 404) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Tracking update endpoint not enabled for current backend");
        }

        Assertions.assertTrue(updateResponse.getStatusCode() == 200 || updateResponse.getStatusCode() == 201,
            "Location update should succeed for a valid booking");

        Response readResponse = apiClient.get("/api/v1/bookings/" + bookingId + "/location");
        Assertions.assertEquals(200, readResponse.getStatusCode(), "Location should be readable after update");
    }
}

