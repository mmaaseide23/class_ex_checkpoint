package propertyListing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PropertyListingDAO {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/realestate";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private PropertyListing mapRow(ResultSet rs) throws SQLException {
        PropertyListing pl = new PropertyListing();
        pl.id = rs.getInt("id");
        pl.listingID = String.valueOf(rs.getInt("id"));
        pl.propertyID = String.valueOf(rs.getLong("property_id"));
        pl.listingDate = rs.getString("listing_date");
        pl.price = String.valueOf(rs.getLong("price"));
        return pl;
    }

    public boolean newListing(PropertyListing listing) {
        String sql = "INSERT INTO property_listings (property_id, listing_date, price) VALUES (?, ?::date, ?) ON CONFLICT (property_id, listing_date, price) DO NOTHING";        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(listing.propertyID));
            stmt.setString(2, listing.listingDate);
            stmt.setLong(3, Long.parseLong(listing.price));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PropertyListing> getListingsByPropertyId(String propertyID) {
        String sql = "SELECT * FROM property_listings WHERE property_id = ? ORDER BY listing_date";
        List<PropertyListing> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(propertyID));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public List<PropertyListing> getAllListings() {
        String sql = "SELECT * FROM property_listings LIMIT 100";
        List<PropertyListing> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}