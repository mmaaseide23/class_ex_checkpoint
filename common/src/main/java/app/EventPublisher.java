package app;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

/**
 * Fire-and-forget event publisher. Services call {@link #publish} to emit
 * domain events to the shared topic exchange without knowing who consumes them.
 */
public class EventPublisher {

    private static Channel channel;

    private static synchronized Channel getChannel() throws Exception {
        if (channel == null || !channel.isOpen()) {
            channel = RabbitConfig.createChannel();
            channel.exchangeDeclare(RabbitConfig.EXCHANGE, BuiltinExchangeType.TOPIC, true);
        }
        return channel;
    }

    /**
     * Publish a JSON event to the shared exchange with the given routing key.
     * Failures are logged but never propagated — publishing is best-effort
     * so the originating request is never blocked by the message broker.
     */
    public static void publish(String routingKey, String jsonMessage) {
        try {
            getChannel().basicPublish(
                    RabbitConfig.EXCHANGE, routingKey, null,
                    jsonMessage.getBytes(StandardCharsets.UTF_8));
            System.out.println("[EventPublisher] Published " + routingKey);
        } catch (Exception e) {
            System.err.println("[EventPublisher] Failed to publish: " + e.getMessage());
        }
    }
}
