package com.ambulanceservice.systemtest;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class AuthControllerApiTest extends AbstractApiTest {

    @Test
    void shouldRegisterAndLoginUser() {
        String email = ApiTestDataFactory.uniqueEmail("auth-user");
        String phone = ApiTestDataFactory.uniquePhone("auth-user");
        String password = "Password123!";

        Response registerResponse = ApiTestSupport.registerUser(apiClient, email, phone, password, "ROLE_USER");
        ApiTestSupport.assertSuccess(registerResponse, "User registration should succeed");

        String token = ApiTestSupport.loginToken(apiClient, email, password);
        Assertions.assertNotNull(token, "Token should be returned after successful login");
    }

    @Test
    void shouldRejectDuplicatePhoneRegistration() {
        String email1 = ApiTestDataFactory.uniqueEmail("duplicate-phone-1");
        String email2 = ApiTestDataFactory.uniqueEmail("duplicate-phone-2");
        String phone = ApiTestDataFactory.uniquePhone("duplicate-phone");
        String password = "Password123!";

        Response firstRegister = ApiTestSupport.registerUser(apiClient, email1, phone, password, "ROLE_USER");
        ApiTestSupport.assertSuccess(firstRegister, "First registration with unique phone should succeed");

        Response secondRegister = ApiTestSupport.registerUser(apiClient, email2, phone, password, "ROLE_USER");
        Assertions.assertEquals(400, secondRegister.getStatusCode(), "Duplicate phone should be rejected");
    }

    @Test
    void shouldAllowAdminLogin() {
        String token = ApiTestSupport.loginToken(apiClient, properties.getAdminEmail(), properties.getAdminPassword());
        Assertions.assertNotNull(token, "Admin login should return a token");
    }
}

