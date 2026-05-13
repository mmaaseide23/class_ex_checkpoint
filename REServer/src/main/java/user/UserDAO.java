package user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/realestate";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public User createUser(User user) {
        String sql = "INSERT INTO users (name, email) VALUES (?, ?) RETURNING id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.name);
            stmt.setString(2, user.email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user.id = rs.getInt("id");
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
                User u = new User();
                u.id = rs.getInt("id");
                u.name = rs.getString("name");
                u.email = rs.getString("email");
                return Optional.of(u);
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
}
