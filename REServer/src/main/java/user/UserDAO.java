package user;

import app.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getConnection();
    }

    public User createUser(User user) {
        String sql = "INSERT INTO users (first_name, last_name, email, phone) VALUES (?, ?, ?, ?) RETURNING id, created_date";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.firstName);
            stmt.setString(2, user.lastName);
            stmt.setString(3, user.email);
            stmt.setString(4, user.phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.id = rs.getInt("id");
                user.createdDate = rs.getDate("created_date").toLocalDate();
            }
            return user;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<User> getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapUser(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public UserPreference addPreference(int userId, UserPreference pref) {
        String sql = "INSERT INTO user_preferences (user_id, preference_type, preference_value) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, pref.preferenceType);
            stmt.setString(3, pref.preferenceValue);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                pref.id = rs.getInt("id");
                pref.userId = userId;
            }
            return pref;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<UserPreference> getPreferences(int userId) {
        String sql = "SELECT * FROM user_preferences WHERE user_id = ?";
        List<UserPreference> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserPreference p = new UserPreference();
                p.id = rs.getInt("id");
                p.userId = rs.getInt("user_id");
                p.preferenceType = rs.getString("preference_type");
                p.preferenceValue = rs.getString("preference_value");
                results.add(p);
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

    public int countPreferencesByType(int userId, String type) {
        String sql = "SELECT COUNT(*) FROM user_preferences WHERE user_id = ? AND preference_type = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public List<UserNotification> getNotificationsForAllUsers() {
        String sql =
            "SELECT u.id, u.first_name, u.last_name, u.email, " +
            "       pl.property_id, pl.price " +
            "FROM users u " +
            "JOIN user_preferences up " +
            "  ON up.user_id = u.id AND up.preference_type = 'postcode' " +
            "JOIN properties p " +
            "  ON p.post_code = up.preference_value " +
            "JOIN property_listings pl " +
            "  ON pl.property_id = p.property_id " +
            "ORDER BY u.id, pl.property_id";

        Map<Integer, UserNotification> byUser = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int uid = rs.getInt("id");
                UserNotification n = byUser.get(uid);
                if (n == null) {
                    n = new UserNotification();
                    n.userId = uid;
                    n.firstName = rs.getString("first_name");
                    n.lastName = rs.getString("last_name");
                    n.email = rs.getString("email");
                    byUser.put(uid, n);
                }
                UserNotification.MatchingListing ml = new UserNotification.MatchingListing();
                ml.propertyId = rs.getLong("property_id");
                ml.price = rs.getLong("price");
                n.listings.add(ml);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(byUser.values());
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.id = rs.getInt("id");
        u.firstName = rs.getString("first_name");
        u.lastName = rs.getString("last_name");
        u.email = rs.getString("email");
        u.phone = rs.getString("phone");
        if (rs.getDate("created_date") != null) {
            u.createdDate = rs.getDate("created_date").toLocalDate();
        }
        return u;
    }
}
