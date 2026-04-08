using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;

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
                Timeout = TimeSpan.FromSeconds(300)
            };
            _client.DefaultRequestHeaders.Add("Accept", "application/json");
        }

        // AUTH

        public async Task<string> PostAuthLoginAsync(string username, string password)
        {
            var payload = new { username, password };
            var response = await _client.PostAsJsonAsync("/api/auth/login", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // ACCOUNTS

        public async Task<string> GetAccountsMeAsync()
        {
            var response = await _client.GetAsync("/api/accounts/me");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetAccountsMerchantsAsync(string? search = null)
        {
            var url = string.IsNullOrEmpty(search)
                ? "/api/accounts/merchants"
                : $"/api/accounts/merchants?search={Uri.EscapeDataString(search)}";
            var response = await _client.GetAsync(url);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetAccountsMerchantByIdAsync(int id)
        {
            var response = await _client.GetAsync($"/api/accounts/merchants/{id}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostAccountsMerchantAsync(string username, string password, string role,
            string companyName, string address, string email, string phone,
            decimal creditLimit, int? discountPlanId = null)
        {
            var payload = new { username, password, role, companyName, address, email, phone, creditLimit, discountPlanId };
            var response = await _client.PostAsJsonAsync("/api/accounts/merchant", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutAccountsMerchantAsync(int id, string companyName, string address,
            string email, string phone, decimal creditLimit, int? discountPlanId = null)
        {
            var payload = new { companyName, address, email, phone, creditLimit, discountPlanId };
            var response = await _client.PutAsJsonAsync($"/api/accounts/merchants/{id}", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutAccountsMerchantRestoreAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/accounts/merchants/{id}/restore", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetAccountsStaffAsync()
        {
            var response = await _client.GetAsync("/api/accounts/staff");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostAccountsStaffAsync(string username, string password, string role)
        {
            var payload = new { username, password, role };
            var response = await _client.PostAsJsonAsync("/api/accounts/staff", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutAccountDeactivateAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/accounts/{id}/deactivate", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutAccountReactivateAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/accounts/{id}/reactivate", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutAccountResetPasswordAsync(int id, string newPassword)
        {
            var payload = new { newPassword };
            var response = await _client.PutAsJsonAsync($"/api/accounts/{id}/reset-password", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // CATALOGUE

        public async Task<string> GetCatalogueAsync(string? search = null)
        {
            var url = string.IsNullOrEmpty(search)
                ? "/api/catalogue"
                : $"/api/catalogue?search={Uri.EscapeDataString(search)}";
            var response = await _client.GetAsync(url);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetCatalogueAdminAsync()
        {
            var response = await _client.GetAsync("/api/catalogue/admin");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetCatalogueAdminByIdAsync(int id)
        {
            var response = await _client.GetAsync($"/api/catalogue/admin/{id}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostCatalogueAsync(string productId, string name, string description,
            decimal price, int availability)
        {
            var payload = new { productId, name, description, price, availability };
            var response = await _client.PostAsJsonAsync("/api/catalogue", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutCatalogueAsync(int id, string name, string description, decimal price)
        {
            var payload = new { name, description, price };
            var response = await _client.PutAsJsonAsync($"/api/catalogue/{id}", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutCatalogueDeactivateAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/catalogue/{id}/deactivate", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutCatalogueReactivateAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/catalogue/{id}/reactivate", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostCatalogueStockAsync(int id, int quantityAdded)
        {
            var payload = new { quantityAdded };
            var response = await _client.PostAsJsonAsync($"/api/catalogue/{id}/stock", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // ORDERS

        public async Task<string> PostOrderAsync(List<OrderItem> items)
        {
            var payload = new { items };
            var response = await _client.PostAsJsonAsync("/api/orders", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetOrderByIdAsync(int id)
        {
            var response = await _client.GetAsync($"/api/orders/{id}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetOrdersByMerchantIdAsync(int merchantId)
        {
            var response = await _client.GetAsync($"/api/orders/merchant/{merchantId}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetOrdersAsync(string? status = null)
        {
            var url = string.IsNullOrEmpty(status)
                ? "/api/orders"
                : $"/api/orders?status={Uri.EscapeDataString(status)}";
            var response = await _client.GetAsync(url);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutOrderStatusAsync(int id, string status)
        {
            var payload = new { status };
            var response = await _client.PutAsJsonAsync($"/api/orders/{id}/status", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutOrderDispatchAsync(int id, string courier, string courierRef,
            string dispatchDate, string expectedDelivery)
        {
            var payload = new { courier, courierRef, dispatchDate, expectedDelivery };
            var response = await _client.PutAsJsonAsync($"/api/orders/{id}/dispatch", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // INVOICES

        public async Task<string> GetInvoiceByIdAsync(string id)
        {
            var response = await _client.GetAsync($"/api/invoices/{id}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetInvoicesByMerchantIdAsync(int merchantId)
        {
            var response = await _client.GetAsync($"/api/invoices/merchant/{merchantId}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetUnpaidInvoicesByMerchantIdAsync(int merchantId)
        {
            var response = await _client.GetAsync($"/api/invoices/merchant/{merchantId}/unpaid");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // PAYMENTS

        public async Task<string> PostPaymentAsync(int merchantId, string invoiceId, decimal amountPaid,
            string paymentMethod, string paymentDate, string? notes = null)
        {
            var payload = new { merchantId, invoiceId, amountPaid, paymentMethod, paymentDate, notes };
            var response = await _client.PostAsJsonAsync("/api/payments", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // DISCOUNT PLANS

        public async Task<string> GetDiscountPlansAsync()
        {
            var response = await _client.GetAsync("/api/discount-plans");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetDiscountPlanByIdAsync(int id)
        {
            var response = await _client.GetAsync($"/api/discount-plans/{id}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostDiscountPlanFixedAsync(string name, decimal discountRate)
        {
            var payload = new { name, discountRate };
            var response = await _client.PostAsJsonAsync("/api/discount-plans/fixed", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PostDiscountPlanFlexibleAsync(string name, List<DiscountTier> tiers)
        {
            var payload = new { name, tiers };
            var response = await _client.PostAsJsonAsync("/api/discount-plans/flexible", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // REPORTS

        public async Task<string> GetReportTurnoverAsync(string from, string to)
        {
            var response = await _client.GetAsync($"/api/reports/turnover?from={from}&to={to}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetReportMerchantSummaryAsync(int merchantId, string from, string to)
        {
            var response = await _client.GetAsync($"/api/reports/merchant-summary?merchantId={merchantId}&from={from}&to={to}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetReportMerchantDetailedAsync(int merchantId, string from, string to)
        {
            var response = await _client.GetAsync($"/api/reports/merchant-detailed?merchantId={merchantId}&from={from}&to={to}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetReportInvoiceListAsync(int merchantId, string from, string to)
        {
            var response = await _client.GetAsync($"/api/reports/invoice-list?merchantId={merchantId}&from={from}&to={to}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetReportAllInvoicesAsync(string from, string to)
        {
            var response = await _client.GetAsync($"/api/reports/all-invoices?from={from}&to={to}");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetReportLowStockAsync()
        {
            var response = await _client.GetAsync("/api/reports/low-stock");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // NOTIFICATIONS

        public async Task<string> GetNotificationsAsync()
        {
            var response = await _client.GetAsync("/api/notifications");
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // COMMERCIAL APPLICATIONS

        public async Task<string> PostCommercialApplicationAsync(string companyName, string companyRegNo,
            string directors, string businessType, string address, string email, string phone)
        {
            var payload = new { companyName, companyRegNo, directors, businessType, address, email, phone };
            var response = await _client.PostAsJsonAsync("/api/applications/commercial", payload);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> GetApplicationsAsync(string? status = null)
        {
            var url = string.IsNullOrEmpty(status)
                ? "/api/applications"
                : $"/api/applications?status={Uri.EscapeDataString(status)}";
            var response = await _client.GetAsync(url);
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutApplicationApproveAsync(int id)
        {
            var response = await _client.PutAsJsonAsync($"/api/applications/{id}/approve", new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        public async Task<string> PutApplicationRejectAsync(int id, string? reason = null)
        {
            var url = string.IsNullOrEmpty(reason)
                ? $"/api/applications/{id}/reject"
                : $"/api/applications/{id}/reject?reason={Uri.EscapeDataString(reason)}";
            var response = await _client.PutAsJsonAsync(url, new { });
            response.EnsureSuccessStatusCode();
            return await response.Content.ReadAsStringAsync();
        }

        // UTILITY

        public void SetBearerToken(string token)
        {
            _client.DefaultRequestHeaders.Authorization =
                new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        }

        public void Dispose() => _client.Dispose();
    }

    public class OrderItem
    {
        public string ProductId { get; set; } = string.Empty;
        public int Quantity { get; set; }
    }

    public class DiscountTier
    {
        public decimal MinSpend { get; set; }
        public decimal DiscountRate { get; set; }
    }
}