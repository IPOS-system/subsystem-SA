package dao;

import db.DBConnection;
import model.Order;
import model.Order.Status;
import model.OrderItem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO – manages orders and order items.
 * Covers IPOS-SA-ORD package functionality.
 */
public class OrderDAO {

    private final Connection conn;

    public OrderDAO() {
        this.conn = DBConnection.getInstance().getConnection();
    }


    /** Generates the next order ID in format "IP0001", "IP0002" etc. */
    public String generateOrderId() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1) + 1;
                return String.format("IP%04d", count);
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] generateOrderId error: " + e.getMessage());
        }
        return "IP0001";
    }



    public boolean createOrder(Order order) {
        String orderSql = """
            INSERT INTO orders
              (order_id, merchant_id, order_date, status,
               subtotal, discount_amount, total_amount)
            VALUES (?,?,?,?,?,?,?)
            """;
        String itemSql = """
            INSERT INTO order_items
              (order_id, product_id, quantity, unit_price, line_total)
            VALUES (?,?,?,?,?)
            """;
        try {
            conn.setAutoCommit(false);

            // Insert order header
            try (PreparedStatement ps = conn.prepareStatement(orderSql)) {
                ps.setString(1, order.getOrderId());
                ps.setInt(2, order.getMerchantId());
                ps.setTimestamp(3, Timestamp.valueOf(order.getOrderDate()));
                ps.setString(4, order.getStatus().name());
                ps.setDouble(5, order.getSubtotal());
                ps.setDouble(6, order.getDiscountAmount());
                ps.setDouble(7, order.getTotalAmount());
                ps.executeUpdate();
            }

            // Insert order items
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (OrderItem item : order.getItems()) {
                    ps.setString(1, order.getOrderId());
                    ps.setString(2, item.getProductId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setDouble(5, item.getLineTotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] createOrder error: " + e.getMessage());
            try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
        }
        return false;
    }


    public Order findById(String orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Order o = mapRow(rs);
                o.setItems(getItemsForOrder(orderId));
                return o;
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] findById error: " + e.getMessage());
        }
        return null;
    }

    public List<Order> findAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[OrderDAO] findAll error: " + e.getMessage());
        }
        return list;
    }

    public List<Order> findByMerchant(int merchantId) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE merchant_id = ? ORDER BY order_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[OrderDAO] findByMerchant error: " + e.getMessage());
        }
        return list;
    }

    public List<Order> findByMerchantAndPeriod(int merchantId,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        List<Order> list = new ArrayList<>();
        String sql = """
            SELECT * FROM orders
            WHERE merchant_id = ? AND order_date BETWEEN ? AND ?
            ORDER BY order_date DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, merchantId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[OrderDAO] findByMerchantAndPeriod error: " + e.getMessage());
        }
        return list;
    }

    private List<OrderItem> getItemsForOrder(String orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = """
            SELECT oi.*, c.description
            FROM order_items oi
            JOIN catalogue c ON oi.product_id = c.product_id
            WHERE oi.order_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                OrderItem item = new OrderItem(
                    orderId,
                    rs.getString("product_id"),
                    rs.getString("description"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price")
                );
                item.setItemId(rs.getInt("item_id"));
                item.setLineTotal(rs.getDouble("line_total"));
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] getItemsForOrder error: " + e.getMessage());
        }
        return items;
    }

    public boolean updateStatus(String orderId, Status newStatus) {
        String sql = "UPDATE orders SET status=? WHERE order_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] updateStatus error: " + e.getMessage());
        }
        return false;
    }

    public boolean recordDispatch(String orderId, String dispatchedBy,
                                   String courier, String courierRef,
                                   LocalDateTime expectedDelivery) {
        String sql = """
            UPDATE orders SET
              status='DISPATCHED', dispatched_by=?, dispatch_date=NOW(),
              courier=?, courier_ref=?, expected_delivery=?
            WHERE order_id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dispatchedBy);
            ps.setString(2, courier);
            ps.setString(3, courierRef);
            ps.setTimestamp(4, Timestamp.valueOf(expectedDelivery));
            ps.setString(5, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] recordDispatch error: " + e.getMessage());
        }
        return false;
    }


    private Order mapRow(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getString("order_id"));
        o.setMerchantId(rs.getInt("merchant_id"));
        o.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
        o.setStatus(Status.valueOf(rs.getString("status")));
        o.setSubtotal(rs.getDouble("subtotal"));
        o.setDiscountAmount(rs.getDouble("discount_amount"));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setDispatchedBy(rs.getString("dispatched_by"));
        Timestamp dispatchTs = rs.getTimestamp("dispatch_date");
        if (dispatchTs != null) o.setDispatchDate(dispatchTs.toLocalDateTime());
        o.setCourier(rs.getString("courier"));
        o.setCourierRef(rs.getString("courier_ref"));
        Timestamp deliveryTs = rs.getTimestamp("expected_delivery");
        if (deliveryTs != null) o.setExpectedDelivery(deliveryTs.toLocalDateTime());
        return o;
    }
}
