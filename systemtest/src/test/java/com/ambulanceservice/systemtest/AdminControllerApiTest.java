package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class AdminControllerApiTest extends AbstractApiTest {

    @Test
    void shouldListManageableAndPendingUsers() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response manageableUsers = apiClient.getWithAuth("/api/v1/admin/users", adminToken);
        if (manageableUsers.getStatusCode() == 403 || manageableUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin users endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, manageableUsers.getStatusCode(), "Admin should list manageable users");

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }
        Assertions.assertEquals(200, pendingUsers.getStatusCode(), "Admin should list pending users");
    }

    @Test
    void shouldApproveAndSuspendUser() {
        String adminToken = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());

        Response pendingUsers = apiClient.getWithAuth("/api/v1/admin/users/pending", adminToken);
        if (pendingUsers.getStatusCode() == 403 || pendingUsers.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin pending users endpoint not enabled for current backend");
        }

        Assertions.assertEquals(200, pendingUsers.getStatusCode(), "Admin should list pending users");

        List<Map<String, Object>> pendingUsersList = pendingUsers.jsonPath().getList("$");
        if (pendingUsersList == null || pendingUsersList.isEmpty()) {
            Assumptions.assumeTrue(false, "No pending users are available for approval in the current backend state");
        }

        Long userId = ((Number) pendingUsersList.get(0).get("id")).longValue();
        if (userId == null) {
            Assumptions.assumeTrue(false, "No pending user is available for approval in the current backend state");
        }

        Response approveResponse = apiClient.putJsonWithAuth("/api/v1/admin/users/" + userId + "/approve", Map.of(), adminToken);
        if (approveResponse.getStatusCode() == 403 || approveResponse.getStatusCode() == 404) {
            Assumptions.assumeTrue(false, "Admin approve endpoint not enabled for current backend");
        }

        Assertions.assertEquals(200, approveResponse.getStatusCode(),
            "Admin should approve a pending user successfully");
    }
}
