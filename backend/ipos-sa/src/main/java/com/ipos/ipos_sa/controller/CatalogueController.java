package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.catalogue.*;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.CatalogueService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for catalogue management.
 *
 * Role rules: GET /api/catalogue → any authenticated user (merchants browse here) GET
 * /api/catalogue/admin → ADMIN, MANAGER only (includes inactive + stock internals) POST
 * /api/catalogue → ADMIN, MANAGER (add product) PUT /api/catalogue/{id} → ADMIN, MANAGER (update
 * product) PUT /api/catalogue/{id}/deactivate → ADMIN, MANAGER (soft-delete) PUT
 * /api/catalogue/{id}/reactivate → ADMIN, MANAGER (restore) POST /api/catalogue/{id}/stock → ADMIN,
 * MANAGER (record stock delivery)
 */
@RestController
@RequestMapping("/api/catalogue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CatalogueController {

  private final CatalogueService catalogueService;
  private final UserRepository userRepository;

  // Merchant-Facing Endpoints

  /**
   * GET /api/catalogue Returns active products only. Optionally filter with ?search=term. Any
   * authenticated user can call this.
   */
  @GetMapping
  public ResponseEntity<List<CatalogueItemDTO>> browseCatalogue(
      @RequestParam(required = false) String search) {

    List<CatalogueItemDTO> result =
        (search != null && !search.isBlank())
            ? catalogueService.searchCatalogue(search)
            : catalogueService.getActiveCatalogue();

    return ResponseEntity.ok(result);
  }

  // Admin-Facing Endpoints

  /** GET /api/catalogue/admin Returns all products with full admin detail. ADMIN/MANAGER only. */
  @GetMapping("/admin")
  public ResponseEntity<List<AdminCatalogueItemDTO>> getAllAdmin(Authentication auth) {
    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    return ResponseEntity.ok(catalogueService.getAllCatalogueAdmin());
  }

  /** GET /api/catalogue/admin/{id} Returns a single product with full admin detail. */
  @GetMapping("/admin/{id}")
  public ResponseEntity<AdminCatalogueItemDTO> getProductAdmin(
      @PathVariable String id, Authentication auth) {
    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    return ResponseEntity.ok(catalogueService.getProductAdmin(id));
  }

  /** POST /api/catalogue Adds a new product to the catalogue. */
  @PostMapping
  public ResponseEntity<AdminCatalogueItemDTO> addProduct(
      @Valid @RequestBody CreateProductRequest request, Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);
    AdminCatalogueItemDTO created = catalogueService.addProduct(request, actingUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * PUT /api/catalogue/{id} Updates product details.
   */
  @PutMapping("/{id}")
  public ResponseEntity<AdminCatalogueItemDTO> updateProduct(
      @PathVariable String id, @RequestBody UpdateProductRequest request, Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);
    return ResponseEntity.ok(catalogueService.updateProduct(id, request, actingUser));
  }

  /**
   * PUT /api/catalogue/{id}/deactivate Soft-deletes a product. Product disappears from
   * merchant view but remains in the database for order history.
   */
  @PutMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivateProduct(@PathVariable String id, Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);
    catalogueService.deactivateProduct(id, actingUser);
    return ResponseEntity.noContent().build();
  }

  /** PUT /api/catalogue/{id}/reactivate Re-activates a previously deactivated product. */
  @PutMapping("/{id}/reactivate")
  public ResponseEntity<Void> reactivateProduct(@PathVariable String id, Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);
    catalogueService.reactivateProduct(id, actingUser);
    return ResponseEntity.noContent().build();
  }

  /** POST /api/catalogue/{id}/stock Records a stock delivery, increases availability. */
  @PostMapping("/{id}/stock")
  public ResponseEntity<AdminCatalogueItemDTO> addStock(
      @PathVariable String id,
      @Valid @RequestBody StockDeliveryRequest request,
      Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);
    return ResponseEntity.ok(catalogueService.addStock(id, request, actingUser));
  }

  // Helpers

  private User resolveUser(Authentication auth) {
    String username = auth.getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new ResourceNotFoundException("Authenticated user not found: " + username));
  }

  private void requireRole(Authentication auth, User.Role... permitted) {
    String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    User.Role callerRole = User.Role.valueOf(roleStr);

    for (User.Role r : permitted) {
      if (r == callerRole) return;
    }
    throw new AccessDeniedException(
        "Role " + callerRole + " is not permitted to perform this action.");
  }
}
