package sale;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SaleDAO extends BaseDAO {

    private Sale mapRow(ResultSet rs) throws SQLException {
        Sale s = new Sale();
        s.id = rs.getInt("id");
        s.propertyId = rs.getLong("property_id");
        s.downloadDate = rs.getDate("download_date") != null ? rs.getDate("download_date").toLocalDate() : null;
        s.councilName = rs.getString("council_name");
        s.purchasePrice = rs.getLong("purchase_price");
        s.address = rs.getString("address");
        s.postCode = rs.getString("post_code");
        s.propertyType = rs.getString("property_type");
        s.strataLotNumber = rs.getString("strata_lot_number");
        s.propertyName = rs.getString("property_name");
        s.area = rs.getDouble("area");
        s.areaType = rs.getString("area_type");
        s.contractDate = rs.getDate("contract_date") != null ? rs.getDate("contract_date").toLocalDate() : null;
        s.settlementDate = rs.getDate("settlement_date") != null ? rs.getDate("settlement_date").toLocalDate() : null;
        s.zoning = rs.getString("zoning");
        s.natureOfProperty = rs.getString("nature_of_property");
        s.primaryPurpose = rs.getString("primary_purpose");
        s.legalDescription = rs.getString("legal_description");
        return s;
    }

    public boolean newSale(Sale sale) {
        String sql = "INSERT INTO sales (property_id, download_date, council_name, purchase_price, "
                + "address, post_code, property_type, strata_lot_number, property_name, area, "
                + "area_type, contract_date, settlement_date, zoning, nature_of_property, "
                + "primary_purpose, legal_description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sale.propertyId);
            stmt.setDate(2, sale.downloadDate != null ? java.sql.Date.valueOf(sale.downloadDate) : null);
            stmt.setString(3, sale.councilName);
            stmt.setLong(4, sale.purchasePrice);
            stmt.setString(5, sale.address);
            stmt.setString(6, sale.postCode);
            stmt.setString(7, sale.propertyType);
            stmt.setString(8, sale.strataLotNumber);
            stmt.setString(9, sale.propertyName);
            stmt.setDouble(10, sale.area);
            stmt.setString(11, sale.areaType);
            stmt.setDate(12, sale.contractDate != null ? java.sql.Date.valueOf(sale.contractDate) : null);
            stmt.setDate(13, sale.settlementDate != null ? java.sql.Date.valueOf(sale.settlementDate) : null);
            stmt.setString(14, sale.zoning);
            stmt.setString(15, sale.natureOfProperty);
            stmt.setString(16, sale.primaryPurpose);
            stmt.setString(17, sale.legalDescription);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Optional<Sale> getSaleByPropertyId(String propertyId) {
        String sql = "SELECT * FROM sales WHERE property_id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, Long.parseLong(propertyId));
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

    public List<Sale> getSalesByPostCode(String postCode) {
        String sql = "SELECT * FROM sales WHERE post_code = ? LIMIT 100";
        List<Sale> results = new ArrayList<>();
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

    public List<Sale> getAllSales() {
        String sql = "SELECT * FROM sales LIMIT 100";
        List<Sale> results = new ArrayList<>();
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

    public List<Sale> getSalesByPriceRange(long minPrice, long maxPrice) {
        String sql = "SELECT * FROM sales WHERE purchase_price >= ? AND purchase_price <= ? LIMIT 100";
        List<Sale> results = new ArrayList<>();
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
}