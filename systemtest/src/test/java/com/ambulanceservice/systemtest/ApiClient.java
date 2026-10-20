package com.ambulanceservice.systemtest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class ApiClient {

    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        RestAssured.baseURI = baseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public Response postJson(String path, Object body) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(baseUrl + path)
            .then()
            .extract()
            .response();
    }

    public Response postJsonWithAuth(String path, Object body, String bearerToken) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + bearerToken)
            .body(body)
            .when()
            .post(baseUrl + path)
            .then()
            .extract()
            .response();
    }

    public Response get(String path) {
        return RestAssured.given()
            .when()
            .get(baseUrl + path)
            .then()
            .extract()
            .response();
    }

    public Response getWithAuth(String path, String bearerToken) {
        return RestAssured.given()
            .header("Authorization", "Bearer " + bearerToken)
            .when()
            .get(baseUrl + path)
            .then()
            .extract()
            .response();
    }

    public Response putJsonWithAuth(String path, Object body, String bearerToken) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + bearerToken)
            .body(body)
            .when()
            .put(baseUrl + path)
            .then()
            .extract()
            .response();
    }
}

