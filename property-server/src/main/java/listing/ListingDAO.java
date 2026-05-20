package listing;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListingDAO extends BaseDAO {

    private Listing mapRow(ResultSet rs) throws SQLException {
        Listing l = new Listing();
        l.id = rs.getInt("id");
        l.propertyId = rs.getLong("property_id");
        l.listingDate = rs.getDate("listing_date").toLocalDate();
        l.price = rs.getLong("price");
        return l;
    }

    public boolean newListing(Listing listing) {
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

    public List<Listing> getListingsByPropertyId(String propertyId) {
        String sql = "SELECT * FROM listings WHERE property_id = ? ORDER BY listing_date";
        List<Listing> results = new ArrayList<>();
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

    public List<Listing> getAllListings() {
        String sql = "SELECT * FROM listings LIMIT 100";
        List<Listing> results = new ArrayList<>();
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

    public List<ListingWithPostcode> getListingsByPostCode(String postCode) {
        String sql =
            "SELECT l.property_id, l.listing_date, l.price, s.post_code, s.address " +
            "FROM listings l " +
            "JOIN sales s ON s.property_id = l.property_id " +
            "WHERE s.post_code = ? " +
            "ORDER BY l.listing_date DESC";
        List<ListingWithPostcode> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, postCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ListingWithPostcode l = new ListingWithPostcode();
                l.propertyId = rs.getLong("property_id");
                l.listingDate = rs.getDate("listing_date").toLocalDate();
                l.price = rs.getLong("price");
                l.postCode = rs.getString("post_code");
                l.address = rs.getString("address");
                results.add(l);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }
}