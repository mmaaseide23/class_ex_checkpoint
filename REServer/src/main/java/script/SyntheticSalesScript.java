package script;

import listing.Listing;
import listing.ListingDAO;
import sale.Sale;

import app.DatabaseConfig;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SyntheticSalesScript {

    public static void main(String[] args) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM listings");
            System.out.println("Cleared existing listings");
        }

        List<Sale> randomSales = getRandom1000Sales();
        System.out.println("Fetched " + randomSales.size() + " sales");

        ListingDAO listingDAO = new ListingDAO();
        LocalDate today = LocalDate.now();
        int success = 0;
        int skipped = 0;

        for (Sale s : randomSales) {
            if (success >= 1000) break;
            if (s.purchasePrice <= 0) {
                skipped++;
                continue;
            }

            Listing listing = new Listing();
            listing.propertyId = s.propertyId;
            listing.listingDate = today;
            listing.price = Math.round(s.purchasePrice * 1.20);

            if (listingDAO.newListing(listing)) success++;
            else skipped++;
        }

        System.out.println("Done! Created: " + success + " | Skipped/duplicate: " + skipped);
    }

    private static List<Sale> getRandom1000Sales() throws SQLException {
        String sql =
            "SELECT DISTINCT ON (property_id) id, property_id, purchase_price, address" +
            " FROM sales" +
            " WHERE purchase_price IS NOT NULL AND purchase_price > 0" +
            " ORDER BY property_id, settlement_date DESC NULLS LAST";
        String wrapped = "SELECT * FROM (" + sql + ") latest ORDER BY RANDOM() LIMIT 1100";

        List<Sale> results = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(wrapped);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Sale s = new Sale();
                s.id = rs.getInt("id");
                s.propertyId = rs.getLong("property_id");
                s.purchasePrice = rs.getLong("purchase_price");
                s.address = rs.getString("address");
                results.add(s);
            }
        }
        return results;
    }
}
