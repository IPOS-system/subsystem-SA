package com.ipos.ipos_sa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a product in the InfoPharma electronic catalogue (IPOS-SA-CAT).
 *
 * <p>Stock management rules: - availability is decremented when an order is accepted. - When
 * availability drops below min_stock_level, a low-stock warning is shown to ADMIN and MANAGER users
 * and the item appears in the Low Stock Level Report (Appendix 3).
 */
@Entity
@Table(name = "catalogue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogueItem {

  @Id
  @Column(name = "product_id", length = 20)
  private String productId;

  @Column(name = "description", nullable = false, length = 200)
  private String description;

  @Column(name = "package_type", length = 50)
  private String packageType;

  @Column(name = "unit", length = 20)
  private String unit;

  @Column(name = "units_per_pack", nullable = false)
  private Integer unitsPerPack;

  /** Price per unit charged to merchants at time of ordering. */
  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  /** Current number of packs/units in InfoPharma's warehouse. */
  @Column(name = "availability", nullable = false)
  private Integer availability;

  /**
   * Minimum stock level threshold. When current_availability < min_stock_level the item is flagged
   * as low-stock. The recommended reorder quantity brings availability to (min_stock_level * (1 +
   * reorder_buffer_pct)).
   */
  @Column(name = "min_stock_level", nullable = false)
  private Integer minStockLevel;

  @Column(name = "reorder_buffer_pct", nullable = false, precision = 5, scale = 2)
  private BigDecimal reorderBufferPct;

  /** Whether the item is visible to merchants and available to order. */
  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "catalogueItem", fetch = FetchType.LAZY)
  private List<OrderItem> orderItems;
}
