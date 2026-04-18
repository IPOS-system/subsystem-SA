package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.catalogue.*;
import com.ipos.ipos_sa.entity.AuditLog;
import com.ipos.ipos_sa.entity.CatalogueItem;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.AuditLogRepository;
import com.ipos.ipos_sa.repository.CatalogueItemRepository;
import com.ipos.ipos_sa.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogueService {

  private final CatalogueItemRepository catalogueItemRepository;
  private final OrderItemRepository orderItemRepository;
  private final AuditLogRepository auditLogRepository;

  // Merchant-facing

  /**
   * Returns all active catalogue items visible to merchants.
   */
  public List<CatalogueItemDTO> getActiveCatalogue() {
    return catalogueItemRepository.findByIsActiveTrue().stream()
        .map(this::toMerchantDTO)
        .collect(Collectors.toList());
  }

  /**
   * Keyword search across active products.
   */
  public List<CatalogueItemDTO> searchCatalogue(String term) {
    return catalogueItemRepository.searchActive(term).stream()
        .map(this::toMerchantDTO)
        .collect(Collectors.toList());
  }

  // Admin-facing

  /**
   * Returns all catalogue items including inactive ones, with internal stock management fields.
   */
  public List<AdminCatalogueItemDTO> getAllCatalogueAdmin() {
    return catalogueItemRepository.findAll().stream()
        .map(this::toAdminDTO)
        .collect(Collectors.toList());
  }

  /** Returns a single product by ID with full admin detail. */
  public AdminCatalogueItemDTO getProductAdmin(String productId) {
    CatalogueItem item =
        catalogueItemRepository
            .findById(normalizeProductId(productId))
            .orElseThrow(() -> new ResourceNotFoundException("CatalogueItem", productId));
    return toAdminDTO(item);
  }

  // Add Item

  /**
   * Adds a new product to the catalogue. Product ID must be unique. 
   */
  @Transactional
  public AdminCatalogueItemDTO addProduct(CreateProductRequest request, User actingUser) {

    if (catalogueItemRepository.existsById(request.getProductId())) {
      throw new ValidationException("Product ID already exists: " + request.getProductId());
    }

    BigDecimal bufferPct =
        request.getReorderBufferPct() != null
            ? request.getReorderBufferPct()
            : new BigDecimal("10.00");

    CatalogueItem item =
        CatalogueItem.builder()
            .productId(request.getProductId())
            .description(request.getDescription())
            .packageType(request.getPackageType())
            .unit(request.getUnit())
            .unitsPerPack(request.getUnitsPerPack())
            .unitPrice(request.getUnitPrice())
            .availability(request.getAvailability())
            .minStockLevel(request.getMinStockLevel())
            .reorderBufferPct(bufferPct)
            .isActive(true)
            .build();

    catalogueItemRepository.save(item);

    audit(
        actingUser,
        "ADD_CATALOGUE_ITEM",
        "catalogue",
        request.getProductId(),
        "Added product: " + request.getDescription());

    log.info("Product added: {}", request.getProductId());
    return toAdminDTO(item);
  }

  // Update Item

  /** Partially updates a product's details. */
  @Transactional
  public AdminCatalogueItemDTO updateProduct(
      String productId, UpdateProductRequest request, User actingUser) {

    CatalogueItem item =
        catalogueItemRepository
            .findById(normalizeProductId(productId))
            .orElseThrow(() -> new ResourceNotFoundException("CatalogueItem", productId));

    if (request.getDescription() != null) item.setDescription(request.getDescription());
    if (request.getPackageType() != null) item.setPackageType(request.getPackageType());
    if (request.getUnit() != null) item.setUnit(request.getUnit());
    if (request.getUnitsPerPack() != null) item.setUnitsPerPack(request.getUnitsPerPack());
    if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
    if (request.getMinStockLevel() != null) item.setMinStockLevel(request.getMinStockLevel());
    if (request.getReorderBufferPct() != null)
      item.setReorderBufferPct(request.getReorderBufferPct());

    catalogueItemRepository.save(item);

    audit(
        actingUser,
        "UPDATE_CATALOGUE_ITEM",
        "catalogue",
        productId,
        "Updated product: " + item.getDescription());

    return toAdminDTO(item);
  }

  // Delete Item / Soft-Delete

  /**
   * Soft-deletes a product by setting isActive = false.
   */
  @Transactional
  public void deactivateProduct(String productId, User actingUser) {

    CatalogueItem item =
        catalogueItemRepository
            .findById(normalizeProductId(productId))
            .orElseThrow(() -> new ResourceNotFoundException("CatalogueItem", productId));

    item.setIsActive(false);
    catalogueItemRepository.save(item);

    audit(
        actingUser,
        "DEACTIVATE_CATALOGUE_ITEM",
        "catalogue",
        productId,
        "Deactivated product: " + item.getDescription());

    log.info("Product deactivated: {}", productId);
  }

  /** Re-activates a previously deactivated product. */
  @Transactional
  public void reactivateProduct(String productId, User actingUser) {

    CatalogueItem item =
        catalogueItemRepository
            .findById(normalizeProductId(productId))
            .orElseThrow(() -> new ResourceNotFoundException("CatalogueItem", productId));

    item.setIsActive(true);
    catalogueItemRepository.save(item);

    audit(
        actingUser,
        "REACTIVATE_CATALOGUE_ITEM",
        "catalogue",
        productId,
        "Reactivated product: " + item.getDescription());

    log.info("Product reactivated: {}", productId);
  }

  // Stock Delivery / Renew Stock

  /**
   * Records a stock delivery, increasing the product's availability.
   */
  @Transactional
  public AdminCatalogueItemDTO addStock(
      String productId, StockDeliveryRequest request, User actingUser) {

    CatalogueItem item =
        catalogueItemRepository
            .findById(normalizeProductId(productId))
            .orElseThrow(() -> new ResourceNotFoundException("CatalogueItem", productId));

    int oldAvailability = item.getAvailability();
    item.setAvailability(oldAvailability + request.getQuantity());
    catalogueItemRepository.save(item);

    audit(
        actingUser,
        "STOCK_DELIVERY",
        "catalogue",
        productId,
        "Stock delivery: +"
            + request.getQuantity()
            + " (was "
            + oldAvailability
            + ", now "
            + item.getAvailability()
            + ")");

    log.info("Stock delivery: {} += {}", productId, request.getQuantity());
    return toAdminDTO(item);
  }

  // Low Stock Check

  /**
   * Returns all active products currently below their minimum stock level.
   */
  public List<CatalogueItem> getLowStockProducts() {
    return catalogueItemRepository.findLowStockProducts();
  }

  // Mappers

  private CatalogueItemDTO toMerchantDTO(CatalogueItem item) {
    return CatalogueItemDTO.builder()
        .productId(item.getProductId())
        .description(item.getDescription())
        .packageType(item.getPackageType())
        .unit(item.getUnit())
        .unitsPerPack(item.getUnitsPerPack())
        .unitPrice(item.getUnitPrice())
        .availability(item.getAvailability())
        .build();
  }

  private AdminCatalogueItemDTO toAdminDTO(CatalogueItem item) {
    return AdminCatalogueItemDTO.builder()
        .productId(item.getProductId())
        .description(item.getDescription())
        .packageType(item.getPackageType())
        .unit(item.getUnit())
        .unitsPerPack(item.getUnitsPerPack())
        .unitPrice(item.getUnitPrice())
        .availability(item.getAvailability())
        .isActive(item.getIsActive())
        .minStockLevel(item.getMinStockLevel())
        .reorderBufferPct(item.getReorderBufferPct())
        .createdAt(item.getCreatedAt())
        .updatedAt(item.getUpdatedAt())
        .build();
  }

  // Product ID Normalizer

  /**
   * Normalizes a product ID by replacing hyphens with spaces.
   */
  private String normalizeProductId(String productId) {
    return productId != null ? productId.replace("-", " ") : productId;
  }

  // Audit Helper

  private void audit(
      User actor, String action, String targetType, String targetId, String details) {
    AuditLog entry =
        AuditLog.builder()
            .user(actor)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .details(details)
            .build();
    auditLogRepository.save(entry);
  }
}
