package script;

import property.Property;
import propertyListing.PropertyListing;
import propertyListing.PropertyListingDAO;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SyntheticSalesScript {

  private static final String DB_URL = "jdbc:postgresql://localhost:5432/realestate";

  public static void main(String[] args) throws SQLException {

    // Drop and recreate to guarantee a clean 1000
    try (Connection conn = DriverManager.getConnection(DB_URL);
         Statement stmt = conn.createStatement()) {
      stmt.execute("DROP TABLE IF EXISTS property_listings");
      stmt.execute(
              "CREATE TABLE property_listings (" +
                      "id SERIAL PRIMARY KEY ," +
                      "property_id BIGINT NOT NULL," +
                      "listing_date DATE NOT NULL," +
                      "price BIGINT NOT NULL," +
                      "CONSTRAINT unique_property_listing UNIQUE (property_id, listing_date, price)" +
                      ")"
      );
      System.out.println("Table reset");
    }

    List<Property> randomProperties = getRandom1000Properties();
    System.out.println("Fetched " + randomProperties.size() + " properties");

    PropertyListingDAO listingDAO = new PropertyListingDAO();
    String today = LocalDate.now().toString();
    int success = 0;
    int skipped = 0;

    for (Property p : randomProperties) {
      if (success >= 1000) break;
      try {
        long lastPrice = Long.parseLong(p.purchasePrice);
        long newPrice = Math.round(lastPrice * 1.20);

        PropertyListing listing = new PropertyListing();
        listing.propertyID = p.propertyID;
        listing.listingDate = today;
        listing.price = String.valueOf(newPrice);

        if (listingDAO.newListing(listing)) success++;
        else skipped++;

      } catch (NumberFormatException e) {
        System.out.println("Skipping property - invalid data | id: " + p.id +
                " propertyID: " + p.propertyID +
                " purchasePrice: " + p.purchasePrice +
                " address: " + p.address);
        skipped++;
      }
    }

    System.out.println("Done! Created: " + success + " | Skipped/duplicate: " + skipped);
  }

  private static List<Property> getRandom1000Properties() throws SQLException {
    String sql =
            "SELECT * FROM (" +
                    "  SELECT DISTINCT ON (property_id) id, property_id, purchase_price, address" +
                    "  FROM properties" +
                    "  ORDER BY property_id, settlement_date DESC" +
                    ") latest " +
                    "ORDER BY RANDOM() LIMIT 1100";
    List<Property> results = new ArrayList<>();

    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        Property p = new Property();
        p.id = rs.getInt("id");
        p.propertyID = rs.getString("property_id");
        p.purchasePrice = String.valueOf(rs.getLong("purchase_price"));
        p.address = rs.getString("address");
        results.add(p);
      }
    }
    return results;
  }
}