package com.ipos.ipos_sa.repository;

import com.ipos.ipos_sa.entity.CatalogueItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogueItemRepository extends JpaRepository<CatalogueItem, String> {

  /** Merchant-facing catalogue: active products only. */
  List<CatalogueItem> findByIsActiveTrue();

  /** Admin-facing: all products including soft-deleted/inactive. */
  List<CatalogueItem> findAll();

  /**
   * Keyword search on active products
   */
  @Query(
      "SELECT c FROM CatalogueItem c WHERE c.isActive = true AND ("
          + "LOWER(c.description) LIKE LOWER(CONCAT('%', :term, '%')) OR "
          + "LOWER(c.productId)   LIKE LOWER(CONCAT('%', :term, '%')))")
  List<CatalogueItem> searchActive(@Param("term") String term);

  /**
   * Returns all active products where current availability is below their configured minimum stock
   * level.
   */
  @Query(
      "SELECT c FROM CatalogueItem c WHERE c.isActive = true AND c.availability < c.minStockLevel")
  List<CatalogueItem> findLowStockProducts();
}
