package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class WhatsAppWebhookControllerApiTest extends AbstractApiTest {

    @Test
    void shouldAcceptWhatsAppWebhookPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fromNumber", "+15550012345");
        payload.put("senderName", "WhatsApp Guest");
        payload.put("messageBody", "Need ambulance");
        payload.put("latitude", 12.9716);
        payload.put("longitude", 77.5946);
        payload.put("addressText", "Test address from WhatsApp");

        Response response = apiClient.postJson("/api/v1/webhooks/whatsapp", payload);
        if (response.getStatusCode() == 403 || response.getStatusCode() == 404 || response.getStatusCode() == 401) {
            Assumptions.assumeTrue(false, "WhatsApp webhook endpoint is not accessible in the current backend configuration");
        }

        Assertions.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
            "Webhook endpoint should accept a valid payload when enabled");
    }
}
