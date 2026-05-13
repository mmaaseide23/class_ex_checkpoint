package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL = env("DB_URL", "jdbc:postgresql://localhost:5432/realestate");
    private static final String USER = env("DB_USER", "postgres");
    private static final String PASS = env("DB_PASS", "postgres");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }
}
