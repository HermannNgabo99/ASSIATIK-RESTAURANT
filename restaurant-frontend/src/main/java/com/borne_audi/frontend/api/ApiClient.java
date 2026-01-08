package com.borne_audi.frontend.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient http;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newHttpClient();
    }

    public CompletableFuture<ApiResponse> get(String path) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .GET()
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new ApiResponse(r.statusCode(), r.body()));
    }

    public CompletableFuture<ApiResponse> postJson(String path, String json) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new ApiResponse(r.statusCode(), r.body()));
    }

    public CompletableFuture<ApiResponse> putJson(String path, String json) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> new ApiResponse(r.statusCode(), r.body()));
    }

    public record ApiResponse(int statusCode, String body) {
        public boolean isOk() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}

