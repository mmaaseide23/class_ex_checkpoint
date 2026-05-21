package notification;

import app.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

/**
 * Final consumer that reads individual purchaser notifications from the
 * purchaser.notifications queue and prints them as formatted messages.
 *
 * Run with:  java -cp notification-service.jar notification.NotificationConsumer
 */
public class NotificationConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Notification Consumer...");

        Channel channel = RabbitConfig.createChannel();
        channel.queueDeclare(RabbitConfig.PURCHASER_QUEUE, true, false, false, null);

        System.out.println("Waiting for notifications on queue: " + RabbitConfig.PURCHASER_QUEUE);
        System.out.println("=".repeat(60));

        DeliverCallback callback = (consumerTag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                JsonNode msg = MAPPER.readTree(json);
                String line = "-".repeat(50);

                System.out.println();
                System.out.println(line);
                System.out.println("  NOTIFICATION");
                System.out.println(line);
                System.out.println("  To:      " + msg.path("purchaserName").asText());
                System.out.println("  Email:   " + msg.path("email").asText());
                System.out.println("  Type:    " + msg.path("eventType").asText());
                System.out.println("  Message: " + msg.path("message").asText());
                System.out.println(line);
            } catch (Exception e) {
                System.out.println("Raw message: " + json);
            }
        };

        channel.basicConsume(RabbitConfig.PURCHASER_QUEUE, true, callback, tag -> {});
        System.out.println("Consumer is running. Press Ctrl+C to exit.");
        Thread.currentThread().join();
    }
}
