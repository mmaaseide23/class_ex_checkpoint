package propertyListing;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PropertyListingDAO extends BaseDAO {

    private PropertyListing mapRow(ResultSet rs) throws SQLException {
        PropertyListing pl = new PropertyListing();
        pl.id = rs.getInt("id");
        pl.propertyId = rs.getLong("property_id");
        pl.listingDate = rs.getDate("listing_date").toLocalDate();
        pl.price = rs.getLong("price");
        return pl;
    }

    public boolean newListing(PropertyListing listing) {
        String sql = "INSERT INTO listings (property_id, listing_date, price) VALUES (?, ?, ?) ON CONFLICT (property_id, listing_date, price) DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listing.propertyId);
            stmt.setDate(2, java.sql.Date.valueOf(listing.listingDate));
            stmt.setLong(3, listing.price);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<PropertyListing> getListingsByPropertyId(String propertyId) {
        String sql = "SELECT * FROM listings WHERE property_id = ? ORDER BY listing_date";
        List<PropertyListing> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(propertyId));
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
        String sql = "SELECT * FROM listings LIMIT 100";
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
