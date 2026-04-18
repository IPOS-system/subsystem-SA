package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * A single line on an Order, linking a CatalogueItem with its ordered quantity and pricing.
 * line_total is stored to preserve the invoiced price even if the catalogue
 * unit_price changes after the order is placed: line_total = quantity * unit_price (at
 * time of order placement).
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Integer itemId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private CatalogueItem catalogueItem;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  /** Unit price captured at order time. */
  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  /** quantity * unit_price */
  @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal lineTotal;
}
