package dao;

import db.DBConnection;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class CatalogueDAO {

    private final Connection conn;

    public CatalogueDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }


    public boolean addProduct(Product p) {
        String sql = """
            INSERT INTO catalogue
              (product_id, description, package_type, unit, units_per_pack,
               unit_price, availability, min_stock_level, reorder_buffer_pct)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductId());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getPackageType());
            ps.setString(4, p.getUnit());
            ps.setInt(5, p.getUnitsPerPack());
            ps.setDouble(6, p.getUnitPrice());
            ps.setInt(7, p.getAvailability());
            ps.setInt(8, p.getMinStockLevel());
            ps.setDouble(9, p.getReorderBufferPct());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] addProduct error: " + e.getMessage());
        }
        return false;
    }


    public Product findById(String productId) {
        String sql = "SELECT * FROM catalogue WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    public List<Product> findAllActive() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM catalogue WHERE is_active = TRUE ORDER BY product_id";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] findAllActive error: " + e.getMessage());
        }
        return list;
    }

    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT * FROM catalogue
            WHERE is_active = TRUE
              AND (description LIKE ? OR product_id LIKE ?)
            ORDER BY product_id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] search error: " + e.getMessage());
        }
        return list;
    }

    public List<Product> getLowStockItems() {
        List<Product> list = new ArrayList<>();
        String sql = """
            SELECT * FROM catalogue
            WHERE is_active = TRUE AND availability < min_stock_level
            ORDER BY product_id
            """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] getLowStockItems error: " + e.getMessage());
        }
        return list;
    }


    public boolean updateProduct(Product p) {
        String sql = """
            UPDATE catalogue SET
              description=?, package_type=?, unit=?, units_per_pack=?,
              unit_price=?, min_stock_level=?, reorder_buffer_pct=?
            WHERE product_id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getDescription());
            ps.setString(2, p.getPackageType());
            ps.setString(3, p.getUnit());
            ps.setInt(4, p.getUnitsPerPack());
            ps.setDouble(5, p.getUnitPrice());
            ps.setInt(6, p.getMinStockLevel());
            ps.setDouble(7, p.getReorderBufferPct());
            ps.setString(8, p.getProductId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] updateProduct error: " + e.getMessage());
        }
        return false;
    }

    public boolean addStock(String productId, int quantityToAdd) {
        String sql = """
            UPDATE catalogue
            SET availability = availability + ?
            WHERE product_id = ? AND is_active = TRUE
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantityToAdd);
            ps.setString(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] addStock error: " + e.getMessage());
        }
        return false;
    }


    public boolean reduceStock(String productId, int quantity) {
        String sql = """
            UPDATE catalogue
            SET availability = availability - ?
            WHERE product_id = ? AND availability >= ? AND is_active = TRUE
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            ps.setInt(3, quantity);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.err.println("[CatalogueDAO] Insufficient stock for: " + productId);
                return false;
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] reduceStock error: " + e.getMessage());
        }
        return false;
    }

    public boolean deactivateProduct(String productId) {
        String sql = "UPDATE catalogue SET is_active = FALSE WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CatalogueDAO] deactivateProduct error: " + e.getMessage());
        }
        return false;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product(
            rs.getString("product_id"),
            rs.getString("description"),
            rs.getString("package_type"),
            rs.getString("unit"),
            rs.getInt("units_per_pack"),
            rs.getDouble("unit_price"),
            rs.getInt("availability"),
            rs.getInt("min_stock_level"),
            rs.getDouble("reorder_buffer_pct")
        );
        p.setActive(rs.getBoolean("is_active"));
        return p;
    }
}
