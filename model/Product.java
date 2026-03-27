// ── Product.java ─────────────────────────────────────────────────────────────
package model;

public class Product {
    private String  productId;
    private String  description;
    private String  packageType;
    private String  unit;
    private int     unitsPerPack;
    private double  unitPrice;
    private int     availability;
    private int     minStockLevel;
    private double  reorderBufferPct;
    private boolean isActive;

    public Product() {}

    public Product(String productId, String description, String packageType,
                   String unit, int unitsPerPack, double unitPrice,
                   int availability, int minStockLevel, double reorderBufferPct) {
        this.productId        = productId;
        this.description      = description;
        this.packageType      = packageType;
        this.unit             = unit;
        this.unitsPerPack     = unitsPerPack;
        this.unitPrice        = unitPrice;
        this.availability     = availability;
        this.minStockLevel    = minStockLevel;
        this.reorderBufferPct = reorderBufferPct;
        this.isActive         = true;
    }

    public String  getProductId()        { return productId; }
    public String  getDescription()      { return description; }
    public String  getPackageType()      { return packageType; }
    public String  getUnit()             { return unit; }
    public int     getUnitsPerPack()     { return unitsPerPack; }
    public double  getUnitPrice()        { return unitPrice; }
    public int     getAvailability()     { return availability; }
    public int     getMinStockLevel()    { return minStockLevel; }
    public double  getReorderBufferPct() { return reorderBufferPct; }
    public boolean isActive()            { return isActive; }

    public void setProductId(String id)           { this.productId = id; }
    public void setDescription(String d)          { this.description = d; }
    public void setPackageType(String pt)         { this.packageType = pt; }
    public void setUnit(String u)                 { this.unit = u; }
    public void setUnitsPerPack(int n)            { this.unitsPerPack = n; }
    public void setUnitPrice(double p)            { this.unitPrice = p; }
    public void setAvailability(int a)            { this.availability = a; }
    public void setMinStockLevel(int m)           { this.minStockLevel = m; }
    public void setReorderBufferPct(double pct)   { this.reorderBufferPct = pct; }
    public void setActive(boolean active)         { this.isActive = active; }

    public int recommendedOrder() {
        int target = (int) Math.ceil(minStockLevel * (1 + reorderBufferPct / 100.0));
        return Math.max(0, target - availability);
    }

    @Override
    public String toString() {
        return productId + " | " + description + " | £" + unitPrice
             + " | Stock: " + availability;
    }
}
