package analytics;

import app.BaseDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccessCountDAO extends BaseDAO {

    public void increment(String accessType, String accessValue) {
        String sql =
            "INSERT INTO access_counts (access_type, access_value, count, last_accessed) " +
            "VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (access_type, access_value) " +
            "DO UPDATE SET count = access_counts.count + 1, last_accessed = CURRENT_TIMESTAMP";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accessType);
            stmt.setString(2, accessValue);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AccessCount> getTopByType(String accessType, int limit) {
        String sql = "SELECT * FROM access_counts WHERE access_type = ? ORDER BY count DESC LIMIT ?";
        List<AccessCount> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accessType);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public List<AccessCount> getAll() {
        String sql = "SELECT * FROM access_counts ORDER BY count DESC";
        List<AccessCount> results = new ArrayList<>();
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

    private AccessCount mapRow(ResultSet rs) throws SQLException {
        AccessCount ac = new AccessCount();
        ac.id = rs.getInt("id");
        ac.accessType = rs.getString("access_type");
        ac.accessValue = rs.getString("access_value");
        ac.count = rs.getInt("count");
        if (rs.getTimestamp("last_accessed") != null) {
            ac.lastAccessed = rs.getTimestamp("last_accessed").toLocalDateTime();
        }
        return ac;
    }
}