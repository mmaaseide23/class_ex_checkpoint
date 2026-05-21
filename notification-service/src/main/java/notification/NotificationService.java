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
 * Bridges domain events to purchaser notifications.
 *
 * Consumes property.changed and property.hot from the shared topic exchange,
 * matches interested purchasers by postcode (via the purchasers-server), and
 * republishes one notification per match to the purchaser.notifications queue.
 */
public class NotificationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static final String PURCHASERS_URL = env("PURCHASERS_URL", "http://localhost:7072");
    private static final String PROPERTY_URL   = env("PROPERTY_URL",   "http://localhost:7071");

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Notification Service...");

        Channel channel = RabbitConfig.createChannel();
        channel.exchangeDeclare(RabbitConfig.EXCHANGE, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(RabbitConfig.EVENT_QUEUE, true, false, false, null);
        channel.queueDeclare(RabbitConfig.PURCHASER_QUEUE, true, false, false, null);
        channel.queueBind(RabbitConfig.EVENT_QUEUE, RabbitConfig.EXCHANGE, RabbitConfig.PROPERTY_CHANGED_KEY);
        channel.queueBind(RabbitConfig.EVENT_QUEUE, RabbitConfig.EXCHANGE, RabbitConfig.PROPERTY_HOT_KEY);

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

    private static void handleEvent(Channel ch, String key, String json) throws Exception {
        JsonNode event = MAPPER.readTree(json);
        if (RabbitConfig.PROPERTY_CHANGED_KEY.equals(key)) {
            handlePropertyChanged(ch, event);
        } else if (RabbitConfig.PROPERTY_HOT_KEY.equals(key)) {
            handlePropertyHot(ch, event);
        }
    }

    private static void handlePropertyChanged(Channel ch, JsonNode event) throws Exception {
        String postCode = event.path("postCode").asText("");
        if (postCode.isEmpty()) return;

        String action = event.path("action").asText("NEW_LISTING");
        String address = event.path("address").asText("Unknown");
        long price = event.path("purchasePrice").asLong(0);
        long propertyId = event.path("propertyId").asLong(0);

        String message = formatChangedMessage(action, address, postCode, price, event);
        notifyMatchingPurchasers(ch, postCode, "PROPERTY_CHANGED", action, propertyId, message);
    }

    private static void handlePropertyHot(Channel ch, JsonNode event) throws Exception {
        String propertyId = event.path("accessValue").asText("");
        if (propertyId.isEmpty()) return;

        // Hot event doesn't carry postcode/address — enrich from the property server.
        JsonNode property = fetchPropertyDetails(propertyId);
        if (property == null) return;

        String postCode = property.path("postCode").asText("");
        if (postCode.isEmpty()) return;
        String address = property.path("address").asText("Unknown");

        String message = String.format(
                "Hot property alert! Property at %s (postcode %s) is trending",
                address, postCode);
        notifyMatchingPurchasers(ch, postCode, "PROPERTY_HOT", "PROPERTY_HOT",
                Long.parseLong(propertyId), message);
    }

    private static void notifyMatchingPurchasers(Channel ch, String postCode, String eventType,
                                                  String action, long propertyId, String message)
            throws Exception {
        JsonNode purchasers = fetchPurchasersWithPostcodePrefs();
        if (purchasers == null || !purchasers.isArray()) return;

        int sent = 0;
        for (JsonNode p : purchasers) {
            if (!hasPostcode(p, postCode)) continue;

            ObjectNode note = MAPPER.createObjectNode()
                    .put("purchaserId", p.path("id").asInt())
                    .put("purchaserName",
                            p.path("firstName").asText() + " " + p.path("lastName").asText())
                    .put("email", p.path("email").asText())
                    .put("message", message)
                    .put("eventType", eventType)
                    .put("action", action)
                    .put("propertyId", propertyId);

            ch.basicPublish("", RabbitConfig.PURCHASER_QUEUE, null,
                    note.toString().getBytes(StandardCharsets.UTF_8));
            sent++;
        }
        System.out.println("[NotificationService] Sent " + sent + " " + action + " notifications");
    }

    private static String formatChangedMessage(String action, String address, String postCode,
                                                long price, JsonNode event) {
        return switch (action) {
            case "NEW_SALE" -> String.format(
                    "New sale registered at %s (postcode %s) for $%,d", address, postCode, price);
            case "NEW_LISTING" -> String.format(
                    "New listing at %s (postcode %s) priced at $%,d", address, postCode, price);
            case "PRICE_CHANGE" -> String.format(
                    "Price change at %s (postcode %s): was $%,d, now $%,d",
                    address, postCode, event.path("oldPrice").asLong(0), price);
            case "STATUS_CHANGE" -> String.format(
                    "Status change at %s (postcode %s): %s -> %s",
                    address, postCode,
                    event.path("oldStatus").asText("?"), event.path("status").asText("?"));
            default -> String.format("Property update at %s (postcode %s)", address, postCode);
        };
    }

    private static boolean hasPostcode(JsonNode purchaser, String postCode) {
        JsonNode postcodes = purchaser.path("postcodes");
        if (!postcodes.isArray()) return false;
        for (JsonNode pc : postcodes) {
            if (postCode.equals(pc.asText())) return true;
        }
        return false;
    }

    private static JsonNode fetchPurchasersWithPostcodePrefs() {
        return fetchJson(PURCHASERS_URL + "/purchaser/with-postcode-prefs", "purchasers");
    }

    private static JsonNode fetchPropertyDetails(String propertyId) {
        return fetchJson(PROPERTY_URL + "/sale/" + propertyId, "property " + propertyId);
    }

    private static JsonNode fetchJson(String url, String label) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return MAPPER.readTree(resp.body());
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to fetch " + label + ": " + e.getMessage());
        }
        return null;
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
