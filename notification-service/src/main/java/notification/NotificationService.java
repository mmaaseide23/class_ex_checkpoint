package notification;

import app.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Event consumer that bridges domain events to purchaser notifications.
 *
 * Subscribes to property.changed and property.hot events on the shared
 * topic exchange, looks up interested purchasers via the purchasers-server
 * HTTP API, and publishes individual notification messages to the
 * purchaser.notifications queue.
 */
public class NotificationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final String PURCHASERS_URL =
            env("PURCHASERS_URL", "http://localhost:7072");
    private static final String PROPERTY_URL =
            env("PROPERTY_URL", "http://localhost:7071");

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Notification Service...");

        Channel channel = RabbitConfig.createChannel();

        // Declare exchange and queues
        channel.exchangeDeclare(RabbitConfig.EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(RabbitConfig.EVENT_QUEUE, true, false, false, null);
        channel.queueDeclare(RabbitConfig.PURCHASER_QUEUE, true, false, false, null);

        // Bind event queue to both routing keys
        channel.queueBind(RabbitConfig.EVENT_QUEUE, RabbitConfig.EXCHANGE,
                RabbitConfig.PROPERTY_CHANGED_KEY);
        channel.queueBind(RabbitConfig.EVENT_QUEUE, RabbitConfig.EXCHANGE,
                RabbitConfig.PROPERTY_HOT_KEY);

        System.out.println("Listening for events on queue: " + RabbitConfig.EVENT_QUEUE);

        DeliverCallback callback = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String key = delivery.getEnvelope().getRoutingKey();
            System.out.println("[NotificationService] Received " + key + ": " + body);

            try {
                handleEvent(channel, key, body);
            } catch (Exception e) {
                System.err.println("[NotificationService] Error: " + e.getMessage());
                e.printStackTrace();
            }
        };

        channel.basicConsume(RabbitConfig.EVENT_QUEUE, true, callback, tag -> {});
        System.out.println("Notification Service is running. Press Ctrl+C to exit.");
        Thread.currentThread().join();
    }

    // ── Event dispatch ──────────────────────────────────────────────

    private static void handleEvent(Channel ch, String key, String json) throws Exception {
        JsonNode event = MAPPER.readTree(json);

        if (RabbitConfig.PROPERTY_CHANGED_KEY.equals(key)) {
            handlePropertyChanged(ch, event);
        } else if (RabbitConfig.PROPERTY_HOT_KEY.equals(key)) {
            handlePropertyHot(ch, event);
        }
    }

    /**
     * A property was listed / price changed / status changed.
     * The event already carries the postcode so we can match purchasers directly.
     */
    private static void handlePropertyChanged(Channel ch, JsonNode event) throws Exception {
        String postCode = event.path("postCode").asText("");
        if (postCode.isEmpty()) {
            System.out.println("[NotificationService] No postcode in event, skipping");
            return;
        }

        String address = event.path("address").asText("Unknown");
        long propertyId = event.path("propertyId").asLong(0);
        String action = event.path("action").asText("");
        String message = buildPropertyChangedMessage(event, action, address, postCode);

        JsonNode purchasers = fetchPurchasersWithPostcodePrefs();
        if (purchasers == null || !purchasers.isArray()) return;

        int sent = 0;
        for (JsonNode p : purchasers) {
            if (hasPostcode(p, postCode)) {
                ObjectNode note = MAPPER.createObjectNode()
                        .put("purchaserId", p.path("id").asInt())
                        .put("purchaserName",
                                p.path("firstName").asText() + " " + p.path("lastName").asText())
                        .put("email", p.path("email").asText())
                        .put("message", message)
                        .put("eventType", "PROPERTY_CHANGED")
                        .put("action", action)
                        .put("propertyId", propertyId);

                ch.basicPublish("", RabbitConfig.PURCHASER_QUEUE, null,
                        note.toString().getBytes(StandardCharsets.UTF_8));
                sent++;
            }
        }
        System.out.println("[NotificationService] Sent " + sent + " PROPERTY_CHANGED (" + action + ") notifications");
    }

    private static String buildPropertyChangedMessage(JsonNode event, String action,
                                                      String address, String postCode) {
        return switch (action) {
            case "PRICE_CHANGED" -> String.format(
                    "Price changed for property at %s (postcode %s): $%,d → $%,d",
                    address, postCode,
                    event.path("oldPrice").asLong(0),
                    event.path("newPrice").asLong(0));
            case "STATUS_CHANGED" -> String.format(
                    "Status changed for property at %s (postcode %s): %s → %s",
                    address, postCode,
                    event.path("oldStatus").asText(""),
                    event.path("newStatus").asText(""));
            default -> String.format(
                    "New property listed at %s (postcode %s) for $%,d",
                    address, postCode,
                    event.path("purchasePrice").asLong(0));
        };
    }

    /**
     * A property is trending (search/access count incremented).
     * We only notify when the property is currently "for sale" — i.e. has a
     * Pending listing — so we look up both the property and its listings.
     */
    private static void handlePropertyHot(Channel ch, JsonNode event) throws Exception {
        String propertyId = event.path("accessValue").asText("");
        if (propertyId.isEmpty()) return;
        long count = event.path("count").asLong(0);

        JsonNode property = fetchPropertyDetails(propertyId);
        if (property == null) {
            System.out.println("[NotificationService] Could not fetch property " + propertyId);
            return;
        }

        String postCode = property.path("postCode").asText("");
        if (postCode.isEmpty()) return;
        String address = property.path("address").asText("Unknown");

        // Only notify for properties currently "for sale" (have a Pending listing)
        JsonNode listings = fetchListings(propertyId);
        if (!hasPendingListing(listings)) {
            System.out.println("[NotificationService] Property " + propertyId
                    + " has no Pending listing; skipping HOT notification");
            return;
        }

        JsonNode purchasers = fetchPurchasersWithPostcodePrefs();
        if (purchasers == null || !purchasers.isArray()) return;

        int sent = 0;
        for (JsonNode p : purchasers) {
            if (hasPostcode(p, postCode)) {
                ObjectNode note = MAPPER.createObjectNode()
                        .put("purchaserId", p.path("id").asInt())
                        .put("purchaserName",
                                p.path("firstName").asText() + " " + p.path("lastName").asText())
                        .put("email", p.path("email").asText())
                        .put("message", String.format(
                                "Hot property alert! %s (postcode %s) has been viewed %d times",
                                address, postCode, count))
                        .put("eventType", "PROPERTY_HOT")
                        .put("propertyId", Long.parseLong(propertyId))
                        .put("count", count);

                ch.basicPublish("", RabbitConfig.PURCHASER_QUEUE, null,
                        note.toString().getBytes(StandardCharsets.UTF_8));
                sent++;
            }
        }
        System.out.println("[NotificationService] Sent " + sent
                + " PROPERTY_HOT notifications (count=" + count + ")");
    }

    private static boolean hasPendingListing(JsonNode listings) {
        if (listings == null || !listings.isArray()) return false;
        for (JsonNode l : listings) {
            if ("Pending".equals(l.path("status").asText())) return true;
        }
        return false;
    }

    private static JsonNode fetchListings(String propertyId) {
        String url = PROPERTY_URL + "/listing/" + propertyId;
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode body = MAPPER.readTree(resp.body());
                int n = body.isArray() ? body.size() : 0;
                System.out.println("[NotificationService] HTTP GET " + url + " -> 200 (" + n + " listings)");
                return body;
            }
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to fetch listings: " + e.getMessage());
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static boolean hasPostcode(JsonNode purchaser, String postCode) {
        JsonNode postcodes = purchaser.path("postcodes");
        if (postcodes.isArray()) {
            for (JsonNode pc : postcodes) {
                if (postCode.equals(pc.asText())) return true;
            }
        }
        return false;
    }

    private static JsonNode fetchPurchasersWithPostcodePrefs() {
        String url = PURCHASERS_URL + "/purchaser/with-postcode-prefs";
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode body = MAPPER.readTree(resp.body());
                int count = body.isArray() ? body.size() : 0;
                System.out.println("[NotificationService] HTTP GET " + url + " -> 200 (" + count + " purchasers)");
                return body;
            }
            System.err.println("[NotificationService] HTTP GET " + url + " -> " + resp.statusCode());
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to fetch purchasers: " + e.getMessage());
        }
        return null;
    }

    private static JsonNode fetchPropertyDetails(String propertyId) {
        String url = PROPERTY_URL + "/sale/" + propertyId;
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                System.out.println("[NotificationService] HTTP GET " + url + " -> 200");
                return MAPPER.readTree(resp.body());
            }
            System.err.println("[NotificationService] HTTP GET " + url + " -> " + resp.statusCode());
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to fetch property: " + e.getMessage());
        }
        return null;
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
