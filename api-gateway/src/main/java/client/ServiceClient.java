package client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper over java.net.http.HttpClient. One instance is shared across the
 * gateway (HttpClient is thread-safe and designed for reuse).
 */
public class ServiceClient {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final HttpClient http;

    public ServiceClient() {
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    public <T> T getJson(String url, Class<T> type) {
        try {
            HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                throw new ServiceException(url, res.statusCode(), res.body());
            }
            return MAPPER.readValue(res.body(), type);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    public <T> List<T> getJsonList(String url, TypeReference<List<T>> type) {
        try {
            HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                throw new ServiceException(url, res.statusCode(), res.body());
            }
            return MAPPER.readValue(res.body(), type);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("GET " + url + " failed: " + e.getMessage(), e);
        }
    }

    public HttpResponse<String> postJson(String url, Object body) {
        try {
            String json = MAPPER.writeValueAsString(body);
            return http.send(
                HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("POST " + url + " failed: " + e.getMessage(), e);
        }
    }

    public HttpResponse<String> patchJson(String url, Object body) {
        try {
            String json = MAPPER.writeValueAsString(body);
            return http.send(
                HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("PATCH " + url + " failed: " + e.getMessage(), e);
        }
    }

    /** Fire-and-forget POST. Used for telemetry where a downed callee must not break the user request. */
    public CompletableFuture<Void> postJsonAsync(String url, Object body) {
        try {
            String json = MAPPER.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            return http.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .thenAccept(r -> {})
                .exceptionally(ex -> {
                    System.err.println("async POST " + url + " failed: " + ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            System.err.println("async POST " + url + " could not serialize: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    public HttpResponse<String> delete(String url) {
        try {
            return http.send(
                HttpRequest.newBuilder(URI.create(url)).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("DELETE " + url + " failed: " + e.getMessage(), e);
        }
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}