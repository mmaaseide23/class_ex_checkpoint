package property;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PropertyDAO {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/realestate";

    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "postgres";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private Property mapRow(ResultSet rs) throws SQLException {
        Property p = new Property();
        p.id = rs.getInt("id");
        p.propertyID = rs.getString("property_id");
        p.downloadDate = rs.getString("download_date");
        p.councilName = rs.getString("council_name");
        p.purchasePrice = rs.getString("purchase_price");
        p.address = rs.getString("address");
        p.postcode = rs.getString("post_code");
        p.propertyType = rs.getString("property_type");
        p.strataLotNumber = rs.getString("strata_lot_number");
        p.propertyName = rs.getString("property_name");
        p.area = rs.getDouble("area");
        p.areaType = rs.getString("area_type");
        p.contractDate = rs.getString("contract_date");
        p.settlementDate = rs.getString("settlement_date");
        p.zoning = rs.getString("zoning");
        p.natureOfProperty = rs.getString("nature_of_property");
        p.primaryPurpose = rs.getString("primary_purpose");
        p.legalDescription = rs.getString("legal_description");
        return p;
    }

    public boolean newProperty(Property property) {
        String sql = "INSERT INTO properties (property_id, post_code, purchase_price) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(property.propertyID));
            stmt.setString(2, property.postcode);
            stmt.setLong(3, Long.parseLong(property.purchasePrice));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Property> getPropertyById(String propertyID) {
        String sql = "SELECT * FROM properties WHERE property_id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(propertyID));
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

    public List<Property> getPropertiesByPostCode(String postCode) {
        String sql = "SELECT * FROM properties WHERE post_code = ? LIMIT 100";
        List<Property> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, postCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public List<Property> getAllProperties() {
        String sql = "SELECT * FROM properties LIMIT 100";
        List<Property> results = new ArrayList<>();
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

    public List<Property> getPropertiesByPriceRange(long minPrice, long maxPrice) {
        String sql = "SELECT * FROM properties WHERE purchase_price >= ? AND purchase_price <= ? LIMIT 100";
        List<Property> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, minPrice);
            stmt.setLong(2, maxPrice);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public List<String> getAllPropertyPrices() {
        String sql = "SELECT purchase_price FROM properties LIMIT 100";
        List<String> prices = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                prices.add(String.valueOf(rs.getLong("purchase_price")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prices;
    }
}
