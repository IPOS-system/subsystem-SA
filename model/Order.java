package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Order {

    public enum Status { ACCEPTED, PROCESSING, DISPATCHED, DELIVERED, CANCELLED }

    private String        orderId;
    private int           merchantId;
    private LocalDateTime orderDate;
    private Status        status;
    private double        subtotal;
    private double        discountAmount;
    private double        totalAmount;
    private List<OrderItem> items;

    private String        dispatchedBy;
    private LocalDateTime dispatchDate;
    private String        courier;
    private String        courierRef;
    private LocalDateTime expectedDelivery;


    public Order() {
        this.items     = new ArrayList<>();
        this.orderDate = LocalDateTime.now();
        this.status    = Status.ACCEPTED;
    }

    public Order(String orderId, int merchantId) {
        this();
        this.orderId    = orderId;
        this.merchantId = merchantId;
    }


    public String        getOrderId()         { return orderId; }
    public int           getMerchantId()       { return merchantId; }
    public LocalDateTime getOrderDate()        { return orderDate; }
    public Status        getStatus()           { return status; }
    public double        getSubtotal()         { return subtotal; }
    public double        getDiscountAmount()   { return discountAmount; }
    public double        getTotalAmount()      { return totalAmount; }
    public List<OrderItem> getItems()          { return items; }
    public String        getDispatchedBy()     { return dispatchedBy; }
    public LocalDateTime getDispatchDate()     { return dispatchDate; }
    public String        getCourier()          { return courier; }
    public String        getCourierRef()       { return courierRef; }
    public LocalDateTime getExpectedDelivery() { return expectedDelivery; }

    public void setOrderId(String id)                   { this.orderId = id; }
    public void setMerchantId(int mid)                  { this.merchantId = mid; }
    public void setOrderDate(LocalDateTime dt)          { this.orderDate = dt; }
    public void setStatus(Status s)                     { this.status = s; }
    public void setSubtotal(double v)                   { this.subtotal = v; }
    public void setDiscountAmount(double v)             { this.discountAmount = v; }
    public void setTotalAmount(double v)                { this.totalAmount = v; }
    public void setItems(List<OrderItem> items)         { this.items = items; }
    public void setDispatchedBy(String by)              { this.dispatchedBy = by; }
    public void setDispatchDate(LocalDateTime dt)       { this.dispatchDate = dt; }
    public void setCourier(String c)                    { this.courier = c; }
    public void setCourierRef(String ref)               { this.courierRef = ref; }
    public void setExpectedDelivery(LocalDateTime dt)   { this.expectedDelivery = dt; }

    public void addItem(OrderItem item) { items.add(item); }

    public void recalculateSubtotal() {
        subtotal = items.stream().mapToDouble(OrderItem::getLineTotal).sum();
        totalAmount = subtotal - discountAmount;
    }

    @Override
    public String toString() {
        return "Order{id='" + orderId + "', merchant=" + merchantId
             + ", status=" + status + ", total=£" + totalAmount + "}";
    }
}
