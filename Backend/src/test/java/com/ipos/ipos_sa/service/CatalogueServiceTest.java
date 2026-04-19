package com.ipos.ipos_sa.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ipos.ipos_sa.dto.catalogue.*;
import com.ipos.ipos_sa.entity.CatalogueItem;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.exception.ValidationException;
import com.ipos.ipos_sa.repository.AuditLogRepository;
import com.ipos.ipos_sa.repository.CatalogueItemRepository;
import com.ipos.ipos_sa.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogueServiceTest {

  @Mock CatalogueItemRepository catalogueItemRepository;
  @Mock OrderItemRepository orderItemRepository;
  @Mock AuditLogRepository auditLogRepository;

  @InjectMocks CatalogueService catalogueService;

  User admin;

  @BeforeEach
  void setup() {
    admin = User.builder().userId(1).username("sysdba").role(User.Role.ADMIN).isActive(true).build();}
  CatalogueItem paracetamol() {
    return CatalogueItem.builder()
        .productId("100 00001").description("Paracetamol")
        .packageType("box").unit("caps")
        .unitsPerPack(20).unitPrice(new BigDecimal("0.10"))
        .availability(10_000).minStockLevel(300)
        .reorderBufferPct(new BigDecimal("10")).isActive(true).build();}
  @Test
  void addProduct_valid_createsWithDefaultBuffer() {
    CreateProductRequest r = new CreateProductRequest();
    r.setProductId("300 00003");
    
    
    r.setDescription("New Med");
    
    
    
    r.setUnitsPerPack(10);
    r.setUnitPrice(new BigDecimal("5.00"));
    r.setAvailability(100);
    r.setMinStockLevel(20);

    when(catalogueItemRepository.existsById("300 00003")).thenReturn(false);

    AdminCatalogueItemDTO result = catalogueService.addProduct(r, admin);

    assertThat(result.getProductId()).isEqualTo("300 00003");
    
    
    
    assertThat(result.getReorderBufferPct()).isEqualByComparingTo("10.00");
    assertThat(result.getIsActive()).isTrue();

    ArgumentCaptor<CatalogueItem> cap = ArgumentCaptor.forClass(CatalogueItem.class);
    verify(catalogueItemRepository).save(cap.capture());
    assertThat(cap.getValue().getAvailability()).isEqualTo(100);}

  @Test
  void addProduct_duplicateId_rejected() {
    CreateProductRequest r = new CreateProductRequest();
    r.setProductId("100 00001");
    when(catalogueItemRepository.existsById("100 00001")).thenReturn(true);

    assertThatThrownBy(() -> catalogueService.addProduct(r, admin))
        .isInstanceOf(ValidationException.class);
    verify(catalogueItemRepository, never()).save(any());}

  @Test
  void updateProduct_partial_onlyChangesGivenFields() {
    CatalogueItem item = paracetamol();
    UpdateProductRequest r = new UpdateProductRequest();
    r.setUnitPrice(new BigDecimal("0.15"));
    r.setMinStockLevel(500);

    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));

    AdminCatalogueItemDTO result = catalogueService.updateProduct("100 00001", r, admin);

    assertThat(result.getUnitPrice()).isEqualByComparingTo("0.15");
    assertThat(result.getMinStockLevel()).isEqualTo(500);
    assertThat(result.getDescription()).isEqualTo("Paracetamol");
  }
  @Test
  void updateProduct_hyphenatedId_normalised() {
    CatalogueItem item = paracetamol();
    UpdateProductRequest r = new UpdateProductRequest();
    r.setDescription("Paracetamol 500mg");
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));
    catalogueService.updateProduct("100-00001", r, admin);

    verify(catalogueItemRepository).findById("100 00001");}
  @Test
  void updateProduct_missing_throws() {
    when(catalogueItemRepository.findById("ZZZ")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> catalogueService.updateProduct("ZZZ", new UpdateProductRequest(), admin))
        .isInstanceOf(ResourceNotFoundException.class);}
  @Test
  void deactivateProduct_softDeletes() {
    CatalogueItem item = paracetamol();
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));
    catalogueService.deactivateProduct("100 00001", admin);
    assertThat(item.getIsActive()).isFalse();}
  @Test
  void reactivateProduct_setsActive() {
    CatalogueItem item = paracetamol();
    item.setIsActive(false);
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));
    catalogueService.reactivateProduct("100 00001", admin);
    assertThat(item.getIsActive()).isTrue();
  }
  @Test
  void addStock_increasesAvailability() {
    
    CatalogueItem item = paracetamol();
    item.setAvailability(250);
    
    StockDeliveryRequest r = new StockDeliveryRequest();
    r.setQuantity(500);
    when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));

    AdminCatalogueItemDTO result = catalogueService.addStock("100 00001", r, admin);

    assertThat(result.getAvailability()).isEqualTo(750);}
  @Test
  void addStock_missingProduct_throws() {
    StockDeliveryRequest r = new StockDeliveryRequest();
    r.setQuantity(100);
    when(catalogueItemRepository.findById("ZZZ")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> catalogueService.addStock("ZZZ", r, admin))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getLowStockProducts_delegates() {
    CatalogueItem low = paracetamol();
    low.setAvailability(100);
    when(catalogueItemRepository.findLowStockProducts()).thenReturn(List.of(low));

    List<CatalogueItem> result = catalogueService.getLowStockProducts();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAvailability()).isLessThan(result.get(0).getMinStockLevel());}
  @Test
  void searchCatalogue_returnsActiveMatches() {
    when(catalogueItemRepository.searchActive("para")).thenReturn(List.of(paracetamol()));
    List<CatalogueItemDTO> result = catalogueService.searchCatalogue("para");
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getDescription()).isEqualTo("Paracetamol");}
  @Test
  void getActiveCatalogue_returnsActive() {
    when(catalogueItemRepository.findByIsActiveTrue()).thenReturn(List.of(paracetamol()));
    List<CatalogueItemDTO> result = catalogueService.getActiveCatalogue();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProductId()).isEqualTo("100 00001");}}
