package dao;

import db.DBConnection;
import model.Payment;
import model.Payment.Method;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private final Connection conn;

    public PaymentDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public boolean recordPayment(Payment p) {
        String sql = "INSERT INTO payments (merchant_id, invoice_id, amount_paid, payment_method, payment_date, recorded_by, notes) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getMerchantId());
            ps.setString(2, p.getInvoiceId());
            ps.setDouble(3, p.getAmountPaid());
            ps.setString(4, p.getPaymentMethod().name());
            ps.setTimestamp(5, Timestamp.valueOf(p.getPaymentDate()));
            ps.setInt(6, p.getRecordedBy());
            ps.setString(7, p.getNotes());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) p.setPaymentId(keys.getInt(1));
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] recordPayment error: " + e.getMessage());
        }
        return false;
    }

    public List<Payment> findByMerchant(int merchantId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE merchant_id = ? ORDER BY payment_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] findByMerchant error: " + e.getMessage());
        }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setMerchantId(rs.getInt("merchant_id"));
        p.setInvoiceId(rs.getString("invoice_id"));
        p.setAmountPaid(rs.getDouble("amount_paid"));
        p.setPaymentMethod(Method.valueOf(rs.getString("payment_method")));
        p.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
        p.setRecordedBy(rs.getInt("recorded_by"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
