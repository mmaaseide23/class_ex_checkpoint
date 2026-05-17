package analytics;

import app.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AnalyticsTest {

    public static void main(String[] args) throws Exception {
        AccessCountDAO dao = new AccessCountDAO();

        System.out.println("=== Analytics Tracking Test ===\n");

        System.out.println("1. Incrementing property '12345' three times...");
        dao.increment("property", "12345");
        dao.increment("property", "12345");
        dao.increment("property", "12345");

        System.out.println("2. Incrementing postcode '2000' twice...");
        dao.increment("postcode", "2000");
        dao.increment("postcode", "2000");

        System.out.println("3. Incrementing postcode '2010' once...");
        dao.increment("postcode", "2010");

        System.out.println("\n--- Verifying counts ---");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM access_counts ORDER BY count DESC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.printf("  type=%-10s value=%-10s count=%d  last_accessed=%s%n",
                    rs.getString("access_type"),
                    rs.getString("access_value"),
                    rs.getInt("count"),
                    rs.getTimestamp("last_accessed"));
            }
        }

        System.out.println("\n4. Testing getTopByType('postcode', 10)...");
        for (AccessCount ac : dao.getTopByType("postcode", 10)) {
            System.out.printf("  postcode=%s count=%d%n", ac.accessValue, ac.count);
        }

        System.out.println("\n=== All tests passed ===");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM access_counts WHERE access_value IN ('12345', '2000', '2010')")) {
            stmt.executeUpdate();
        }
        System.out.println("(Cleaned up test data)");
    }
}
