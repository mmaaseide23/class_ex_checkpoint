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
     * The `action` field tells us which message to format.
     */
    private static void handlePropertyChanged(Channel ch, JsonNode event) throws Exception {
        String postCode = event.path("postCode").asText("");
        if (postCode.isEmpty()) {
            System.out.println("[NotificationService] No postcode in event, skipping");
            return;
        }

        String action = event.path("action").asText("NEW_LISTING");
        String address = event.path("address").asText("Unknown");
        long price = event.path("purchasePrice").asLong(0);
        long propertyId = event.path("propertyId").asLong(0);
        String message = formatMessage(action, address, postCode, price, event);

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
        System.out.println("[NotificationService] Sent " + sent + " " + action + " notifications");
    }

    private static String formatMessage(String action, String address, String postCode,
                                         long price, JsonNode event) {
        return switch (action) {
            case "NEW_SALE" -> String.format(
                    "New sale registered at %s (postcode %s) for $%,d",
                    address, postCode, price);
            case "NEW_LISTING" -> String.format(
                    "New listing at %s (postcode %s) priced at $%,d",
                    address, postCode, price);
            case "PRICE_CHANGE" -> {
                long oldPrice = event.path("oldPrice").asLong(0);
                yield String.format(
                        "Price change at %s (postcode %s): was $%,d, now $%,d",
                        address, postCode, oldPrice, price);
            }
            case "STATUS_CHANGE" -> {
                String oldStatus = event.path("oldStatus").asText("?");
                String newStatus = event.path("status").asText("?");
                yield String.format(
                        "Status change at %s (postcode %s): %s -> %s",
                        address, postCode, oldStatus, newStatus);
            }
            default -> String.format(
                    "Property update at %s (postcode %s)",
                    address, postCode);
        };
    }

    /**
     * A property is trending (search/access count incremented).
     * We need to fetch the property details from the property-server
     * to find its postcode, then match interested purchasers.
     */
    private static void handlePropertyHot(Channel ch, JsonNode event) throws Exception {
        String propertyId = event.path("accessValue").asText("");
        if (propertyId.isEmpty()) return;

        // Look up property to get the postcode
        JsonNode property = fetchPropertyDetails(propertyId);
        if (property == null) {
            System.out.println("[NotificationService] Could not fetch property " + propertyId);
            return;
        }

        String postCode = property.path("postCode").asText("");
        if (postCode.isEmpty()) return;
        String address = property.path("address").asText("Unknown");

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
                                "Hot property alert! Property at %s (postcode %s) is trending",
                                address, postCode))
                        .put("eventType", "PROPERTY_HOT")
                        .put("propertyId", Long.parseLong(propertyId));

                ch.basicPublish("", RabbitConfig.PURCHASER_QUEUE, null,
                        note.toString().getBytes(StandardCharsets.UTF_8));
                sent++;
            }
        }
        System.out.println("[NotificationService] Sent " + sent + " PROPERTY_HOT notifications");
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
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PURCHASERS_URL + "/purchaser/with-postcode-prefs"))
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return MAPPER.readTree(resp.body());
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to fetch purchasers: " + e.getMessage());
        }
        return null;
    }

    private static JsonNode fetchPropertyDetails(String propertyId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PROPERTY_URL + "/sale/" + propertyId))
                    .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return MAPPER.readTree(resp.body());
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
