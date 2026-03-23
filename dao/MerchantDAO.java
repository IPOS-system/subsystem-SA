package dao;

import db.DBConnection;
import model.Merchant;
import model.Merchant.AccountStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class MerchantDAO {

    private final Connection conn;

    public MerchantDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }

    public int createMerchant(Merchant m) {
        String sql = """
            INSERT INTO merchants
              (user_id, company_name, address, phone, fax, email,
               credit_limit, current_balance, account_status, discount_plan_id)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getUserId());
            ps.setString(2, m.getCompanyName());
            ps.setString(3, m.getAddress());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getFax());
            ps.setString(6, m.getEmail());
            ps.setDouble(7, m.getCreditLimit());
            ps.setDouble(8, 0.00);
            ps.setString(9, m.getAccountStatus().name());
            if (m.getDiscountPlanId() != null)
                ps.setInt(10, m.getDiscountPlanId());
            else
                ps.setNull(10, Types.INTEGER);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] createMerchant error: " + e.getMessage());
        }
        return -1;
    }


    public Merchant findById(int merchantId) {
        String sql = "SELECT * FROM merchants WHERE merchant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    public Merchant findByUserId(int userId) {
        String sql = "SELECT * FROM merchants WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] findByUserId error: " + e.getMessage());
        }
        return null;
    }

    public List<Merchant> findAll() {
        List<Merchant> list = new ArrayList<>();
        String sql = "SELECT * FROM merchants ORDER BY company_name";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] findAll error: " + e.getMessage());
        }
        return list;
    }


    public boolean updateMerchant(Merchant m) {
        String sql = """
            UPDATE merchants SET
              company_name=?, address=?, phone=?, fax=?, email=?,
              credit_limit=?, discount_plan_id=?
            WHERE merchant_id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getCompanyName());
            ps.setString(2, m.getAddress());
            ps.setString(3, m.getPhone());
            ps.setString(4, m.getFax());
            ps.setString(5, m.getEmail());
            ps.setDouble(6, m.getCreditLimit());
            if (m.getDiscountPlanId() != null)
                ps.setInt(7, m.getDiscountPlanId());
            else
                ps.setNull(7, Types.INTEGER);
            ps.setInt(8, m.getMerchantId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] updateMerchant error: " + e.getMessage());
        }
        return false;
    }

    public boolean updateAccountStatus(int merchantId, AccountStatus status) {
        String sql = "UPDATE merchants SET account_status=? WHERE merchant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, merchantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] updateAccountStatus error: " + e.getMessage());
        }
        return false;
    }

    public boolean updateBalance(int merchantId, double newBalance) {
        String sql = "UPDATE merchants SET current_balance=? WHERE merchant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, merchantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] updateBalance error: " + e.getMessage());
        }
        return false;
    }

    public boolean setCreditLimit(int merchantId, double limit) {
        String sql = "UPDATE merchants SET credit_limit=? WHERE merchant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, limit);
            ps.setInt(2, merchantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] setCreditLimit error: " + e.getMessage());
        }
        return false;
    }


    public boolean deleteMerchant(int merchantId) {
        String sql = "DELETE FROM merchants WHERE merchant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MerchantDAO] deleteMerchant error: " + e.getMessage());
        }
        return false;
    }



    public boolean wouldExceedCreditLimit(int merchantId, double orderAmount) {
        Merchant m = findById(merchantId);
        if (m == null) return true;
        return (m.getCurrentBalance() + orderAmount) > m.getCreditLimit();
    }


    private Merchant mapRow(ResultSet rs) throws SQLException {
        int planId = rs.getInt("discount_plan_id");
        Integer planIdObj = rs.wasNull() ? null : planId;
        return new Merchant(
            rs.getInt("merchant_id"),
            rs.getInt("user_id"),
            rs.getString("company_name"),
            rs.getString("address"),
            rs.getString("phone"),
            rs.getString("fax"),
            rs.getString("email"),
            rs.getDouble("credit_limit"),
            rs.getDouble("current_balance"),
            AccountStatus.valueOf(rs.getString("account_status")),
            planIdObj
        );
    }
}
