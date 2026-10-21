package com.ambulanceservice.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class GeoCodingService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${app.http.proxy.host:}")
    private String proxyHost;

    @Value("${app.http.proxy.port:0}")
    private int proxyPort;

    @Value("${app.http.proxy.username:}")
    private String proxyUsername;

    @Value("${app.http.proxy.password:}")
    private String proxyPassword;

    @Value("${app.http.proxy.enabled:false}")
    private boolean proxyEnabled;

    public GeoCodingService() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder();

        if (proxyEnabled && proxyHost != null && !proxyHost.isBlank() && proxyPort > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));

            if (proxyUsername != null && !proxyUsername.isBlank()) {
                final String username = proxyUsername;
                final char[] password = (proxyPassword == null ? new char[0] : proxyPassword.toCharArray());

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            }
        }

        return builder.build();
    }

    public Optional<GeoPoint> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        HttpClient client = buildHttpClient();

        try {
            String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + encodedAddress;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "ambulance-service-api/1.0")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                return Optional.empty();
            }

            JsonNode first = root.get(0);
            double latitude = first.path("lat").asDouble(Double.NaN);
            double longitude = first.path("lon").asDouble(Double.NaN);

            if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                return Optional.empty();
            }

            return Optional.of(new GeoPoint(latitude, longitude));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public record GeoPoint(double latitude, double longitude) {
    }
}
