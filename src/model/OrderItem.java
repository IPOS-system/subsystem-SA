package model;

/** One line in an order. */
public class OrderItem {

    private int    itemId;
    private String orderId;
    private String productId;
    private String productDescription;  // denormalised for display
    private int    quantity;
    private double unitPrice;
    private double lineTotal;

    public OrderItem() {}

    public OrderItem(String orderId, String productId,
                     String productDescription, int quantity, double unitPrice) {
        this.orderId            = orderId;
        this.productId          = productId;
        this.productDescription = productDescription;
        this.quantity           = quantity;
        this.unitPrice          = unitPrice;
        this.lineTotal          = quantity * unitPrice;
    }

    // Getters
    public int    getItemId()              { return itemId; }
    public String getOrderId()             { return orderId; }
    public String getProductId()           { return productId; }
    public String getProductDescription()  { return productDescription; }
    public int    getQuantity()            { return quantity; }
    public double getUnitPrice()           { return unitPrice; }
    public double getLineTotal()           { return lineTotal; }

    // Setters
    public void setItemId(int id)                  { this.itemId = id; }
    public void setOrderId(String oid)             { this.orderId = oid; }
    public void setProductId(String pid)           { this.productId = pid; }
    public void setProductDescription(String d)    { this.productDescription = d; }
    public void setQuantity(int q)                 { this.quantity = q; recalc(); }
    public void setUnitPrice(double p)             { this.unitPrice = p; recalc(); }
    public void setLineTotal(double t)             { this.lineTotal = t; }

    private void recalc() { lineTotal = quantity * unitPrice; }

    @Override
    public String toString() {
        return productId + " x" + quantity + " @ £" + unitPrice
             + " = £" + lineTotal;
    }
}
