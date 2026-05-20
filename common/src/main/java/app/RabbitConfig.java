package app;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Shared RabbitMQ configuration. All services use the same exchange
 * and queue names so producers and consumers agree on routing.
 */
public class RabbitConfig {

    public static final String EXCHANGE = "realestate.events";
    public static final String PROPERTY_CHANGED_KEY = "property.changed";
    public static final String PROPERTY_HOT_KEY = "property.hot";
    public static final String EVENT_QUEUE = "notification.events";
    public static final String PURCHASER_QUEUE = "purchaser.notifications";

    private static Connection connection;

    public static synchronized Connection getConnection() throws Exception {
        if (connection == null || !connection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(env("RABBIT_HOST", "localhost"));
            factory.setPort(Integer.parseInt(env("RABBIT_PORT", "5672")));
            factory.setUsername(env("RABBIT_USER", "guest"));
            factory.setPassword(env("RABBIT_PASS", "guest"));
            connection = factory.newConnection();
        }
        return connection;
    }

    public static Channel createChannel() throws Exception {
        return getConnection().createChannel();
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
