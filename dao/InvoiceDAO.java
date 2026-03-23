package dao;

import db.DBConnection;
import model.Invoice;
import model.Invoice.PaymentStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private final Connection conn;

    public InvoiceDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public String generateInvoiceId() {
        String sql = "SELECT COUNT(*) FROM invoices WHERE YEAR(invoice_date) = YEAR(NOW())";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1) + 1;
                int year  = LocalDate.now().getYear();
                return String.format("INV-%d-%05d", year, count);
            }
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] generateInvoiceId error: " + e.getMessage());
        }
        return "INV-2026-00001";
    }

    public boolean createInvoice(Invoice inv) {
        String sql = "INSERT INTO invoices (invoice_id, order_id, merchant_id, invoice_date, amount_due, payment_status, due_date) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inv.getInvoiceId());
            ps.setString(2, inv.getOrderId());
            ps.setInt(3, inv.getMerchantId());
            ps.setTimestamp(4, Timestamp.valueOf(inv.getInvoiceDate()));
            ps.setDouble(5, inv.getAmountDue());
            ps.setString(6, inv.getPaymentStatus().name());
            ps.setDate(7, Date.valueOf(inv.getDueDate()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] createInvoice error: " + e.getMessage());
        }
        return false;
    }

    public Invoice findById(String invoiceId) {
        String sql = "SELECT * FROM invoices WHERE invoice_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    public Invoice findByOrderId(String orderId) {
        String sql = "SELECT * FROM invoices WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] findByOrderId error: " + e.getMessage());
        }
        return null;
    }

    public List<Invoice> findByMerchant(int merchantId) {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE merchant_id = ? ORDER BY invoice_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] findByMerchant error: " + e.getMessage());
        }
        return list;
    }

    public List<Invoice> findAll() {
        List<Invoice> list = new ArrayList<>();
        String sql = "SELECT * FROM invoices ORDER BY invoice_date DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    public boolean updatePaymentStatus(String invoiceId, PaymentStatus status) {
        String sql = "UPDATE invoices SET payment_status=? WHERE invoice_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, invoiceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[InvoiceDAO] updatePaymentStatus error: " + e.getMessage());
        }
        return false;
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setInvoiceId(rs.getString("invoice_id"));
        inv.setOrderId(rs.getString("order_id"));
        inv.setMerchantId(rs.getInt("merchant_id"));
        inv.setInvoiceDate(rs.getTimestamp("invoice_date").toLocalDateTime());
        inv.setAmountDue(rs.getDouble("amount_due"));
        inv.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        inv.setDueDate(rs.getDate("due_date").toLocalDate());
        return inv;
    }
}
