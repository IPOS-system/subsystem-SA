package com.ipos.ipos_sa.service;

import com.ipos.ipos_sa.dto.account.*;
import com.ipos.ipos_sa.entity.*;
import com.ipos.ipos_sa.exception.*;
import com.ipos.ipos_sa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository          userRepository;
    private final MerchantRepository      merchantRepository;
    private final DiscountPlanRepository  discountPlanRepository;
    private final InvoiceRepository       invoiceRepository;
    private final AuditLogRepository      auditLogRepository;
    private final PasswordEncoder         passwordEncoder;

    // ── Merchant Account Creation ─────────────────────────────────────────────

    /**
     * Creates a new merchant account (User + Merchant records).
     * Per the brief: if any required field is missing the account must NOT be created.
     * All required fields are validated by @Valid on the request DTO before this
     * method is called, so we only need to check business-level rules here.
     *
     * @param request    the incoming creation request
     * @param actingUser the admin user performing this action (for audit log)
     */
    @Transactional
    public MerchantDTO createMerchantAccount(CreateMerchantRequest request, User actingUser) {

        // 1. Username must be unique
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        // 2. Discount plan must exist
        DiscountPlan discountPlan = discountPlanRepository.findById(request.getDiscountPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DiscountPlan", request.getDiscountPlanId()));

        // 3. Create the User record
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.MERCHANT)
                .isActive(true)
                .build();
        userRepository.save(user);

        // 4. Create the Merchant record linked to the User
        Merchant merchant = Merchant.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .fax(request.getFax())
                .email(request.getEmail())
                .creditLimit(request.getCreditLimit())
                .currentBalance(java.math.BigDecimal.ZERO)
                .accountStatus(Merchant.AccountStatus.NORMAL)
                .statusChangedAt(LocalDateTime.now())
                .discountPlan(discountPlan)
                .build();
        merchantRepository.save(merchant);

        // 5. Audit log
        audit(actingUser, "CREATE_MERCHANT_ACCOUNT",
                "merchant", String.valueOf(merchant.getMerchantId()),
                "Created merchant account for: " + request.getCompanyName());

        log.info("Merchant account created: {} (userId={})", request.getUsername(), user.getUserId());
        return toMerchantDTO(merchant);
    }

    // ── Staff Account Creation ────────────────────────────────────────────────

    /**
     * Creates a new staff account (ADMIN, MANAGER, ACCOUNTANT, DIRECTOR).
     * Only ADMIN users should call this endpoint — enforced in the controller.
     *
     * @param request    the incoming creation request
     * @param actingUser the admin performing this action
     */
    @Transactional
    public StaffDTO createStaffAccount(CreateStaffRequest request, User actingUser) {

        // 1. MERCHANT role must not be assignable via this endpoint
        if (request.getRole() == User.Role.MERCHANT) {
            throw new ValidationException(
                    "MERCHANT accounts must be created via the merchant account endpoint.");
        }

        // 2. Username must be unique
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        // 3. Create the User record
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .build();
        userRepository.save(user);

        // 4. Audit log
        audit(actingUser, "CREATE_STAFF_ACCOUNT",
                "user", String.valueOf(user.getUserId()),
                "Created staff account: " + request.getUsername() + " role=" + request.getRole());

        log.info("Staff account created: {} role={}", request.getUsername(), request.getRole());
        return toStaffDTO(user);
    }

    // ── Deactivate Account ────────────────────────────────────────────────────

    /**
     * Soft-deletes an account by setting isActive = false.
     * The record is retained in the database for audit and reporting purposes.
     */
    @Transactional
    public void deactivateAccount(Integer userId, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsActive(false);
        userRepository.save(user);

        audit(actingUser, "DEACTIVATE_ACCOUNT",
                "user", String.valueOf(userId),
                "Deactivated account: " + user.getUsername());

        log.info("Account deactivated: userId={}", userId);
    }

    // ── Reactivate Account ────────────────────────────────────────────────────

    /**
     * Re-activates a previously deactivated account.
     */
    @Transactional
    public void reactivateAccount(Integer userId, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setIsActive(true);
        userRepository.save(user);

        audit(actingUser, "REACTIVATE_ACCOUNT",
                "user", String.valueOf(userId),
                "Reactivated account: " + user.getUsername());

        log.info("Account reactivated: userId={}", userId);
    }

    // ── Update Merchant Contact Details ───────────────────────────────────────

    /**
     * Updates the contact fields of a merchant account.
     * Only non-null fields in the request are applied (partial update).
     */
    @Transactional
    public MerchantDTO updateMerchantDetails(Integer merchantId,
                                             UpdateMerchantRequest request,
                                             User actingUser) {

        Merchant merchant = merchantRepository
                .findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        if (request.getCompanyName() != null) merchant.setCompanyName(request.getCompanyName());
        if (request.getAddress()     != null) merchant.setAddress(request.getAddress());
        if (request.getPhone()       != null) merchant.setPhone(request.getPhone());
        if (request.getFax()         != null) merchant.setFax(request.getFax());
        if (request.getEmail()       != null) merchant.setEmail(request.getEmail());

        // Password change - ADMIN / MANAGER only ()
        // Credit limit — ADMIN / MANAGER only (caller must enforce role before calling)
        if (request.getCreditLimit() != null) 
        {
            merchant.setCreditLimit(request.getCreditLimit());
        }

        // Discount plan — ADMIN / MANAGER only
        if (request.getDiscountPlanId() != null) 
        {
            DiscountPlan plan = discountPlanRepository
            .findById(request.getDiscountPlanId())
            .orElseThrow(() -> new ResourceNotFoundException
            (
                "DiscountPlan", request.getDiscountPlanId()));
            merchant.setDiscountPlan(plan);
        }

        merchantRepository.save(merchant);

        audit(actingUser, "UPDATE_MERCHANT_DETAILS",
                "merchant", String.valueOf(merchantId),
                "Updated merchant details for: " + merchant.getCompanyName());

        return toMerchantDTO(merchant);
    }

    /**
     * Changes the role of an existing user account.
     * Per the marking sheet: "change roles (promote/demote) to level of access."
     * The user's username and password are retained.
     */
    @Transactional
    public StaffDTO changeUserRole(Integer userId, User.Role newRole, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        User.Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        audit(actingUser, "CHANGE_ROLE",
                "user", String.valueOf(userId),
                "Changed role from " + oldRole + " to " + newRole
                        + " for user: " + user.getUsername());

        log.info("Role changed: userId={} from {} to {}", userId, oldRole, newRole);

        return StaffDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

        // ── Reset Password (UC-3) ──────────────────────────────────────────────────

    /**
     * Resets a user's password. ADMIN only — enforced in the controller.
     * Per the brief (UC-3), the old password is invalidated and replaced.
     */
    @Transactional
    public void resetPassword(Integer userId, String newPassword, User actingUser) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        audit(actingUser, "RESET_PASSWORD",
                "user", String.valueOf(userId),
                "Password reset for: " + user.getUsername());

        log.info("Password reset: userId={} by adminId={}", userId, actingUser.getUserId());
    }

    // ── Restore from IN_DEFAULT ───────────────────────────────────────────────

    /**
     * Restores a merchant account from IN_DEFAULT to NORMAL.
     * Per the brief: only DIRECTOR or ADMIN can authorise this.
     * Role check is enforced in the controller — this method trusts the caller.
     */
    @Transactional
    public MerchantDTO restoreFromDefault(Integer merchantId, User actingUser) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        if (merchant.getAccountStatus() != Merchant.AccountStatus.IN_DEFAULT) {
            throw new ValidationException(
                    "Merchant account is not IN_DEFAULT — no restoration needed.");
        }

        merchant.setAccountStatus(Merchant.AccountStatus.NORMAL);
        merchant.setStatusChangedAt(LocalDateTime.now());
        merchant.setAuthorizedBy(actingUser);
        merchantRepository.save(merchant);

        audit(actingUser, "RESTORE_FROM_DEFAULT",
                "merchant", String.valueOf(merchantId),
                "Restored merchant from IN_DEFAULT: " + merchant.getCompanyName());

        log.info("Merchant restored from IN_DEFAULT: merchantId={} by userId={}",
                merchantId, actingUser.getUserId());

        return toMerchantDTO(merchant);
    }

    // ── Account Status Check ──────────────────────────────────────────────────

    /**
     * Checks the merchant's oldest unpaid invoice and automatically transitions
     * account status based on how many days overdue it is:
     *
     *   0–15 days overdue  → NORMAL  (reminder flag set on login response, handled in AuthService)
     *   15–30 days overdue → SUSPENDED
     *   30+ days overdue   → IN_DEFAULT
     *
     * Called on every merchant login and before every order placement.
     * Does nothing if the account is already IN_DEFAULT (requires manual restoration).
     */
    @Transactional
    public void checkAndUpdateAccountStatus(Integer merchantId) {

        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        // IN_DEFAULT can only be cleared manually — don't auto-change it
        if (merchant.getAccountStatus() == Merchant.AccountStatus.IN_DEFAULT) {
            return;
        }

        List<Invoice> unpaid = invoiceRepository
                .findUnpaidByMerchantOrderByDueDateAsc(merchantId);

        if (unpaid.isEmpty()) {
            // All invoices paid — ensure account is NORMAL
            if (merchant.getAccountStatus() != Merchant.AccountStatus.NORMAL) {
                merchant.setAccountStatus(Merchant.AccountStatus.NORMAL);
                merchant.setStatusChangedAt(LocalDateTime.now());
                merchantRepository.save(merchant);
            }
            return;
        }

        // Use the oldest unpaid invoice to determine overdue days
        LocalDate oldestDueDate = unpaid.get(0).getDueDate();
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                oldestDueDate, LocalDate.now());

        Merchant.AccountStatus newStatus;
        if (daysOverdue > 30) {
            newStatus = Merchant.AccountStatus.IN_DEFAULT;
        } else if (daysOverdue > 15) {
            newStatus = Merchant.AccountStatus.SUSPENDED;
        } else {
            newStatus = Merchant.AccountStatus.NORMAL;
        }

        // Only write to DB if the status actually changed
        if (newStatus != merchant.getAccountStatus()) {
            log.info("Merchant {} status changing from {} to {} ({} days overdue)",
                    merchantId, merchant.getAccountStatus(), newStatus, daysOverdue);
            merchant.setAccountStatus(newStatus);
            merchant.setStatusChangedAt(LocalDateTime.now());
            merchantRepository.save(merchant);
        }
    }

    // ── Read / Query ──────────────────────────────────────────────────────────

    /** Returns all merchants — used by the Admin/Manager account management screen. */
    public List<MerchantDTO> getAllMerchants() {
        return merchantRepository.findAll()
                .stream()
                .map(this::toMerchantDTO)
                .collect(Collectors.toList());
    }

    /** Search merchants by company name or email — powers the search bar. */
    public List<MerchantDTO> searchMerchants(String term) {
        return merchantRepository.searchByNameOrEmail(term)
                .stream()
                .map(this::toMerchantDTO)
                .collect(Collectors.toList());
    }

    /** Returns all non-merchant (staff) users — used by Admin account management. */
    public List<StaffDTO> getAllStaff() {
        return userRepository.findByRoleNot(User.Role.MERCHANT)
                .stream()
                .map(this::toStaffDTO)
                .collect(Collectors.toList());
    }

    /** Get a single merchant by ID. */
    public MerchantDTO getMerchantById(Integer merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));
        return toMerchantDTO(merchant);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    public MerchantDTO toMerchantDTO(Merchant merchant) {
        return MerchantDTO.builder()
                .merchantId(merchant.getMerchantId())
                .userId(merchant.getUser().getUserId())
                .username(merchant.getUser().getUsername())
                .companyName(merchant.getCompanyName())
                .address(merchant.getAddress())
                .phone(merchant.getPhone())
                .fax(merchant.getFax())
                .email(merchant.getEmail())
                .creditLimit(merchant.getCreditLimit())
                .currentBalance(merchant.getCurrentBalance())
                .accountStatus(merchant.getAccountStatus())
                .statusChangedAt(merchant.getStatusChangedAt())
                .discountPlanId(merchant.getDiscountPlan() != null
                        ? merchant.getDiscountPlan().getPlanId() : null)
                .discountPlanName(merchant.getDiscountPlan() != null
                        ? merchant.getDiscountPlan().getPlanName() : null)
                .createdAt(merchant.getCreatedAt())
                .build();
    }

    private StaffDTO toStaffDTO(User user) {
        return StaffDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // ── Audit Helper ──────────────────────────────────────────────────────────

    private void audit(User actor, String action, String targetType,
                       String targetId, String details) {
        AuditLog entry = AuditLog.builder()
                .user(actor)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}
