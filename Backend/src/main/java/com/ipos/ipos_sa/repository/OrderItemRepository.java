package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

  /** Fetch all line items for a single order. */
  List<OrderItem> findByOrder_OrderId(String orderId);

  /** Fetch line items for a batch of orders. */
  List<OrderItem> findByOrder_OrderIdIn(List<String> orderIds);

  /**
   * Check whether any order item references a given product. Used before soft-deleting a catalogue
   * item to confirm it has no order history.
   */
  boolean existsByCatalogueItem_ProductId(String productId);
}
