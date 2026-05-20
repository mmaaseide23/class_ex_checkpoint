package purchaser;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PurchaserDAO extends BaseDAO {

    public Purchaser createPurchaser(Purchaser p) {
        String sql = "INSERT INTO users (first_name, last_name, email, phone) VALUES (?, ?, ?, ?) RETURNING id, created_date";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.firstName);
            stmt.setString(2, p.lastName);
            stmt.setString(3, p.email);
            stmt.setString(4, p.phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                p.id = rs.getInt("id");
                p.createdDate = rs.getDate("created_date").toLocalDate();
            }
            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<Purchaser> getPurchaserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapPurchaser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public PurchaserPreference addPreference(int purchaserId, PurchaserPreference pref) {
        String sql = "INSERT INTO user_preferences (user_id, preference_type, preference_value) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, purchaserId);
            stmt.setString(2, pref.preferenceType);
            stmt.setString(3, pref.preferenceValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                pref.id = rs.getInt("id");
                pref.purchaserId = purchaserId;
            }
            return pref;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PurchaserPreference> getPreferences(int purchaserId) {
        String sql = "SELECT * FROM user_preferences WHERE user_id = ?";
        List<PurchaserPreference> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, purchaserId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapPreference(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public boolean deletePreference(int prefId) {
        String sql = "DELETE FROM user_preferences WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prefId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int countPreferencesByType(int purchaserId, String type) {
        String sql = "SELECT COUNT(*) FROM user_preferences WHERE user_id = ? AND preference_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, purchaserId);
            stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<PurchaserWithPreferences> getPurchasersWithPostcodePreferences() {
        String sql =
            "SELECT u.id, u.first_name, u.last_name, u.email, up.preference_value " +
            "FROM users u " +
            "JOIN user_preferences up " +
            "  ON up.user_id = u.id AND up.preference_type = 'postcode' " +
            "ORDER BY u.id";

        Map<Integer, PurchaserWithPreferences> byId = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int uid = rs.getInt("id");
                PurchaserWithPreferences row = byId.get(uid);
                if (row == null) {
                    row = new PurchaserWithPreferences();
                    row.id = uid;
                    row.firstName = rs.getString("first_name");
                    row.lastName = rs.getString("last_name");
                    row.email = rs.getString("email");
                    byId.put(uid, row);
                }
                row.postcodes.add(rs.getString("preference_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(byId.values());
    }

    private PurchaserPreference mapPreference(ResultSet rs) throws SQLException {
        PurchaserPreference p = new PurchaserPreference();
        p.id = rs.getInt("id");
        p.purchaserId = rs.getInt("user_id");
        p.preferenceType = rs.getString("preference_type");
        p.preferenceValue = rs.getString("preference_value");
        return p;
    }

    private Purchaser mapPurchaser(ResultSet rs) throws SQLException {
        Purchaser p = new Purchaser();
        p.id = rs.getInt("id");
        p.firstName = rs.getString("first_name");
        p.lastName = rs.getString("last_name");
        p.email = rs.getString("email");
        p.phone = rs.getString("phone");
        if (rs.getDate("created_date") != null) {
            p.createdDate = rs.getDate("created_date").toLocalDate();
        }
        return p;
    }
}