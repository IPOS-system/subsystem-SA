package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, String> {

  /** My Orders screen: all orders for a specific merchant, newest first. */
  List<Order> findByMerchant_MerchantIdOrderByOrderDateDesc(Integer merchantId);

  /** Admin orders management: filter by status. */
  List<Order> findByStatus(Order.OrderStatus status);

  /** RPT-02 / RPT-03: merchant order history within a date range. */
  List<Order> findByMerchant_MerchantIdAndOrderDateBetween(
      Integer merchantId, LocalDateTime from, LocalDateTime to);

  /**
   * Returns all orders that are not yet completed (not DELIVERED, not CANCELLED). Per the marking
   * sheet: "Observing the list of orders taken but not completed" (1 mark).
   */
  List<Order> findByStatusNotIn(List<Order.OrderStatus> statuses);

  /**
   * RPT-01 (Turnover): total revenue received in a date range. Sums total_amount across all
   * non-cancelled orders in the period.
   */
  @Query(
      "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o "
          + "WHERE o.orderDate BETWEEN :from AND :to AND o.status <> 'CANCELLED'")
  BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /**
   * RPT-01 (Turnover): quantity sold per product in a date range. Returns Object[] rows of
   * [productId, description, totalQty, totalRevenue].
   */
  @Query(
      "SELECT oi.catalogueItem.productId, oi.catalogueItem.description, "
          + "       SUM(oi.quantity), SUM(oi.lineTotal) "
          + "FROM OrderItem oi "
          + "WHERE oi.order.orderDate BETWEEN :from AND :to "
          + "  AND oi.order.status <> 'CANCELLED' "
          + "GROUP BY oi.catalogueItem.productId, oi.catalogueItem.description")
  List<Object[]> findProductSaleTotalsBetween(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /** All orders within a date range regardless of merchant — used by admin reports. */
  List<Order> findByOrderDateBetween(LocalDateTime from, LocalDateTime to);
}
