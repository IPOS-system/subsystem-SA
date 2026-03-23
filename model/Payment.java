package model;

import java.time.LocalDateTime;

public class Payment {

    public enum Method { BANK_TRANSFER, CARD, CHEQUE }

    private int           paymentId;
    private int           merchantId;
    private String        invoiceId;
    private double        amountPaid;
    private Method        paymentMethod;
    private LocalDateTime paymentDate;
    private int           recordedBy;   // user_id of accountant
    private String        notes;

    public Payment() {}

    public Payment(int merchantId, String invoiceId, double amountPaid,
                   Method method, int recordedBy) {
        this.merchantId    = merchantId;
        this.invoiceId     = invoiceId;
        this.amountPaid    = amountPaid;
        this.paymentMethod = method;
        this.paymentDate   = LocalDateTime.now();
        this.recordedBy    = recordedBy;
    }

    public int           getPaymentId()     { return paymentId; }
    public int           getMerchantId()    { return merchantId; }
    public String        getInvoiceId()     { return invoiceId; }
    public double        getAmountPaid()    { return amountPaid; }
    public Method        getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getPaymentDate()   { return paymentDate; }
    public int           getRecordedBy()    { return recordedBy; }
    public String        getNotes()         { return notes; }

    public void setPaymentId(int id)              { this.paymentId = id; }
    public void setMerchantId(int mid)            { this.merchantId = mid; }
    public void setInvoiceId(String iid)          { this.invoiceId = iid; }
    public void setAmountPaid(double amt)         { this.amountPaid = amt; }
    public void setPaymentMethod(Method m)        { this.paymentMethod = m; }
    public void setPaymentDate(LocalDateTime dt)  { this.paymentDate = dt; }
    public void setRecordedBy(int uid)            { this.recordedBy = uid; }
    public void setNotes(String notes)            { this.notes = notes; }

    @Override
    public String toString() {
        return "Payment{id=" + paymentId + ", invoice='" + invoiceId
             + "', amount=£" + amountPaid + ", method=" + paymentMethod + "}";
    }
}
