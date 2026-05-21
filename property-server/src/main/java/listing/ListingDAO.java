package listing;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListingDAO extends BaseDAO {

    private Listing mapRow(ResultSet rs) throws SQLException {
        Listing l = new Listing();
        l.id = rs.getInt("id");
        l.propertyId = rs.getLong("property_id");
        l.listingDate = rs.getDate("listing_date").toLocalDate();
        l.price = rs.getLong("price");
        l.status = rs.getString("status");
        return l;
    }

    public boolean newListing(Listing listing) {
        String status = (listing.status == null || listing.status.isBlank()) ? "Pending" : listing.status;
        String sql = "INSERT INTO listings (property_id, listing_date, price, status) VALUES (?, ?, ?, ?) ON CONFLICT (property_id, listing_date, price) DO NOTHING";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listing.propertyId);
            stmt.setDate(2, java.sql.Date.valueOf(listing.listingDate));
            stmt.setLong(3, listing.price);
            stmt.setString(4, status);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Listing> getListingById(int id) {
        String sql = "SELECT * FROM listings WHERE id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public boolean updateListing(int id, Long newPrice, String newStatus) {
        StringBuilder sql = new StringBuilder("UPDATE listings SET ");
        List<Object> params = new ArrayList<>();
        if (newPrice != null) {
            sql.append("price = ?");
            params.add(newPrice);
        }
        if (newStatus != null) {
            if (!params.isEmpty()) sql.append(", ");
            sql.append("status = ?");
            params.add(newStatus);
        }
        if (params.isEmpty()) return false;
        sql.append(" WHERE id = ?");
        params.add(id);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long lp) stmt.setLong(i + 1, lp);
                else if (p instanceof Integer ip) stmt.setInt(i + 1, ip);
                else stmt.setString(i + 1, (String) p);
            }
            return stmt.executeUpdate() > 0;
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
            "SELECT l.property_id, l.listing_date, l.price, l.status, s.post_code, s.address " +
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
                l.status = rs.getString("status");
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