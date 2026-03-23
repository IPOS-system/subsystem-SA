package dao;

import db.DBConnection;
import java.sql.*;

public class AuditDAO {

    private final Connection conn;

    public AuditDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public void log(int userId, String action, String targetType,
                    String targetId, String details) {
        String sql = """
            INSERT INTO audit_log (user_id, action, target_type, target_id, details)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, targetType);
            ps.setString(4, targetId);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AuditDAO] log error: " + e.getMessage());
        }
    }
}
