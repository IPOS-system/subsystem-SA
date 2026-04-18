package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.order.*;
import com.ipos.ipos_sa.entity.Merchant;
import com.ipos.ipos_sa.entity.Order;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.exception.ResourceNotFoundException;
import com.ipos.ipos_sa.repository.MerchantRepository;
import com.ipos.ipos_sa.repository.UserRepository;
import com.ipos.ipos_sa.service.AccountService;
import com.ipos.ipos_sa.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for order management.
 *
 * Role rules: POST /api/orders → MERCHANT (place order from own account) GET
 * /api/orders/incomplete → ADMIN, MANAGER (orders not yet completed) GET
 * /api/orders/merchant/{merchantId} → MERCHANT (own), ADMIN, MANAGER GET /api/orders/{id} → any
 * authenticated user (track order) GET /api/orders → ADMIN, MANAGER (all orders) PUT
 * /api/orders/{id}/status → ADMIN, MANAGER (update status) PUT /api/orders/{id}/dispatch → ADMIN,
 * MANAGER (dispatch with courier details)
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

  private final OrderService orderService;
  private final AccountService accountService;
  private final UserRepository userRepository;
  private final MerchantRepository merchantRepository;

@PostMapping
  public ResponseEntity<PlaceOrderResponse> placeOrder(
      @Valid @RequestBody PlaceOrderRequest request, Authentication auth) {

    try {
      requireRole(auth, User.Role.MERCHANT);
      User user = resolveUser(auth);

      Merchant merchant =
          merchantRepository
              .findByUser(user)
              .orElseThrow(
                  () -> new ResourceNotFoundException(
                      "Merchant profile not found for user: " + user.getUsername()));

      accountService.checkAndUpdateAccountStatus(merchant.getMerchantId());

      OrderDTO created = orderService.placeOrder(merchant.getMerchantId(), request, user);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(PlaceOrderResponse.success(created.getOrderId()));

    } catch (com.ipos.ipos_sa.exception.ValidationException e) {
      String errorCode = inferErrorCode(e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(PlaceOrderResponse.failure(errorCode, e.getMessage()));

    } catch (ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(PlaceOrderResponse.failure("PRODUCT_NOT_FOUND", e.getMessage()));

    } catch (AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(PlaceOrderResponse.failure("ACCESS_DENIED", e.getMessage()));

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(PlaceOrderResponse.failure("INTERNAL_ERROR", e.getMessage()));
    }
  }

  /**
   * Maps validation error messages to error codes.
   */
  private String inferErrorCode(String message) {
    if (message == null) return "VALIDATION_ERROR";
    String lower = message.toLowerCase();
    if (lower.contains("suspended")) return "ACCOUNT_SUSPENDED";
    if (lower.contains("in_default") || lower.contains("in default")) return "ACCOUNT_IN_DEFAULT";
    if (lower.contains("insufficient stock")) return "INSUFFICIENT_STOCK";
    if (lower.contains("not available") || lower.contains("inactive")) return "PRODUCT_INACTIVE";
    if (lower.contains("credit")) return "CREDIT_LIMIT_EXCEEDED";
    return "VALIDATION_ERROR";
  }


  // Incomplete Orders

  /**
   * GET /api/orders/incomplete Returns all orders that are not yet DELIVERED or CANCELLED.
   *
   */
  @GetMapping("/incomplete")
  public ResponseEntity<List<OrderDTO>> getIncompleteOrders(Authentication auth) {
    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER, User.Role.MERCHANT);
    return ResponseEntity.ok(orderService.getIncompleteOrders());
  }

  // Order History — Merchant

  /**
   * GET /api/orders/merchant/{merchantId} Returns all orders for a specific merchant, newest first.
   * Merchants can only query their own history; admins/managers can query any.
   *
   */
  @GetMapping("/merchant/{merchantId}")
  public ResponseEntity<List<OrderDTO>> getOrdersByMerchant(
      @PathVariable Integer merchantId, Authentication auth) {

    if (hasRole(auth, User.Role.MERCHANT)) {
      User user = resolveUser(auth);
      Merchant merchant = merchantRepository.findByUser(user).orElse(null);
      if (merchant == null || !merchant.getMerchantId().equals(merchantId)) {
        throw new AccessDeniedException("You can only view your own orders.");
      }
    } else {
      requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    }

    return ResponseEntity.ok(orderService.getOrdersByMerchant(merchantId));
  }

  // Track Order

  /**
   * GET /api/orders/{id} Returns a single order with full detail including line items and dispatch
   * info.
   */
  @GetMapping("/{id}")
  public ResponseEntity<OrderDTO> getOrder(@PathVariable String id, Authentication auth) {

    OrderDTO order = orderService.getOrderById(id);

    // Merchants can only view their own orders
    if (hasRole(auth, User.Role.MERCHANT)) {
      User user = resolveUser(auth);
      Merchant merchant = merchantRepository.findByUser(user).orElse(null);
      if (merchant == null || !merchant.getMerchantId().equals(order.getMerchantId())) {
        throw new AccessDeniedException("You can only view your own orders.");
      }
    }

    return ResponseEntity.ok(order);
  }

  // All Orders — Admin

  /**
   * GET /api/orders Returns all orders. Optionally filter with ?status=ACCEPTED etc. ADMIN and
   * MANAGER only.
   */
  @GetMapping
  public ResponseEntity<List<OrderDTO>> getAllOrders(
      @RequestParam(required = false) Order.OrderStatus status, Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);

    List<OrderDTO> result =
        (status != null) ? orderService.getOrdersByStatus(status) : orderService.getAllOrders();

    return ResponseEntity.ok(result);
  }

  // Update Order Status

  /**
   * PUT /api/orders/{id}/status Updates the order status. Use for ACCEPTED→PROCESSING,
   * PROCESSING→CANCELLED, DISPATCHED→DELIVERED.
   *
   * For DISPATCHED specifically, use PUT /api/orders/{id}/dispatch instead because courier
   * details are required.
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<OrderDTO> updateOrderStatus(
      @PathVariable String id,
      @Valid @RequestBody UpdateOrderStatusRequest request,
      Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER, User.Role.MERCHANT);
    User actingUser = resolveUser(auth);

    return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus(), actingUser));
  }

  // Dispatch

  /**
   * PUT /api/orders/{id}/dispatch Marks an order as DISPATCHED and records courier name, reference,
   * dispatch date, and expected delivery date.
   */
  @PutMapping("/{id}/dispatch")
  public ResponseEntity<OrderDTO> dispatchOrder(
      @PathVariable String id,
      @Valid @RequestBody DispatchOrderRequest request,
      Authentication auth) {

    requireRole(auth, User.Role.ADMIN, User.Role.MANAGER);
    User actingUser = resolveUser(auth);

    return ResponseEntity.ok(orderService.dispatchOrder(id, request, actingUser));
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

  private boolean hasRole(Authentication auth, User.Role role) {
    String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    return role.name().equals(roleStr);
  }
}