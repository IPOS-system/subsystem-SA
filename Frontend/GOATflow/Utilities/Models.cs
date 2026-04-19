using Newtonsoft.Json;
using Newtonsoft.Json.Converters;
using System;
using System.Collections.Generic;

namespace GOATflow.Models
{ 
    public class LoginSuccessResponse
    {
        [JsonProperty("userId")] public int UserId { get; set; }
        [JsonProperty("username")] public string Username { get; set; } = "";
        [JsonProperty("role")] public string Role { get; set; } = "";
        [JsonProperty("paymentReminderDue")] public bool PaymentReminderDue { get; set; }
        [JsonProperty("merchantId")] public int? MerchantId { get; set; }
        [JsonProperty("token")] public string Token { get; set; } = "";
    }

    public class LoginErrorResponse
    {
        [JsonProperty("status")] public int Status { get; set; }
        [JsonProperty("error")] public string Error { get; set; } = "";
        [JsonProperty("message")] public string Message { get; set; } = "";
    }
    public class MerchantDTO
    {
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("userId")] public int UserId { get; set; }
        [JsonProperty("username")] public string Username { get; set; } = "";
        [JsonProperty("companyName")] public string CompanyName { get; set; } = "";
        [JsonProperty("address")] public string Address { get; set; } = "";
        [JsonProperty("phone")] public string Phone { get; set; } = "";
        [JsonProperty("fax")] public string? Fax { get; set; }
        [JsonProperty("email")] public string Email { get; set; } = "";
        [JsonProperty("creditLimit")] public decimal CreditLimit { get; set; }
        [JsonProperty("currentBalance")] public decimal CurrentBalance { get; set; }
        [JsonProperty("accountStatus")] public string AccountStatus { get; set; } = "";
        [JsonProperty("isActive")] public bool IsActive { get; set; }
        [JsonProperty("statusChangedAt")] public DateTime? StatusChangedAt { get; set; }
        [JsonProperty("discountPlanId")] public int? DiscountPlanId { get; set; }
        [JsonProperty("discountPlanName")] public string? DiscountPlanName { get; set; }
        [JsonProperty("createdAt")] public DateTime CreatedAt { get; set; }
    }

    public class StaffDTO
    {
        [JsonProperty("userId")] public int UserId { get; set; }
        [JsonProperty("username")] public string Username { get; set; } = "";
        [JsonProperty("role")] public string Role { get; set; } = "";
        [JsonProperty("isActive")] public bool IsActive { get; set; }
        [JsonProperty("createdAt")] public DateTime CreatedAt { get; set; }
    }
    public class CatalogueItemDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("description")] public string Description { get; set; } = "";
        [JsonProperty("packageType")] public string? PackageType { get; set; }
        [JsonProperty("unit")] public string? Unit { get; set; }
        [JsonProperty("unitsPerPack")] public int UnitsPerPack { get; set; }
        [JsonProperty("unitPrice")] public decimal UnitPrice { get; set; }
        [JsonProperty("availability")] public int Availability { get; set; }
    }
    public class AdminCatalogueItemDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("description")] public string Description { get; set; } = "";
        [JsonProperty("packageType")] public string? PackageType { get; set; }
        [JsonProperty("unit")] public string? Unit { get; set; }
        [JsonProperty("unitsPerPack")] public int UnitsPerPack { get; set; }
        [JsonProperty("unitPrice")] public decimal UnitPrice { get; set; }
        [JsonProperty("availability")] public int Availability { get; set; }
        [JsonProperty("isActive")] public bool IsActive { get; set; }
        [JsonProperty("minStockLevel")] public int MinStockLevel { get; set; }
        [JsonProperty("reorderBufferPct")] public decimal ReorderBufferPct { get; set; }
        [JsonProperty("createdAt")] public DateTime CreatedAt { get; set; }
        [JsonProperty("updatedAt")] public DateTime UpdatedAt { get; set; }
    }
    public class OrderDTO
    {
        [JsonProperty("orderId")] public string OrderId { get; set; } = "";
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("merchantName")] public string MerchantName { get; set; } = "";
        [JsonProperty("orderDate")] public DateTime OrderDate { get; set; }
        [JsonProperty("status")] public string Status { get; set; } = "";
        [JsonProperty("subtotal")] public decimal Subtotal { get; set; }
        [JsonProperty("discountAmount")] public decimal DiscountAmount { get; set; }
        [JsonProperty("totalAmount")] public decimal TotalAmount { get; set; }
        [JsonProperty("items")] public List<OrderItemDTO> Items { get; set; } = new();
        [JsonProperty("dispatchedByUsername")] public string? DispatchedByUsername { get; set; }
        [JsonProperty("dispatchDate")] public DateTime? DispatchDate { get; set; }
        [JsonProperty("courier")] public string? Courier { get; set; }
        [JsonProperty("courierRef")] public string? CourierRef { get; set; }
        [JsonProperty("expectedDelivery")] public DateTime? ExpectedDelivery { get; set; }
    }
    public class OrderItemDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("description")] public string Description { get; set; } = "";
        [JsonProperty("quantity")] public int Quantity { get; set; }
        [JsonProperty("unitPrice")] public decimal UnitPrice { get; set; }
        [JsonProperty("lineTotal")] public decimal LineTotal { get; set; }
    }

    public class InvoiceDTO
    {
        [JsonProperty("invoiceId")] public string InvoiceId { get; set; } = "";
        [JsonProperty("orderId")] public string OrderId { get; set; } = "";
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("merchantName")] public string MerchantName { get; set; } = "";
        [JsonProperty("invoiceDate")] public DateTime InvoiceDate { get; set; }
        [JsonProperty("dueDate")] public string DueDate { get; set; } = ""; // LocalDate → string
        [JsonProperty("amountDue")] public decimal AmountDue { get; set; }
        [JsonProperty("totalPaid")] public decimal TotalPaid { get; set; }
        [JsonProperty("paymentStatus")] public string PaymentStatus { get; set; } = "";
        [JsonProperty("payments")] public List<PaymentSummaryDTO> Payments { get; set; } = new();
    }

    public class PaymentSummaryDTO
    {
        [JsonProperty("paymentId")] public int PaymentId { get; set; }
        [JsonProperty("amountPaid")] public decimal AmountPaid { get; set; }
        [JsonProperty("paymentMethod")] public string PaymentMethod { get; set; } = "";
        [JsonProperty("paymentDate")] public DateTime PaymentDate { get; set; }
        [JsonProperty("notes")] public string? Notes { get; set; }
    }

    public class DiscountPlanDTO
    {
        [JsonProperty("planId")] public int PlanId { get; set; }
        [JsonProperty("planName")] public string PlanName { get; set; } = "";
        [JsonProperty("planType")] public string PlanType { get; set; } = ""; // "FIXED" | "FLEXIBLE"
        [JsonProperty("fixedRate")] public decimal? FixedRate { get; set; }
        [JsonProperty("tiers")] public List<DiscountTierDTO> Tiers { get; set; } = new();

        public string Summary => PlanType == "FIXED"
            ? $"{FixedRate}% flat"
            : $"{Tiers?.Count ?? 0} tier(s)";
    }

    public class DiscountTierDTO
    {
        [JsonProperty("tierId")] public int TierId { get; set; }
        [JsonProperty("minOrderVal")] public decimal MinOrderVal { get; set; }
        [JsonProperty("maxOrderVal")] public decimal? MaxOrderVal { get; set; }
        [JsonProperty("discountRate")] public decimal DiscountRate { get; set; }
    }

    public class NotificationDTO
    {
        [JsonProperty("lowStockWarning")] public bool LowStockWarning { get; set; }
        [JsonProperty("lowStockCount")] public int LowStockCount { get; set; }
        [JsonProperty("defaultedMerchants")] public List<DefaultedMerchantDTO> DefaultedMerchants { get; set; } = new();
    }

    public class DefaultedMerchantDTO
    {
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("companyName")] public string CompanyName { get; set; } = "";
        [JsonProperty("accountStatus")] public string AccountStatus { get; set; } = "";
    }

   
    public class AuditLogDTO
    {
        [JsonProperty("logId")] public int LogId { get; set; }
        [JsonProperty("userId")] public int UserId { get; set; }
        [JsonProperty("username")] public string Username { get; set; } = "";
        [JsonProperty("action")] public string Action { get; set; } = "";
        [JsonProperty("targetType")] public string? TargetType { get; set; }
        [JsonProperty("targetId")] public string? TargetId { get; set; }
        [JsonProperty("details")] public string? Details { get; set; }
        [JsonProperty("loggedAt")] public DateTime LoggedAt { get; set; }
    }

   
    public class TurnoverReportDTO
    {
        [JsonProperty("from")] public DateTime From { get; set; }
        [JsonProperty("to")] public DateTime To { get; set; }
        [JsonProperty("totalRevenue")] public decimal TotalRevenue { get; set; }
        [JsonProperty("lines")] public List<ProductTurnoverDTO> Lines { get; set; } = new();
    }

    public class ProductTurnoverDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("description")] public string Description { get; set; } = "";
        [JsonProperty("totalQuantitySold")] public int TotalQuantitySold { get; set; }
        [JsonProperty("totalRevenue")] public decimal TotalRevenue { get; set; }
    }

    public class MerchantOrdersSummaryDTO
    {
        [JsonProperty("from")] public DateTime From { get; set; }
        [JsonProperty("to")] public DateTime To { get; set; }
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("companyName")] public string CompanyName { get; set; } = "";
        [JsonProperty("address")] public string? Address { get; set; }
        [JsonProperty("phone")] public string? Phone { get; set; }
        [JsonProperty("fax")] public string? Fax { get; set; }
        [JsonProperty("orders")] public List<OrderSummaryLineDTO> Orders { get; set; } = new();
        [JsonProperty("totalOrders")] public int TotalOrders { get; set; }
        [JsonProperty("totalValue")] public decimal TotalValue { get; set; }
        [JsonProperty("totalDispatched")] public int TotalDispatched { get; set; }
        [JsonProperty("totalDelivered")] public int TotalDelivered { get; set; }
        [JsonProperty("totalPaid")] public int TotalPaid { get; set; }
    }

    public class OrderSummaryLineDTO
    {
        [JsonProperty("orderId")] public string OrderId { get; set; } = "";
        [JsonProperty("orderedDate")] public DateTime OrderedDate { get; set; }
        [JsonProperty("amount")] public decimal Amount { get; set; }
        [JsonProperty("dispatchedDate")] public DateTime? DispatchedDate { get; set; }
        [JsonProperty("deliveredDate")] public DateTime? DeliveredDate { get; set; }
        [JsonProperty("paymentStatus")] public string PaymentStatus { get; set; } = "";
    }

    public class MerchantDetailedReportDTO
    {
        [JsonProperty("from")] public DateTime From { get; set; }
        [JsonProperty("to")] public DateTime To { get; set; }
        [JsonProperty("merchantId")] public int MerchantId { get; set; }
        [JsonProperty("companyName")] public string CompanyName { get; set; } = "";
        [JsonProperty("address")] public string? Address { get; set; }
        [JsonProperty("phone")] public string? Phone { get; set; }
        [JsonProperty("fax")] public string? Fax { get; set; }
        [JsonProperty("email")] public string? Email { get; set; }
        [JsonProperty("orders")] public List<DetailedOrderDTO> Orders { get; set; } = new();
        [JsonProperty("grandTotal")] public decimal GrandTotal { get; set; }
    }

    public class DetailedOrderDTO
    {
        [JsonProperty("orderId")] public string OrderId { get; set; } = "";
        [JsonProperty("orderTotal")] public decimal OrderTotal { get; set; }
        [JsonProperty("orderedDate")] public DateTime OrderedDate { get; set; }
        [JsonProperty("discountAmount")] public decimal DiscountAmount { get; set; }
        [JsonProperty("paymentStatus")] public string PaymentStatus { get; set; } = "";
        [JsonProperty("items")] public List<LineItemDTO> Items { get; set; } = new();
    }

    public class LineItemDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("quantity")] public int Quantity { get; set; }
        [JsonProperty("unitCost")] public decimal UnitCost { get; set; }
        [JsonProperty("amount")] public decimal Amount { get; set; }
    }

    public class InvoiceListReportDTO
    {
        [JsonProperty("from")] public DateTime From { get; set; }
        [JsonProperty("to")] public DateTime To { get; set; }
        [JsonProperty("invoices")] public List<InvoiceLineDTO> Invoices { get; set; } = new();
    }

    public class InvoiceLineDTO
    {
        [JsonProperty("invoiceId")] public string InvoiceId { get; set; } = "";
        [JsonProperty("orderId")] public string OrderId { get; set; } = "";
        [JsonProperty("merchantId")] public int? MerchantId { get; set; }
        [JsonProperty("merchantName")] public string? MerchantName { get; set; }
        [JsonProperty("invoiceDate")] public DateTime InvoiceDate { get; set; }
        [JsonProperty("dueDate")] public string DueDate { get; set; } = "";
        [JsonProperty("amountDue")] public decimal AmountDue { get; set; }
        [JsonProperty("totalPaid")] public decimal TotalPaid { get; set; }
        [JsonProperty("paymentStatus")] public string PaymentStatus { get; set; } = "";
    }

    public class LowStockReportDTO
    {
        [JsonProperty("generatedAt")] public DateTime GeneratedAt { get; set; }
        [JsonProperty("items")] public List<LowStockLineDTO> Items { get; set; } = new();
    }

    public class LowStockLineDTO
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("description")] public string Description { get; set; } = "";
        [JsonProperty("availability")] public int Availability { get; set; }
        [JsonProperty("minStockLevel")] public int MinStockLevel { get; set; }
        [JsonProperty("recommendedOrderQty")] public int RecommendedOrderQty { get; set; }
    }

   
    public class BasketItem
    {
        public string ProductId { get; set; } = "";
        public string Description { get; set; } = "";
        public decimal UnitPrice { get; set; }
        public int Quantity { get; set; }
        public decimal LineTotal => UnitPrice * Quantity;
    }
}
