package script;

import property.Property;
import propertyListing.PropertyListing;
import propertyListing.PropertyListingDAO;

import app.DatabaseConfig;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SyntheticSalesScript {

    public static void main(String[] args) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM property_listings");
            System.out.println("Cleared existing listings");
        }

        List<Property> randomProperties = getRandom1000Properties();
        System.out.println("Fetched " + randomProperties.size() + " properties");

        PropertyListingDAO listingDAO = new PropertyListingDAO();
        LocalDate today = LocalDate.now();
        int success = 0;
        int skipped = 0;

        for (Property p : randomProperties) {
            if (success >= 1000) break;
            if (p.purchasePrice <= 0) {
                skipped++;
                continue;
            }

            PropertyListing listing = new PropertyListing();
            listing.propertyId = p.propertyId;
            listing.listingDate = today;
            listing.price = Math.round(p.purchasePrice * 1.20);

            if (listingDAO.newListing(listing)) success++;
            else skipped++;
        }

        System.out.println("Done! Created: " + success + " | Skipped/duplicate: " + skipped);
    }

    private static List<Property> getRandom1000Properties() throws SQLException {
        String sql =
            "SELECT DISTINCT ON (property_id) id, property_id, purchase_price, address" +
            " FROM properties" +
            " WHERE purchase_price IS NOT NULL AND purchase_price > 0" +
            " ORDER BY property_id, settlement_date DESC NULLS LAST";
        String wrapped = "SELECT * FROM (" + sql + ") latest ORDER BY RANDOM() LIMIT 1100";

        List<Property> results = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(wrapped);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Property p = new Property();
                p.id = rs.getInt("id");
                p.propertyId = rs.getLong("property_id");
                p.purchasePrice = rs.getLong("purchase_price");
                p.address = rs.getString("address");
                results.add(p);
            }
        }
        return results;
    }
}
