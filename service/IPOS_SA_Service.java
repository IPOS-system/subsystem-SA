package service;

import dao.*;
import model.*;
import model.Merchant.AccountStatus;
import model.Order.Status;
import model.Invoice.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public class IPOS_SA_Service {

    private final UserDAO       userDAO       = new UserDAO();
    private final MerchantDAO   merchantDAO   = new MerchantDAO();
    private final CatalogueDAO  catalogueDAO  = new CatalogueDAO();
    private final OrderDAO      orderDAO      = new OrderDAO();
    private final InvoiceDAO    invoiceDAO    = new InvoiceDAO();
    private final PaymentDAO    paymentDAO    = new PaymentDAO();
    private final AuditDAO      auditDAO      = new AuditDAO();


    public User login(String username, String password) {
        User user = userDAO.authenticate(username, password);
        if (user != null) {
            System.out.println("[Service] Login successful: " + username);
        }
        return user;
    }

    public boolean changePassword(int userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            System.err.println("[Service] Password too short.");
            return false;
        }
        return userDAO.updatePassword(userId, newPassword);
    }


    public int createMerchantAccount(String username, String password,
                                      String companyName, String address,
                                      String phone, String fax, String email,
                                      double creditLimit, Integer discountPlanId) {
        if (username == null || username.isBlank() ||
            password == null || password.isBlank() ||
            companyName == null || companyName.isBlank() ||
            email == null || email.isBlank()) {
            System.err.println("[Service] createMerchantAccount: missing required fields.");
            return -1;
        }

        int userId = userDAO.createUser(username, password, User.Role.MERCHANT);
        if (userId == -1) {
            System.err.println("[Service] Failed to create user for: " + username);
            return -1;
        }

        Merchant m = new Merchant();
        m.setUserId(userId);
        m.setCompanyName(companyName);
        m.setAddress(address);
        m.setPhone(phone);
        m.setFax(fax);
        m.setEmail(email);
        m.setCreditLimit(creditLimit);
        m.setCurrentBalance(0.00);
        m.setAccountStatus(AccountStatus.NORMAL);
        m.setDiscountPlanId(discountPlanId);

        int merchantId = merchantDAO.createMerchant(m);
        if (merchantId == -1) {
            System.err.println("[Service] Failed to create merchant profile for userId: " + userId);
            userDAO.deleteUser(userId); // rollback user creation
            return -1;
        }

        auditDAO.log(userId, "CREATE_MERCHANT_ACCOUNT", "MERCHANT", String.valueOf(merchantId),
                     "Created account for: " + companyName);
        return merchantId;
    }

    public boolean editMerchantAccount(int actorUserId, Merchant updated) {
        boolean ok = merchantDAO.updateMerchant(updated);
        if (ok) {
            auditDAO.log(actorUserId, "EDIT_MERCHANT_ACCOUNT", "MERCHANT",
                         String.valueOf(updated.getMerchantId()), "Updated: " + updated.getCompanyName());
        }
        return ok;
    }

    public boolean deleteMerchantAccount(int actorUserId, int merchantId) {
        Merchant m = merchantDAO.findById(merchantId);
        if (m == null) return false;
        boolean ok = userDAO.setActive(m.getUserId(), false);
        if (ok) {
            auditDAO.log(actorUserId, "DELETE_MERCHANT_ACCOUNT", "MERCHANT",
                         String.valueOf(merchantId), "Deactivated.");
        }
        return ok;
    }

    public boolean setCreditLimit(int actorUserId, int merchantId, double limit) {
        if (limit < 0) {
            System.err.println("[Service] Credit limit cannot be negative.");
            return false;
        }
        boolean ok = merchantDAO.setCreditLimit(merchantId, limit);
        if (ok) {
            auditDAO.log(actorUserId, "SET_CREDIT_LIMIT", "MERCHANT",
                         String.valueOf(merchantId), "New limit: £" + limit);
        }
        return ok;
    }

    public boolean changeDiscountPlan(int actorUserId, int merchantId, int planId) {
        Merchant m = merchantDAO.findById(merchantId);
        if (m == null) return false;
        m.setDiscountPlanId(planId);
        boolean ok = merchantDAO.updateMerchant(m);
        if (ok) {
            auditDAO.log(actorUserId, "CHANGE_DISCOUNT_PLAN", "MERCHANT",
                         String.valueOf(merchantId), "New plan: " + planId);
        }
        return ok;
    }

    public String getAccountStatus(int merchantId) {
        Merchant m = merchantDAO.findById(merchantId);
        if (m == null) return "NOT_FOUND";
        return m.getAccountStatus().name();
    }

    public double getAccountBalance(int merchantId) {
        Merchant m = merchantDAO.findById(merchantId);
        return m != null ? m.getCurrentBalance() : -1;
    }


    public boolean addCatalogueItem(Product p) {
        return catalogueDAO.addProduct(p);
    }

    public boolean updateCatalogueItem(Product p) {
        return catalogueDAO.updateProduct(p);
    }

    public boolean deleteCatalogueItem(String productId) {
        return catalogueDAO.deactivateProduct(productId);
    }

    public List<Product> getCatalogue() {
        return catalogueDAO.findAllActive();
    }

    public List<Product> searchCatalogue(String keyword) {
        return catalogueDAO.search(keyword);
    }

    public boolean addStock(String productId, int quantity) {
        if (quantity <= 0) return false;
        return catalogueDAO.addStock(productId, quantity);
    }

    public List<Product> getLowStockReport() {
        return catalogueDAO.getLowStockItems();
    }

    public Product getProductById(String productId) {
        return catalogueDAO.findById(productId);
    }

    public List<Merchant> getAllMerchants() {
        return merchantDAO.findAll();
    }

    public Merchant getMerchantById(int merchantId) {
        return merchantDAO.findById(merchantId);
    }

    public Merchant getMerchantByUserId(int userId) {
        return merchantDAO.findByUserId(userId);
    }

    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    public Order getOrderById(String orderId) {
        return orderDAO.findById(orderId);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceDAO.findAll();
    }

    public boolean restoreAccountToNormal(int actorUserId, int merchantId) {
        boolean ok = merchantDAO.updateAccountStatus(merchantId, Merchant.AccountStatus.NORMAL);
        if (ok) {
            auditDAO.log(actorUserId, "RESTORE_ACCOUNT", "MERCHANT",
                         String.valueOf(merchantId), "Restored to NORMAL by manager.");
        }
        return ok;
    }


    public String generateReport(String type, String merchantIdStr,
                                  String fromStr, String toStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════\n");
        sb.append("  ").append(type).append("\n");
        sb.append("  Period: ").append(fromStr).append(" — ").append(toStr).append("\n");
        sb.append("══════════════════════════════════════════\n\n");

        try {
            int merchantId = Integer.parseInt(merchantIdStr);
            Merchant m = merchantDAO.findById(merchantId);
            if (m == null) return "Merchant ID not found: " + merchantId;

            sb.append("Client: ").append(m.getCompanyName()).append("\n");
            sb.append("IPOS Account: ").append(merchantId).append("\n\n");

            List<Order> orders = orderDAO.findByMerchant(merchantId);

            if (type.equals("Merchant Orders Summary")) {
                sb.append(String.format("%-12s %-12s %-12s %-12s %-12s %-10s%n",
                    "Order ID", "Date", "Amount £", "Dispatched", "Delivered", "Paid"));
                sb.append("─".repeat(72)).append("\n");
                double total = 0;
                for (Order o : orders) {
                    Invoice inv = invoiceDAO.findByOrderId(o.getOrderId());
                    sb.append(String.format("%-12s %-12s %-12s %-12s %-12s %-10s%n",
                        o.getOrderId(),
                        o.getOrderDate().toLocalDate(),
                        String.format("%.2f", o.getTotalAmount()),
                        o.getDispatchDate() != null ? o.getDispatchDate().toLocalDate() : "Pending",
                        o.getStatus() == Order.Status.DELIVERED ? o.getExpectedDelivery() != null
                            ? o.getExpectedDelivery().toLocalDate() : "Yes" : "Pending",
                        inv != null && inv.getPaymentStatus() == Invoice.PaymentStatus.PAID
                            ? "Paid" : "Pending"
                    ));
                    total += o.getTotalAmount();
                }
                sb.append("─".repeat(72)).append("\n");
                sb.append(String.format("Total: %d orders   £%.2f%n", orders.size(), total));

            } else if (type.equals("Merchant Activity Detail")) {
                for (Order o : orders) {
                    sb.append("Order: ").append(o.getOrderId())
                      .append("  Date: ").append(o.getOrderDate().toLocalDate())
                      .append("  Total: £").append(String.format("%.2f", o.getTotalAmount()))
                      .append("\n");
                    Order full = orderDAO.findById(o.getOrderId());
                    if (full != null) {
                        for (OrderItem item : full.getItems()) {
                            sb.append(String.format("  %-20s x%-4d @ £%-8.2f = £%.2f%n",
                                item.getProductId(), item.getQuantity(),
                                item.getUnitPrice(), item.getLineTotal()));
                        }
                    }
                    sb.append("\n");
                }

            } else {
                double totalRevenue = orders.stream().mapToDouble(Order::getTotalAmount).sum();
                sb.append("Total Orders:  ").append(orders.size()).append("\n");
                sb.append("Total Revenue: £").append(String.format("%.2f", totalRevenue)).append("\n");
            }

        } catch (NumberFormatException e) {
            sb.append("[No merchant ID provided — showing system-wide summary]\n\n");
            List<Order> all = orderDAO.findAll();
            double total = all.stream().mapToDouble(Order::getTotalAmount).sum();
            sb.append("Total orders in system: ").append(all.size()).append("\n");
            sb.append("Total revenue:          £").append(String.format("%.2f", total)).append("\n");
        }

        return sb.toString();
    }


    public String placeOrder(int merchantId, List<OrderItem> items) {
        Merchant merchant = merchantDAO.findById(merchantId);
        if (merchant == null) {
            System.err.println("[Service] placeOrder: merchant not found.");
            return null;
        }
        if (!merchant.canPlaceOrders()) {
            System.err.println("[Service] placeOrder: account status = "
                + merchant.getAccountStatus() + ". Orders blocked.");
            return null;
        }

        for (OrderItem item : items) {
            Product p = catalogueDAO.findById(item.getProductId());
            if (p == null || !p.isActive()) {
                System.err.println("[Service] placeOrder: product not found: " + item.getProductId());
                return null;
            }
            if (p.getAvailability() < item.getQuantity()) {
                System.err.println("[Service] placeOrder: insufficient stock for " + item.getProductId());
                return null;
            }
            item.setUnitPrice(p.getUnitPrice());
        }

        double subtotal = items.stream().mapToDouble(OrderItem::getLineTotal).sum();

        double discount = calculateDiscount(merchant, subtotal);
        double total    = subtotal - discount;

        if (merchantDAO.wouldExceedCreditLimit(merchantId, total)) {
            System.err.println("[Service] placeOrder: would exceed credit limit.");
            return null;
        }

        String orderId = orderDAO.generateOrderId();
        Order order = new Order(orderId, merchantId);
        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);

        boolean saved = orderDAO.createOrder(order);
        if (!saved) {
            System.err.println("[Service] placeOrder: failed to save order.");
            return null;
        }

        for (OrderItem item : items) {
            catalogueDAO.reduceStock(item.getProductId(), item.getQuantity());
        }

        double newBalance = merchant.getCurrentBalance() + total;
        merchantDAO.updateBalance(merchantId, newBalance);

        generateInvoice(orderId, merchantId, total);

        return orderId;
    }

    public boolean updateOrderStatus(String orderId, String status) {
        try {
            Status s = Status.valueOf(status.toUpperCase());
            return orderDAO.updateStatus(orderId, s);
        } catch (IllegalArgumentException e) {
            System.err.println("[Service] Invalid status: " + status);
            return false;
        }
    }

    public boolean dispatchOrder(String orderId, String dispatchedBy,
                                  String courier, String courierRef,
                                  LocalDateTime expectedDelivery) {
        return orderDAO.recordDispatch(orderId, dispatchedBy, courier, courierRef, expectedDelivery);
    }

    public String trackOrder(String orderId) {
        Order o = orderDAO.findById(orderId);
        if (o == null) return "NOT_FOUND";
        return o.getStatus().name();
    }

    public List<Order> getOrderHistory(int merchantId) {
        return orderDAO.findByMerchant(merchantId);
    }

    public Invoice getInvoice(String invoiceId) {
        return invoiceDAO.findById(invoiceId);
    }

    public List<Invoice> getInvoicesForMerchant(int merchantId) {
        return invoiceDAO.findByMerchant(merchantId);
    }


    public boolean recordPayment(int merchantId, String invoiceId,
                                  double amountPaid, String paymentMethod,
                                  int recordedByUserId) {
        Invoice inv = invoiceDAO.findById(invoiceId);
        if (inv == null) {
            System.err.println("[Service] recordPayment: invoice not found: " + invoiceId);
            return false;
        }

        Payment.Method method;
        try {
            method = Payment.Method.valueOf(paymentMethod.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[Service] Invalid payment method: " + paymentMethod);
            return false;
        }

        Payment payment = new Payment(merchantId, invoiceId, amountPaid, method, recordedByUserId);
        boolean saved = paymentDAO.recordPayment(payment);
        if (!saved) return false;

        Merchant m = merchantDAO.findById(merchantId);
        if (m != null) {
            double newBalance = Math.max(0, m.getCurrentBalance() - amountPaid);
            merchantDAO.updateBalance(merchantId, newBalance);

            if (m.getAccountStatus() == AccountStatus.SUSPENDED) {
                merchantDAO.updateAccountStatus(merchantId, AccountStatus.NORMAL);
                System.out.println("[Service] Account restored to NORMAL for merchant: " + merchantId);
            }
        }

        if (amountPaid >= inv.getAmountDue()) {
            invoiceDAO.updatePaymentStatus(invoiceId, PaymentStatus.PAID);
        }

        return true;
    }


    public void enforceCreditControl() {
        List<Merchant> merchants = merchantDAO.findAll();
        LocalDate today = LocalDate.now();

        for (Merchant m : merchants) {
            List<Invoice> invoices = invoiceDAO.findByMerchant(m.getMerchantId());
            for (Invoice inv : invoices) {
                if (inv.getPaymentStatus() == PaymentStatus.PAID) continue;

                long daysOverdue = today.toEpochDay() - inv.getDueDate().toEpochDay();

                if (daysOverdue > 30) {
                    if (m.getAccountStatus() != AccountStatus.IN_DEFAULT) {
                        merchantDAO.updateAccountStatus(m.getMerchantId(), AccountStatus.IN_DEFAULT);
                        invoiceDAO.updatePaymentStatus(inv.getInvoiceId(), PaymentStatus.OVERDUE);
                        System.out.println("[CreditControl] Merchant " + m.getMerchantId()
                            + " → IN_DEFAULT (>" + daysOverdue + " days overdue)");
                    }
                } else if (daysOverdue > 15) {
                    if (m.getAccountStatus() == AccountStatus.NORMAL) {
                        merchantDAO.updateAccountStatus(m.getMerchantId(), AccountStatus.SUSPENDED);
                        invoiceDAO.updatePaymentStatus(inv.getInvoiceId(), PaymentStatus.OVERDUE);
                        System.out.println("[CreditControl] Merchant " + m.getMerchantId()
                            + " → SUSPENDED (" + daysOverdue + " days overdue)");
                    }
                } else if (daysOverdue > 0) {
                    invoiceDAO.updatePaymentStatus(inv.getInvoiceId(), PaymentStatus.OVERDUE);
                }
            }
        }
    }


    public boolean shouldShowPaymentReminder(int merchantId) {
        List<Invoice> invoices = invoiceDAO.findByMerchant(merchantId);
        LocalDate today = LocalDate.now();
        for (Invoice inv : invoices) {
            if (inv.getPaymentStatus() == PaymentStatus.PAID) continue;
            long daysOverdue = today.toEpochDay() - inv.getDueDate().toEpochDay();
            if (daysOverdue > 0 && daysOverdue <= 15) return true;
        }
        return false;
    }


    private double calculateDiscount(Merchant merchant, double subtotal) {
        // TODO: implement flexible tier lookup from discount_plans/discount_tiers tables
        return 0.0; // placeholder until DiscountPlanDAO is added
    }

    private void generateInvoice(String orderId, int merchantId, double amount) {
        String invoiceId   = invoiceDAO.generateInvoiceId();
        LocalDate dueDate  = LocalDate.now().withDayOfMonth(
                                LocalDate.now().lengthOfMonth());
        Invoice inv = new Invoice(invoiceId, orderId, merchantId, amount, dueDate);
        invoiceDAO.createInvoice(inv);
    }
}
