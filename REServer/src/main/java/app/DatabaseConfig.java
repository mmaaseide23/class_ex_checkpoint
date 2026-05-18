package app;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DatabaseConfig {

    private static final String URI = env("MONGO_URI", "mongodb://localhost:27017");
    private static final String DB_NAME = env("MONGO_DB", "realestate");

    private static MongoClient client;

    public static synchronized MongoDatabase getDatabase() {
        if (client == null) {
            client = MongoClients.create(URI);
        }
        return client.getDatabase(DB_NAME);
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }
}
