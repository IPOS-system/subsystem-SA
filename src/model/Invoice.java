package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Invoice generated for a merchant order. */
public class Invoice {

    public enum PaymentStatus { PENDING, PAID, OVERDUE }

    private String        invoiceId;
    private String        orderId;
    private int           merchantId;
    private LocalDateTime invoiceDate;
    private double        amountDue;
    private PaymentStatus paymentStatus;
    private LocalDate     dueDate;

    public Invoice() {}

    public Invoice(String invoiceId, String orderId, int merchantId,
                   double amountDue, LocalDate dueDate) {
        this.invoiceId     = invoiceId;
        this.orderId       = orderId;
        this.merchantId    = merchantId;
        this.invoiceDate   = LocalDateTime.now();
        this.amountDue     = amountDue;
        this.paymentStatus = PaymentStatus.PENDING;
        this.dueDate       = dueDate;
    }

    // Getters
    public String        getInvoiceId()     { return invoiceId; }
    public String        getOrderId()       { return orderId; }
    public int           getMerchantId()    { return merchantId; }
    public LocalDateTime getInvoiceDate()   { return invoiceDate; }
    public double        getAmountDue()     { return amountDue; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public LocalDate     getDueDate()       { return dueDate; }

    // Setters
    public void setInvoiceId(String id)          { this.invoiceId = id; }
    public void setOrderId(String oid)           { this.orderId = oid; }
    public void setMerchantId(int mid)           { this.merchantId = mid; }
    public void setInvoiceDate(LocalDateTime dt) { this.invoiceDate = dt; }
    public void setAmountDue(double amt)         { this.amountDue = amt; }
    public void setPaymentStatus(PaymentStatus s){ this.paymentStatus = s; }
    public void setDueDate(LocalDate d)          { this.dueDate = d; }

    @Override
    public String toString() {
        return "Invoice{id='" + invoiceId + "', order='" + orderId
             + "', due=£" + amountDue + ", status=" + paymentStatus + "}";
    }
}
