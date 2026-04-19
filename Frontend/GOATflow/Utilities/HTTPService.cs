using GOATflow.Models;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;
using System.Windows;

namespace GOATflow.Utilities
{
    public class HttpService : IDisposable
    {
        private readonly HttpClient _client;
        private const string BaseUrl = "http://localhost:8080";

        public HttpService()
        {
            _client = new HttpClient
            {
                BaseAddress = new Uri(BaseUrl),
                Timeout = TimeSpan.FromSeconds(30)
            };
            _client.DefaultRequestHeaders.Add("Accept", "application/json");
        }

        // ── AUTH ─────────────────────────────────────────────────────────────

        /// POST /api/auth/login
        /// Returns true on success and populates Instance.*
        public async Task<(bool success, string errorMessage)> PostAuthLoginAsync(string username, string password)
        {
            var payload = new { username, password };
            var response = await _client.PostAsJsonAsync("/api/auth/login", payload);
            var body = await response.Content.ReadAsStringAsync();

            if (response.IsSuccessStatusCode)
            {
                var success = JsonConvert.DeserializeObject<LoginSuccessResponse>(body)!;
                Instance.UserID = success.UserId.ToString();
                Instance.Username = success.Username;
                Instance.Role = success.Role;
                Instance.PaymentReminderDueToken = success.PaymentReminderDue.ToString();
                Instance.MerchantID = success.MerchantId?.ToString() ?? "";
                Instance.Token = success.Token;
                SetBearerToken(success.Token);
                return (true, "");
            }
            else
            {
                var err = JsonConvert.DeserializeObject<LoginErrorResponse>(body);
                return (false, err?.Message ?? "Invalid credentials.");
            }
        }

        // ── ACCOUNTS ─────────────────────────────────────────────────────────

        /// GET /api/accounts/me  — MERCHANT own profile
        public async Task<MerchantDTO> GetAccountsMeAsync()
        {
            var json = await GetStringAsync("/api/accounts/me");
            return JsonConvert.DeserializeObject<MerchantDTO>(json)!;
        }

        /// GET /api/accounts/merchants[?search=]
        public async Task<List<MerchantDTO>> GetMerchantsAsync(string? search = null)
        {
            var url = string.IsNullOrWhiteSpace(search)
                ? "/api/accounts/merchants"
                : $"/api/accounts/merchants?search={Uri.EscapeDataString(search)}";
            var json = await GetStringAsync(url);
            return JsonConvert.DeserializeObject<List<MerchantDTO>>(json) ?? new();
        }

        /// GET /api/accounts/merchants/{id}
        public async Task<MerchantDTO> GetMerchantByIdAsync(int id)
        {
            var json = await GetStringAsync($"/api/accounts/merchants/{id}");
            return JsonConvert.DeserializeObject<MerchantDTO>(json)!;
        }

        /// POST /api/accounts/merchant
        /// discountPlanId is @NotNull on the server — must be provided
        public async Task<MerchantDTO> PostMerchantAsync(
            string username, string password, string companyName,
            string address, string phone, string? fax, string email,
            decimal creditLimit, int discountPlanId)
        {
            var payload = new
            {
                username,
                password,
                companyName,
                address,
                phone,
                fax,
                email,
                creditLimit,
                discountPlanId
            };
            var json = await PostJsonAsync("/api/accounts/merchant", payload);
            return JsonConvert.DeserializeObject<MerchantDTO>(json)!;
        }

        /// PUT /api/accounts/merchants/{id}  — all fields optional (null = unchanged)
        public async Task<MerchantDTO> PutMerchantAsync(
            int id, string? companyName = null, string? address = null,
            string? phone = null, string? fax = null, string? email = null,
            decimal? creditLimit = null, int? discountPlanId = null)
        {
            var payload = new { companyName, address, phone, fax, email, creditLimit, discountPlanId };
            var json = await PutJsonAsync($"/api/accounts/merchants/{id}", payload);
            return JsonConvert.DeserializeObject<MerchantDTO>(json)!;
        }

        /// PUT /api/accounts/merchants/{id}/restore
        public async Task<MerchantDTO> PutMerchantRestoreAsync(int id)
        {
            var json = await PutJsonAsync($"/api/accounts/merchants/{id}/restore", new { });
            return JsonConvert.DeserializeObject<MerchantDTO>(json)!;
        }

        /// GET /api/accounts/staff
        public async Task<List<StaffDTO>> GetStaffAsync()
        {
            var json = await GetStringAsync("/api/accounts/staff");
            return JsonConvert.DeserializeObject<List<StaffDTO>>(json) ?? new();
        }

        /// POST /api/accounts/staff  — role: ADMIN | MANAGER | ACCOUNTANT | DIRECTOR
        public async Task<StaffDTO> PostStaffAsync(string username, string password, string role)
        {
            var payload = new { username, password, role };
            var json = await PostJsonAsync("/api/accounts/staff", payload);
            return JsonConvert.DeserializeObject<StaffDTO>(json)!;
        }

        /// PUT /api/accounts/{id}/deactivate  — returns 204
        public async Task PutDeactivateAsync(int id)
        {
            var r = await _client.PutAsJsonAsync($"/api/accounts/{id}/deactivate", new { });
            r.EnsureSuccessStatusCode();
        }

        /// PUT /api/accounts/{id}/reactivate  — returns 204
        public async Task PutReactivateAsync(int id)
        {
            var r = await _client.PutAsJsonAsync($"/api/accounts/{id}/reactivate", new { });
            r.EnsureSuccessStatusCode();
        }

        /// PUT /api/accounts/{id}/reset-password  — returns 204
        public async Task PutResetPasswordAsync(int id, string newPassword)
        {
            var r = await _client.PutAsJsonAsync($"/api/accounts/{id}/reset-password",
                new { newPassword });
            r.EnsureSuccessStatusCode();
        }

        /// PUT /api/accounts/{id}/change-role?newRole=  — returns StaffDTO
        public async Task<StaffDTO> PutChangeRoleAsync(int id, string newRole)
        {
            var r = await _client.PutAsJsonAsync(
                $"/api/accounts/{id}/change-role?newRole={Uri.EscapeDataString(newRole)}", new { });
            var body = await r.Content.ReadAsStringAsync();
            r.EnsureSuccessStatusCode();
            return JsonConvert.DeserializeObject<StaffDTO>(body)!;
        }

        // ── CATALOGUE ────────────────────────────────────────────────────────

        /// GET /api/catalogue[?search=]  — active products, merchant-facing
        public async Task<List<CatalogueItemDTO>> GetCatalogueAsync(string? search = null)
        {
            var url = string.IsNullOrWhiteSpace(search)
                ? "/api/catalogue"
                : $"/api/catalogue?search={Uri.EscapeDataString(search)}";
            var json = await GetStringAsync(url);
            return JsonConvert.DeserializeObject<List<CatalogueItemDTO>>(json) ?? new();
        }

        /// GET /api/catalogue/admin  — all products including inactive
        public async Task<List<AdminCatalogueItemDTO>> GetCatalogueAdminAsync()
        {
            var json = await GetStringAsync("/api/catalogue/admin");
            return JsonConvert.DeserializeObject<List<AdminCatalogueItemDTO>>(json) ?? new();
        }

        /// GET /api/catalogue/admin/{productId}
        public async Task<AdminCatalogueItemDTO> GetCatalogueAdminByIdAsync(string productId)
        {
            var json = await GetStringAsync($"/api/catalogue/admin/{Uri.EscapeDataString(productId)}");
            return JsonConvert.DeserializeObject<AdminCatalogueItemDTO>(json)!;
        }

        /// POST /api/catalogue
        public async Task<AdminCatalogueItemDTO> PostCatalogueAsync(
            string productId, string description, string? packageType, string? unit,
            int unitsPerPack, decimal unitPrice, int availability,
            int minStockLevel, decimal reorderBufferPct = 10m)
        {
            var payload = new
            {
                productId,
                description,
                packageType,
                unit,
                unitsPerPack,
                unitPrice,
                availability,
                minStockLevel,
                reorderBufferPct
            };
            var json = await PostJsonAsync("/api/catalogue", payload);
            return JsonConvert.DeserializeObject<AdminCatalogueItemDTO>(json)!;
        }

        /// PUT /api/catalogue/{productId}  — all fields optional
        public async Task<AdminCatalogueItemDTO> PutCatalogueAsync(
            string productId, string? description = null, string? packageType = null,
            string? unit = null, int? unitsPerPack = null, decimal? unitPrice = null,
            int? minStockLevel = null, decimal? reorderBufferPct = null)
        {
            var payload = new
            {
                description,
                packageType,
                unit,
                unitsPerPack,
                unitPrice,
                minStockLevel,
                reorderBufferPct
            };
            var json = await PutJsonAsync($"/api/catalogue/{Uri.EscapeDataString(productId)}", payload);
            return JsonConvert.DeserializeObject<AdminCatalogueItemDTO>(json)!;
        }

        /// PUT /api/catalogue/{productId}/deactivate  — 204
        public async Task PutCatalogueDeactivateAsync(string productId)
        {
            var r = await _client.PutAsJsonAsync(
                $"/api/catalogue/{Uri.EscapeDataString(productId)}/deactivate", new { });
            r.EnsureSuccessStatusCode();
        }

        /// PUT /api/catalogue/{productId}/reactivate  — 204
        public async Task PutCatalogueReactivateAsync(string productId)
        {
            var r = await _client.PutAsJsonAsync(
                $"/api/catalogue/{Uri.EscapeDataString(productId)}/reactivate", new { });
            r.EnsureSuccessStatusCode();
        }

        /// POST /api/catalogue/{productId}/stock  — field is "quantity" not "quantityAdded"
        public async Task<AdminCatalogueItemDTO> PostCatalogueStockAsync(string productId, int quantity)
        {
            var json = await PostJsonAsync(
                $"/api/catalogue/{Uri.EscapeDataString(productId)}/stock",
                new { quantity });
            return JsonConvert.DeserializeObject<AdminCatalogueItemDTO>(json)!;
        }

        // ── ORDERS ───────────────────────────────────────────────────────────

        /// POST /api/orders — items use productId (String) + quantity (int)
        public async Task<OrderDTO> PostOrderAsync(List<OrderLineRequest> items)
        {
            var json = await PostJsonAsync("/api/orders", new { items });
            return JsonConvert.DeserializeObject<OrderDTO>(json)!;
        }

        /// GET /api/orders/{orderId}  — orderId is a String e.g. "ORD-20260406-0001"
        public async Task<OrderDTO> GetOrderByIdAsync(string orderId)
        {
            var json = await GetStringAsync($"/api/orders/{Uri.EscapeDataString(orderId)}");
            return JsonConvert.DeserializeObject<OrderDTO>(json)!;
        }

        /// GET /api/orders/merchant/{merchantId}
        public async Task<List<OrderDTO>> GetOrdersByMerchantAsync(int merchantId)
        {
            var json = await GetStringAsync($"/api/orders/merchant/{merchantId}");
            return JsonConvert.DeserializeObject<List<OrderDTO>>(json) ?? new();
        }

        /// GET /api/orders[?status=]
        public async Task<List<OrderDTO>> GetAllOrdersAsync(string? status = null)
        {
            var url = string.IsNullOrWhiteSpace(status)
                ? "/api/orders"
                : $"/api/orders?status={Uri.EscapeDataString(status)}";
            var json = await GetStringAsync(url);
            return JsonConvert.DeserializeObject<List<OrderDTO>>(json) ?? new();
        }

        /// PUT /api/orders/{orderId}/status  — body: { "status": "PROCESSING" }
        public async Task<OrderDTO> PutOrderStatusAsync(string orderId, string status)
        {
            var json = await PutJsonAsync(
                $"/api/orders/{Uri.EscapeDataString(orderId)}/status",
                new { status });
            return JsonConvert.DeserializeObject<OrderDTO>(json)!;
        }

        /// PUT /api/orders/{orderId}/dispatch
        /// dispatchDate and expectedDelivery must be ISO 8601 e.g. "2026-04-06T10:00:00"
        public async Task<OrderDTO> PutOrderDispatchAsync(
            string orderId, string courier, string courierRef,
            string dispatchDate, string expectedDelivery)
        {
            var payload = new { courier, courierRef, dispatchDate, expectedDelivery };
            var json = await PutJsonAsync(
                $"/api/orders/{Uri.EscapeDataString(orderId)}/dispatch", payload);
            return JsonConvert.DeserializeObject<OrderDTO>(json)!;
        }

        // ── INVOICES ─────────────────────────────────────────────────────────

        /// GET /api/invoices/{invoiceId}
        public async Task<InvoiceDTO> GetInvoiceByIdAsync(string invoiceId)
        {
            var json = await GetStringAsync($"/api/invoices/{Uri.EscapeDataString(invoiceId)}");
            return JsonConvert.DeserializeObject<InvoiceDTO>(json)!;
        }

        /// GET /api/invoices/merchant/{merchantId}
        public async Task<List<InvoiceDTO>> GetInvoicesByMerchantAsync(int merchantId)
        {
            var json = await GetStringAsync($"/api/invoices/merchant/{merchantId}");
            return JsonConvert.DeserializeObject<List<InvoiceDTO>>(json) ?? new();
        }

        /// GET /api/invoices/merchant/{merchantId}/unpaid
        public async Task<List<InvoiceDTO>> GetUnpaidInvoicesAsync(int merchantId)
        {
            var json = await GetStringAsync($"/api/invoices/merchant/{merchantId}/unpaid");
            return JsonConvert.DeserializeObject<List<InvoiceDTO>>(json) ?? new();
        }

        // ── PAYMENTS ─────────────────────────────────────────────────────────

        /// POST /api/payments  — returns updated InvoiceDTO (201)
        /// paymentDate must be ISO 8601 string e.g. "2026-04-06T14:00:00"
        /// paymentMethod: "BANK_TRANSFER" | "CARD" | "CHEQUE"
        public async Task<InvoiceDTO> PostPaymentAsync(
            int merchantId, string invoiceId, decimal amountPaid,
            string paymentMethod, string paymentDate, string? notes = null)
        {
            var payload = new { merchantId, invoiceId, amountPaid, paymentMethod, paymentDate, notes };
            var json = await PostJsonAsync("/api/payments", payload);
            return JsonConvert.DeserializeObject<InvoiceDTO>(json)!;
        }

        // ── DISCOUNT PLANS ───────────────────────────────────────────────────

        /// GET /api/discount-plans
        public async Task<List<DiscountPlanDTO>> GetDiscountPlansAsync()
        {
            var json = await GetStringAsync("/api/discount-plans");
            return JsonConvert.DeserializeObject<List<DiscountPlanDTO>>(json) ?? new();
        }

        /// GET /api/discount-plans/{id}
        public async Task<DiscountPlanDTO> GetDiscountPlanByIdAsync(int id)
        {
            var json = await GetStringAsync($"/api/discount-plans/{id}");
            return JsonConvert.DeserializeObject<DiscountPlanDTO>(json)!;
        }

        /// POST /api/discount-plans/fixed  — field is "planName" + "fixedRate"
        public async Task<DiscountPlanDTO> PostFixedPlanAsync(string planName, decimal fixedRate)
        {
            var json = await PostJsonAsync("/api/discount-plans/fixed", new { planName, fixedRate });
            return JsonConvert.DeserializeObject<DiscountPlanDTO>(json)!;
        }

        /// POST /api/discount-plans/flexible  — tiers: [{minOrderVal, maxOrderVal?, discountRate}]
        public async Task<DiscountPlanDTO> PostFlexiblePlanAsync(
            string planName, List<FlexTierRequest> tiers)
        {
            var json = await PostJsonAsync("/api/discount-plans/flexible", new { planName, tiers });
            return JsonConvert.DeserializeObject<DiscountPlanDTO>(json)!;
        }

        // ── REPORTS ──────────────────────────────────────────────────────────

        public async Task<TurnoverReportDTO> GetReportTurnoverAsync(string from, string to)
        {
            var json = await GetStringAsync($"/api/reports/turnover?from={from}&to={to}");
            return JsonConvert.DeserializeObject<TurnoverReportDTO>(json)!;
        }

        public async Task<MerchantOrdersSummaryDTO> GetReportMerchantSummaryAsync(
            int merchantId, string from, string to)
        {
            var json = await GetStringAsync(
                $"/api/reports/merchant-summary?merchantId={merchantId}&from={from}&to={to}");
            return JsonConvert.DeserializeObject<MerchantOrdersSummaryDTO>(json)!;
        }

        public async Task<MerchantDetailedReportDTO> GetReportMerchantDetailedAsync(
            int merchantId, string from, string to)
        {
            var json = await GetStringAsync(
                $"/api/reports/merchant-detailed?merchantId={merchantId}&from={from}&to={to}");
            return JsonConvert.DeserializeObject<MerchantDetailedReportDTO>(json)!;
        }

        public async Task<InvoiceListReportDTO> GetReportInvoiceListAsync(
            int merchantId, string from, string to)
        {
            var json = await GetStringAsync(
                $"/api/reports/invoice-list?merchantId={merchantId}&from={from}&to={to}");
            return JsonConvert.DeserializeObject<InvoiceListReportDTO>(json)!;
        }

        public async Task<InvoiceListReportDTO> GetReportAllInvoicesAsync(string from, string to)
        {
            var json = await GetStringAsync($"/api/reports/all-invoices?from={from}&to={to}");
            return JsonConvert.DeserializeObject<InvoiceListReportDTO>(json)!;
        }

        public async Task<LowStockReportDTO> GetReportLowStockAsync()
        {
            var json = await GetStringAsync("/api/reports/low-stock");
            return JsonConvert.DeserializeObject<LowStockReportDTO>(json)!;
        }

        // ── NOTIFICATIONS ────────────────────────────────────────────────────

        public async Task<NotificationDTO> GetNotificationsAsync()
        {
            var json = await GetStringAsync("/api/notifications");
            return JsonConvert.DeserializeObject<NotificationDTO>(json)!;
        }

        // ── AUDIT LOG ────────────────────────────────────────────────────────

        public async Task<List<AuditLogDTO>> GetAuditLogAsync(
            string? userId = null, string? targetType = null,
            string? targetId = null, string? from = null, string? to = null)
        {
            var parts = new List<string>();
            if (!string.IsNullOrWhiteSpace(userId)) parts.Add($"userId={userId}");
            if (!string.IsNullOrWhiteSpace(targetType)) parts.Add($"targetType={Uri.EscapeDataString(targetType)}");
            if (!string.IsNullOrWhiteSpace(targetId)) parts.Add($"targetId={Uri.EscapeDataString(targetId)}");
            if (!string.IsNullOrWhiteSpace(from)) parts.Add($"from={from}");
            if (!string.IsNullOrWhiteSpace(to)) parts.Add($"to={to}");
            var qs = parts.Count > 0 ? "?" + string.Join("&", parts) : "";
            var json = await GetStringAsync("/api/audit-log" + qs);
            return JsonConvert.DeserializeObject<List<AuditLogDTO>>(json) ?? new();
        }

        // ── UTILITY ──────────────────────────────────────────────────────────

        public void SetBearerToken(string token)
        {
            _client.DefaultRequestHeaders.Authorization =
                new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        public void Dispose() => _client.Dispose();

        // ── Private helpers ──────────────────────────────────────────────────

        private async Task<string> GetStringAsync(string url)
        {
            var r = await _client.GetAsync(url);
            var body = await r.Content.ReadAsStringAsync();
            if (!r.IsSuccessStatusCode)
                throw new HttpRequestException(ExtractError(body, (int)r.StatusCode));
            return body;
        }

        private async Task<string> PostJsonAsync(string url, object payload)
        {
            var r = await _client.PostAsJsonAsync(url, payload);
            var body = await r.Content.ReadAsStringAsync();
            if (!r.IsSuccessStatusCode)
                throw new HttpRequestException(ExtractError(body, (int)r.StatusCode));
            return body;
        }

        private async Task<string> PutJsonAsync(string url, object payload)
        {
            var r = await _client.PutAsJsonAsync(url, payload);
            var body = await r.Content.ReadAsStringAsync();
            if (!r.IsSuccessStatusCode)
                throw new HttpRequestException(ExtractError(body, (int)r.StatusCode));
            return body;
        }

        /// Extracts the human-readable message from ApiErrorResponse, or falls back to status code.
        private static string ExtractError(string body, int status)
        {
            try
            {
                var err = JsonConvert.DeserializeObject<LoginErrorResponse>(body);
                if (!string.IsNullOrWhiteSpace(err?.Message)) return err.Message;
            }
            catch { }
            return $"HTTP {status}";
        }
    }

    // ── Request helpers used by HttpService ──────────────────────────────────

    public class OrderLineRequest
    {
        [JsonProperty("productId")] public string ProductId { get; set; } = "";
        [JsonProperty("quantity")] public int Quantity { get; set; }
    }

    public class FlexTierRequest
    {
        [JsonProperty("minOrderVal")] public decimal MinOrderVal { get; set; }
        [JsonProperty("maxOrderVal")] public decimal? MaxOrderVal { get; set; }
        [JsonProperty("discountRate")] public decimal DiscountRate { get; set; }
    }
}
