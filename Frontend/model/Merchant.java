package model;


public class Merchant {

    public enum AccountStatus { NORMAL, SUSPENDED, IN_DEFAULT }

    private int           merchantId;
    private int           userId;           // FK → users.user_id
    private String        companyName;
    private String        address;
    private String        phone;
    private String        fax;
    private String        email;
    private double        creditLimit;
    private double        currentBalance;
    private AccountStatus accountStatus;
    private Integer       discountPlanId;  // nullable


    public Merchant() {}

    public Merchant(int merchantId, int userId, String companyName,
                    String address, String phone, String fax, String email,
                    double creditLimit, double currentBalance,
                    AccountStatus accountStatus, Integer discountPlanId) {
        this.merchantId    = merchantId;
        this.userId        = userId;
        this.companyName   = companyName;
        this.address       = address;
        this.phone         = phone;
        this.fax           = fax;
        this.email         = email;
        this.creditLimit   = creditLimit;
        this.currentBalance= currentBalance;
        this.accountStatus = accountStatus;
        this.discountPlanId= discountPlanId;
    }


    public int           getMerchantId()     { return merchantId; }
    public int           getUserId()         { return userId; }
    public String        getCompanyName()    { return companyName; }
    public String        getAddress()        { return address; }
    public String        getPhone()          { return phone; }
    public String        getFax()            { return fax; }
    public String        getEmail()          { return email; }
    public double        getCreditLimit()    { return creditLimit; }
    public double        getCurrentBalance() { return currentBalance; }
    public AccountStatus getAccountStatus()  { return accountStatus; }
    public Integer       getDiscountPlanId() { return discountPlanId; }

    public void setMerchantId(int id)               { this.merchantId = id; }
    public void setUserId(int uid)                   { this.userId = uid; }
    public void setCompanyName(String name)          { this.companyName = name; }
    public void setAddress(String addr)              { this.address = addr; }
    public void setPhone(String phone)               { this.phone = phone; }
    public void setFax(String fax)                   { this.fax = fax; }
    public void setEmail(String email)               { this.email = email; }
    public void setCreditLimit(double limit)         { this.creditLimit = limit; }
    public void setCurrentBalance(double balance)    { this.currentBalance = balance; }
    public void setAccountStatus(AccountStatus s)    { this.accountStatus = s; }
    public void setDiscountPlanId(Integer planId)    { this.discountPlanId = planId; }

    public boolean canPlaceOrders() {
        return accountStatus == AccountStatus.NORMAL;
    }

    @Override
    public String toString() {
        return "Merchant{id=" + merchantId + ", company='" + companyName
             + "', status=" + accountStatus + ", balance=" + currentBalance + "}";
    }
}
