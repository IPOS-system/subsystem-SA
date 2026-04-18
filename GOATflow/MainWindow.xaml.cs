using GOATflow.Models;
using GOATflow.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;

namespace GOATflow
{
    public partial class MainWindow : Window
    {
        private readonly List<BasketItem> _basket = new();
        private MerchantDTO? _myProfile;

        public MainWindow()
        {
            InitializeComponent();

            RadioMerchantForm.Checked += AccountFormToggle;
            RadioStaffForm.Checked += AccountFormToggle;
            RadioFixed.Checked += DiscountTypeToggle;
            RadioFlexible.Checked += DiscountTypeToggle;

            ApplyRoleNav();
            _ = LoadInitialDataAsync();
        }

        #region Navigation

        private void ApplyRoleNav()
        {
            UserLabel.Text = $"{Instance.Username}\n({Instance.Role})";

            if (Instance.PaymentReminderDueToken == "True")
                PaymentReminderBanner.Visibility = Visibility.Visible;

            switch (Instance.Role)
            {
                case "MERCHANT":
                    Show(NavAccountInfo, NavCatalogue, NavMyOrders, NavMyBalance);
                    break;

                case "ADMIN":
                    Show(NavManageAccounts, NavCatalogueMgmt, NavOrders,
                         NavDiscountPlans, NavRecordPayment, NavReports, NavAuditLog);
                    NotifPanel.Visibility = Visibility.Visible;
                    break;

                case "MANAGER":
                    Show(NavManageAccounts, NavReports);
                    NotifPanel.Visibility = Visibility.Visible;
                    StaffTab.Visibility = Visibility.Collapsed;
                    CreateAccountTab.Visibility = Visibility.Collapsed;
                    break;
            }
        }

        private void NavClick(object sender, RoutedEventArgs e)
        {
            switch (((Button)sender).Name)
            {
                case "NavAccountInfo": ShowPanel(PanelAccountInfo); _ = LoadAccountInfoAsync(); break;
                case "NavCatalogue": ShowPanel(PanelCatalogue); _ = LoadCatalogueAsync(); break;
                case "BackToCatalogue": ShowPanel(PanelCatalogue); _ = LoadCatalogueAsync(); break;
                case "NavMyOrders": ShowPanel(PanelMyOrders); _ = LoadMyOrdersAsync(); break;
                case "NavMyBalance": ShowPanel(PanelMyBalance); _ = LoadMyBalanceAsync(); break;
                case "NavManageAccounts": ShowPanel(PanelManageAccounts); _ = LoadMerchantsAsync(); break;
                case "NavCatalogueMgmt": ShowPanel(PanelCatalogueMgmt); _ = LoadCatMgmtAsync(); break;
                case "NavOrders": ShowPanel(PanelOrders); break;
                case "NavDiscountPlans": ShowPanel(PanelDiscountPlans); _ = LoadDiscountPlansAsync(); break;
                case "NavRecordPayment": ShowPanel(PanelRecordPayment); break;
                case "NavReports": ShowPanel(PanelReports); break;
                case "NavAuditLog": ShowPanel(PanelAuditLog); break;
                case "NavLogout": Logout(); break;
            }
        }

        private void NavToBasket(object sender, RoutedEventArgs e)
        {
            ShowPanel(PanelBasket);
            RefreshBasket();
        }

        private void ShowPanel(UIElement panel)
        {
            UIElement[] all =
            {
                PanelAccountInfo, PanelCatalogue, PanelBasket, PanelMyOrders, PanelMyBalance,
                PanelManageAccounts, PanelCatalogueMgmt, PanelOrders, PanelDiscountPlans,
                PanelRecordPayment, PanelReports, PanelAuditLog
            };

            foreach (var p in all)
                p.Visibility = Visibility.Collapsed;

            panel.Visibility = Visibility.Visible;
        }

        private static void Show(params UIElement[] els)
        {
            foreach (var e in els)
                e.Visibility = Visibility.Visible;
        }

        private void Logout()
        {
            new Concept2().Show();
            Close();
        }

        #endregion

        #region Startup & Notifications

        private async Task LoadInitialDataAsync()
        {
            if (Instance.Role is "ADMIN" or "MANAGER")
                await LoadNotificationsAsync();

            switch (Instance.Role)
            {
                case "MERCHANT":
                    ShowPanel(PanelAccountInfo);
                    await LoadAccountInfoAsync();
                    break;

                case "ADMIN":
                case "MANAGER":
                    ShowPanel(PanelManageAccounts);
                    await LoadMerchantsAsync();
                    break;
            }
        }

        private async Task LoadNotificationsAsync()
        {
            try
            {
                var notif = await Instance.HttpService.GetNotificationsAsync();
                var lines = new List<string>();

                if (notif.LowStockCount > 0)
                    lines.Add($"⚠ {notif.LowStockCount} product(s) low on stock");

                if (notif.DefaultedMerchants?.Count > 0)
                    lines.Add($"⚠ {notif.DefaultedMerchants.Count} merchant(s) IN DEFAULT");

                NotifText.Text = lines.Count > 0 ? string.Join("\n", lines) : "No alerts.";
            }
            catch
            {
                NotifText.Text = "Could not load notifications.";
            }
        }

        #endregion

        #region Account Info

        private async void AccountInfo_Refresh(object sender, RoutedEventArgs e) =>
            await LoadAccountInfoAsync();

        private async Task LoadAccountInfoAsync()
        {
            try
            {
                _myProfile = await Instance.HttpService.GetAccountsMeAsync();

                var sb = new StringBuilder();
                sb.AppendLine($"Company:        {_myProfile.CompanyName}");
                sb.AppendLine($"Username:       {_myProfile.Username}");
                sb.AppendLine($"Email:          {_myProfile.Email}");
                sb.AppendLine($"Phone:          {_myProfile.Phone}");

                if (!string.IsNullOrWhiteSpace(_myProfile.Fax))
                    sb.AppendLine($"Fax:            {_myProfile.Fax}");

                sb.AppendLine($"Address:        {_myProfile.Address}");
                sb.AppendLine($"Credit Limit:   £{_myProfile.CreditLimit:N2}");
                sb.AppendLine($"Balance:        £{_myProfile.CurrentBalance:N2}");
                sb.AppendLine($"Account Status: {_myProfile.AccountStatus}");

                if (_myProfile.DiscountPlanName != null)
                    sb.AppendLine($"Discount Plan:  {_myProfile.DiscountPlanName} (ID {_myProfile.DiscountPlanId})");

                AccountInfoBox.Text = sb.ToString();
            }
            catch (Exception ex)
            {
                AccountInfoBox.Text = "Error: " + ex.Message;
            }
        }

        #endregion

        #region Catalogue

        private async void Catalogue_Search(object sender, RoutedEventArgs e) =>
            await LoadCatalogueAsync(CatalogueSearch.Text);

        private async void Catalogue_Clear(object sender, RoutedEventArgs e)
        {
            CatalogueSearch.Clear();
            await LoadCatalogueAsync();
        }

        private async Task LoadCatalogueAsync(string? search = null)
        {
            try
            {
                var items = await Instance.HttpService.GetCatalogueAsync(search);
                CatalogueGrid.ItemsSource = items;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error loading catalogue: " + ex.Message);
            }
        }

        private void Catalogue_AddToBasket(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not CatalogueItemDTO item) return;

            var existing = _basket.FirstOrDefault(b => b.ProductId == item.ProductId);

            if (existing != null)
            {
                existing.Quantity++;
            }
            else
            {
                _basket.Add(new BasketItem
                {
                    ProductId = item.ProductId,
                    Description = item.Description,
                    UnitPrice = item.UnitPrice,
                    Quantity = 1
                });
            }

            MessageBox.Show($"'{item.Description}' added to basket.",
                "Basket", MessageBoxButton.OK, MessageBoxImage.Information);
        }

        #endregion

        #region Basket

        private void RefreshBasket()
        {
            BasketGrid.ItemsSource = null;
            BasketGrid.ItemsSource = _basket;

            decimal subtotal = _basket.Sum(b => b.LineTotal);
            BasketSubtotal.Text = $"£{subtotal:N2}";
            BasketTotal.Text = $"£{subtotal:N2}";

            DiscountRow.Visibility = Visibility.Collapsed;
            FlexNoteRow.Visibility = Visibility.Collapsed;

            if (_myProfile?.DiscountPlanId.HasValue == true)
            {
                FlexNoteRow.Visibility = Visibility.Visible;
                _ = TryShowDiscountAsync(subtotal);
            }
        }

        private async Task TryShowDiscountAsync(decimal subtotal)
        {
            if (_myProfile?.DiscountPlanId == null) return;

            try
            {
                var plan = await Instance.HttpService.GetDiscountPlanByIdAsync(_myProfile.DiscountPlanId.Value);

                FlexNoteRow.Visibility = Visibility.Collapsed;
                DiscountRow.Visibility = Visibility.Collapsed;

                if (plan.PlanType == "FIXED" && plan.FixedRate.HasValue)
                {
                    decimal disc = subtotal * plan.FixedRate.Value / 100m;
                    decimal total = subtotal - disc;

                    DiscountLabel.Text = $"Discount ({plan.FixedRate}%):";
                    DiscountAmount.Text = $"-£{disc:N2}";
                    DiscountRow.Visibility = Visibility.Visible;
                    BasketTotal.Text = $"£{total:N2}";
                }
                else if (plan.PlanType == "FLEXIBLE")
                {
                    FlexNoteRow.Visibility = Visibility.Visible;
                    BasketTotal.Text = $"£{subtotal:N2}";
                }
            }
            catch { }
        }

        private void Basket_Remove(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is BasketItem item)
            {
                _basket.Remove(item);
                RefreshBasket();
            }
        }

        private void Basket_Clear(object sender, RoutedEventArgs e)
        {
            _basket.Clear();
            RefreshBasket();
        }

        private async void Basket_Submit(object sender, RoutedEventArgs e)
        {
            if (_basket.Count == 0)
            {
                MessageBox.Show("Basket is empty.");
                return;
            }

            var lines = _basket.Select(b => new OrderLineRequest
            {
                ProductId = b.ProductId,
                Quantity = b.Quantity
            }).ToList();

            try
            {
                var order = await Instance.HttpService.PostOrderAsync(lines);
                await Task.Delay(1000);

                MessageBox.Show($"Order {order.OrderId} placed!\nTotal: £{order.TotalAmount:N2}",
                    "Order Confirmed", MessageBoxButton.OK, MessageBoxImage.Information);

                _basket.Clear();
                RefreshBasket();
            }
            catch (Exception ex)
            {
                bool accountIssue = ex.Message.Contains("403")
                    || ex.Message.Contains("suspended", StringComparison.OrdinalIgnoreCase)
                    || ex.Message.Contains("default", StringComparison.OrdinalIgnoreCase);

                if (accountIssue)
                    MessageBox.Show("Order blocked: your account is SUSPENDED or IN DEFAULT.",
                        "Order Blocked", MessageBoxButton.OK, MessageBoxImage.Error);
                else
                    MessageBox.Show("Error placing order: " + ex.Message,
                        "Error", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }

        #endregion

        #region My Orders

        private async void MyOrders_Refresh(object sender, RoutedEventArgs e) =>
            await LoadMyOrdersAsync();

        private async Task LoadMyOrdersAsync()
        {
            if (!int.TryParse(Instance.MerchantID, out int mid)) return;

            try
            {
                var orders = await Instance.HttpService.GetOrdersByMerchantAsync(mid);
                MyOrdersGrid.ItemsSource = orders;
                OrderDetailBox.Clear();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void MyOrders_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (MyOrdersGrid.SelectedItem is not OrderDTO row) return;

            try
            {
                var order = await Instance.HttpService.GetOrderByIdAsync(row.OrderId);
                OrderDetailBox.Text = FormatOrderDetail(order);
            }
            catch (Exception ex)
            {
                OrderDetailBox.Text = "Error: " + ex.Message;
            }
        }

        private static string FormatOrderDetail(OrderDTO o)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"Order ID:   {o.OrderId}");
            sb.AppendLine($"Date:       {o.OrderDate:dd/MM/yyyy HH:mm}");
            sb.AppendLine($"Status:     {o.Status}");
            sb.AppendLine($"Subtotal:   £{o.Subtotal:N2}");

            if (o.DiscountAmount > 0)
                sb.AppendLine($"Discount:   -£{o.DiscountAmount:N2}");

            sb.AppendLine($"Total:      £{o.TotalAmount:N2}");

            if (o.Items?.Count > 0)
            {
                sb.AppendLine("\nItems:");
                foreach (var i in o.Items)
                    sb.AppendLine($"  {i.ProductId}  {i.Description}  x{i.Quantity}  £{i.UnitPrice:N2}  = £{i.LineTotal:N2}");
            }

            if (o.Courier != null)
            {
                sb.AppendLine($"\nCourier:    {o.Courier} ({o.CourierRef})");
                sb.AppendLine($"Dispatched: {o.DispatchDate:dd/MM/yyyy}");
                sb.AppendLine($"Expected:   {o.ExpectedDelivery:dd/MM/yyyy}");
            }

            return sb.ToString();
        }

        #endregion

        #region My Balance

        private async void MyBalance_Refresh(object sender, RoutedEventArgs e) =>
            await LoadMyBalanceAsync();

        private async Task LoadMyBalanceAsync()
        {
            if (!int.TryParse(Instance.MerchantID, out int mid)) return;

            try
            {
                var invoices = await Instance.HttpService.GetInvoicesByMerchantAsync(mid);
                MyInvoicesGrid.ItemsSource = invoices;
                InvoiceDetailBox.Clear();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void MyBalance_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (MyInvoicesGrid.SelectedItem is not InvoiceDTO row) return;

            try
            {
                var inv = await Instance.HttpService.GetInvoiceByIdAsync(row.InvoiceId);
                InvoiceDetailBox.Text = FormatInvoiceDetail(inv);
            }
            catch (Exception ex)
            {
                InvoiceDetailBox.Text = "Error: " + ex.Message;
            }
        }

        private static string FormatInvoiceDetail(InvoiceDTO inv)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"Invoice ID:  {inv.InvoiceId}");
            sb.AppendLine($"Order ID:    {inv.OrderId}");
            sb.AppendLine($"Date:        {inv.InvoiceDate:dd/MM/yyyy}");
            sb.AppendLine($"Due Date:    {inv.DueDate}");
            sb.AppendLine($"Amount Due:  £{inv.AmountDue:N2}");
            sb.AppendLine($"Total Paid:  £{inv.TotalPaid:N2}");
            sb.AppendLine($"Status:      {inv.PaymentStatus}");

            if (inv.Payments?.Count > 0)
            {
                sb.AppendLine("\nPayments:");
                foreach (var p in inv.Payments)
                    sb.AppendLine($"  £{p.AmountPaid:N2}  {p.PaymentMethod}  {p.PaymentDate:dd/MM/yyyy}  {p.Notes}");
            }

            return sb.ToString();
        }

        #endregion

        #region Manage Accounts — Merchants

        private async void Merchants_Search(object sender, RoutedEventArgs e) =>
            await LoadMerchantsAsync(MerchantSearch.Text);

        private async void Merchants_ClearSearch(object sender, RoutedEventArgs e)
        {
            MerchantSearch.Clear();
            await LoadMerchantsAsync();
        }

        private async Task LoadMerchantsAsync(string? search = null)
        {
            try
            {
                var list = await Instance.HttpService.GetMerchantsAsync(search);
                MerchantsGrid.ItemsSource = list;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private void Merchant_Edit(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not MerchantDTO row) return;

            var dlg = new EditMerchantDialog(row);
            if (dlg.ShowDialog() == true)
                _ = LoadMerchantsAsync();
        }

        private async void Merchant_Restore(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not MerchantDTO row) return;

            try
            {
                await Instance.HttpService.PutMerchantRestoreAsync(row.MerchantId);
                MessageBox.Show($"{row.CompanyName} restored to NORMAL.");
                await LoadMerchantsAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Merchant_Deactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not MerchantDTO row) return;

            if (MessageBox.Show($"Deactivate {row.CompanyName}?", "Confirm",
                    MessageBoxButton.YesNo) != MessageBoxResult.Yes) return;

            try
            {
                await Instance.HttpService.PutDeactivateAsync(row.UserId);
                await LoadMerchantsAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Merchant_Reactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not MerchantDTO row) return;

            try
            {
                await Instance.HttpService.PutReactivateAsync(row.UserId);
                await LoadMerchantsAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Manage Accounts — Staff

        private async void Staff_Refresh(object sender, RoutedEventArgs e)
        {
            try
            {
                var list = await Instance.HttpService.GetStaffAsync();
                StaffGrid.ItemsSource = list;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async Task Staff_RefreshAsync()
        {
            var list = await Instance.HttpService.GetStaffAsync();
            StaffGrid.ItemsSource = list;
        }

        private async void Staff_ChangeRole(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not StaffDTO row) return;

            var roles = new[] { "ADMIN", "MANAGER", "ACCOUNTANT", "DIRECTOR" };
            var dlg = new SelectionDialog("Change Role", "New role:", roles);
            if (dlg.ShowDialog() != true) return;

            try
            {
                await Instance.HttpService.PutChangeRoleAsync(row.UserId, dlg.Selected);
                MessageBox.Show("Role updated.");
                await Staff_RefreshAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Staff_ResetPassword(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not StaffDTO row) return;

            var pwd = Microsoft.VisualBasic.Interaction.InputBox("New password:", "Reset Password");
            if (string.IsNullOrWhiteSpace(pwd)) return;

            try
            {
                await Instance.HttpService.PutResetPasswordAsync(row.UserId, pwd);
                MessageBox.Show("Password reset.");
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Staff_Deactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not StaffDTO row) return;

            try
            {
                await Instance.HttpService.PutDeactivateAsync(row.UserId);
                await Staff_RefreshAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Staff_Reactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not StaffDTO row) return;

            try
            {
                await Instance.HttpService.PutReactivateAsync(row.UserId);
                await Staff_RefreshAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Account Creation

        private void AccountFormToggle(object sender, RoutedEventArgs e)
        {
            if (MerchantCreateForm == null || StaffCreateForm == null) return;

            bool isMerchant = RadioMerchantForm.IsChecked == true;
            MerchantCreateForm.Visibility = isMerchant ? Visibility.Visible : Visibility.Collapsed;
            StaffCreateForm.Visibility = isMerchant ? Visibility.Collapsed : Visibility.Visible;
        }

        private async void CreateMerchant_Submit(object sender, RoutedEventArgs e)
        {
            if (!decimal.TryParse(NewMerchantCreditLimit.Text, out decimal credit))
            {
                MessageBox.Show("Enter a valid credit limit.");
                return;
            }

            if (!int.TryParse(NewMerchantDiscountPlan.Text, out int planId))
            {
                MessageBox.Show("Discount plan ID is required (must be an integer).");
                return;
            }

            try
            {
                await Instance.HttpService.PostMerchantAsync(
                    NewMerchantUsername.Text,
                    NewMerchantPassword.Password,
                    NewMerchantCompany.Text,
                    NewMerchantAddress.Text,
                    NewMerchantPhone.Text,
                    null,
                    NewMerchantEmail.Text,
                    credit,
                    planId);

                MessageBox.Show("Merchant account created.");
                await LoadMerchantsAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void CreateStaff_Submit(object sender, RoutedEventArgs e)
        {
            var role = (NewStaffRole.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "";

            if (string.IsNullOrWhiteSpace(NewStaffUsername.Text) ||
                NewStaffPassword.Password.Length == 0 ||
                string.IsNullOrWhiteSpace(role))
            {
                MessageBox.Show("All fields required.");
                return;
            }

            try
            {
                await Instance.HttpService.PostStaffAsync(
                    NewStaffUsername.Text, NewStaffPassword.Password, role);

                MessageBox.Show("Staff account created.");
                await Staff_RefreshAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Catalogue Management

        private async void CatMgmt_Refresh(object sender, RoutedEventArgs e) =>
            await LoadCatMgmtAsync();

        private async Task LoadCatMgmtAsync()
        {
            try
            {
                var items = await Instance.HttpService.GetCatalogueAdminAsync();
                CatMgmtGrid.ItemsSource = items;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private void CatMgmt_AddProduct(object sender, RoutedEventArgs e)
        {
            var dlg = new AddProductDialog();
            if (dlg.ShowDialog() == true)
                _ = LoadCatMgmtAsync();
        }

        private void CatMgmt_Edit(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not AdminCatalogueItemDTO item) return;

            var dlg = new EditProductDialog(item);
            if (dlg.ShowDialog() == true)
                _ = LoadCatMgmtAsync();
        }

        private async void CatMgmt_StockIn(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not AdminCatalogueItemDTO item) return;

            var input = Microsoft.VisualBasic.Interaction.InputBox("Quantity to add:", "Stock Delivery", "0");
            if (!int.TryParse(input, out int qty) || qty <= 0) return;

            try
            {
                await Instance.HttpService.PostCatalogueStockAsync(item.ProductId, qty);
                MessageBox.Show("Stock updated.");
                await LoadCatMgmtAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void CatMgmt_Deactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not AdminCatalogueItemDTO item) return;

            try
            {
                await Instance.HttpService.PutCatalogueDeactivateAsync(item.ProductId);
                await LoadCatMgmtAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void CatMgmt_Reactivate(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not AdminCatalogueItemDTO item) return;

            try
            {
                await Instance.HttpService.PutCatalogueReactivateAsync(item.ProductId);
                await LoadCatMgmtAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Orders Management

        private async void Orders_Load(object sender, RoutedEventArgs e)
        {
            var statusItem = (OrderStatusFilter.SelectedItem as ComboBoxItem)?.Content?.ToString();
            var status = statusItem == "(all)" ? null : statusItem;

            try
            {
                var orders = await Instance.HttpService.GetAllOrdersAsync(status);
                AllOrdersGrid.ItemsSource = orders;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Order_UpdateStatus(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not OrderDTO row) return;

            var statuses = new[] { "ACCEPTED", "PROCESSING", "DELIVERED", "CANCELLED" };
            var dlg = new SelectionDialog("Update Status", "New status:", statuses);
            if (dlg.ShowDialog() != true) return;

            try
            {
                await Instance.HttpService.PutOrderStatusAsync(row.OrderId, dlg.Selected);
                MessageBox.Show("Status updated.");
                await RefreshAllOrdersAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private void Order_Dispatch(object sender, RoutedEventArgs e)
        {
            if (((Button)sender).Tag is not OrderDTO row) return;

            var dlg = new DispatchDialog(row.OrderId);
            if (dlg.ShowDialog() == true)
                _ = RefreshAllOrdersAsync();
        }

        private async Task RefreshAllOrdersAsync()
        {
            var orders = await Instance.HttpService.GetAllOrdersAsync();
            AllOrdersGrid.ItemsSource = orders;
        }

        #endregion

        #region Discount Plans

        private async void DiscountPlans_Refresh(object sender, RoutedEventArgs e) =>
            await LoadDiscountPlansAsync();

        private async Task LoadDiscountPlansAsync()
        {
            try
            {
                var plans = await Instance.HttpService.GetDiscountPlansAsync();
                DiscountPlansGrid.ItemsSource = plans;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private void DiscountTypeToggle(object sender, RoutedEventArgs e)
        {
            if (FixedPlanForm == null || FlexiblePlanForm == null) return;

            bool isFixed = RadioFixed.IsChecked == true;
            FixedPlanForm.Visibility = isFixed ? Visibility.Visible : Visibility.Collapsed;
            FlexiblePlanForm.Visibility = isFixed ? Visibility.Collapsed : Visibility.Visible;
        }

        private async void CreateFixedPlan(object sender, RoutedEventArgs e)
        {
            if (!decimal.TryParse(FixedPlanRate.Text, out decimal rate))
            {
                MessageBox.Show("Enter a valid rate.");
                return;
            }

            if (string.IsNullOrWhiteSpace(FixedPlanName.Text))
            {
                MessageBox.Show("Enter a plan name.");
                return;
            }

            try
            {
                await Instance.HttpService.PostFixedPlanAsync(FixedPlanName.Text, rate);
                MessageBox.Show("Fixed plan created.");
                await LoadDiscountPlansAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void CreateFlexPlan(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(FlexPlanName.Text))
            {
                MessageBox.Show("Enter a plan name.");
                return;
            }

            var tiers = new List<FlexTierRequest>();

            foreach (var line in FlexPlanTiers.Text.Split('\n'))
            {
                var parts = line.Trim().Split(',');
                if (parts.Length < 2) continue;
                if (!decimal.TryParse(parts[0].Trim(), out decimal min)) continue;
                if (!decimal.TryParse(parts[parts.Length - 1].Trim(), out decimal dr)) continue;

                decimal? max = parts.Length == 3 && decimal.TryParse(parts[1].Trim(), out decimal mx) ? mx : null;
                tiers.Add(new FlexTierRequest { MinOrderVal = min, MaxOrderVal = max, DiscountRate = dr });
            }

            if (tiers.Count == 0)
            {
                MessageBox.Show("Enter at least one valid tier.\nFormat: minVal,maxVal,rate  or  minVal,rate");
                return;
            }

            try
            {
                await Instance.HttpService.PostFlexiblePlanAsync(FlexPlanName.Text, tiers);
                MessageBox.Show("Flexible plan created.");
                await LoadDiscountPlansAsync();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Record Payment

        private async void Payment_LoadInvoices(object sender, RoutedEventArgs e)
        {
            if (!int.TryParse(PaymentMerchantId.Text, out int mid))
            {
                MessageBox.Show("Enter a valid merchant ID.");
                return;
            }

            try
            {
                var invoices = await Instance.HttpService.GetUnpaidInvoicesAsync(mid);
                PaymentInvoiceCombo.ItemsSource = invoices;
                PaymentInvoiceCombo.DisplayMemberPath = "InvoiceId";

                if (invoices.Count == 0)
                    MessageBox.Show("No unpaid invoices for this merchant.");
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        private async void Payment_Save(object sender, RoutedEventArgs e)
        {
            if (!int.TryParse(PaymentMerchantId.Text, out int mid))
            { MessageBox.Show("Enter a valid merchant ID."); return; }

            if (PaymentInvoiceCombo.SelectedItem is not InvoiceDTO inv)
            { MessageBox.Show("Select an invoice."); return; }

            if (!decimal.TryParse(PaymentAmount.Text, out decimal amount) || amount <= 0)
            { MessageBox.Show("Enter a valid positive amount."); return; }

            if (PaymentMethod.SelectedItem is not ComboBoxItem methodItem)
            { MessageBox.Show("Select a payment method."); return; }

            if (PaymentDate.SelectedDate == null)
            { MessageBox.Show("Select a payment date."); return; }

            var method = methodItem.Content.ToString()!;
            var dateStr = PaymentDate.SelectedDate.Value.ToString("yyyy-MM-ddTHH:mm:ss");

            try
            {
                var updated = await Instance.HttpService.PostPaymentAsync(
                    mid, inv.InvoiceId, amount, method, dateStr,
                    string.IsNullOrWhiteSpace(PaymentNotes.Text) ? null : PaymentNotes.Text);

                MessageBox.Show(
                    $"Payment recorded.\nInvoice {updated.InvoiceId}: £{updated.AmountDue:N2} outstanding  ({updated.PaymentStatus})",
                    "Payment Saved", MessageBoxButton.OK, MessageBoxImage.Information);

                PaymentInvoiceCombo.ItemsSource = null;
                PaymentAmount.Clear();
                PaymentNotes.Clear();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion

        #region Reports

        private async void Report_Generate(object sender, RoutedEventArgs e)
        {
            var typeItem = (ReportType.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "";
            var from = ReportFrom.SelectedDate?.ToString("yyyy-MM-ddT00:00:00") ?? "";
            var to = ReportTo.SelectedDate?.ToString("yyyy-MM-ddT23:59:59") ?? "";
            int.TryParse(ReportMerchantId.Text, out int mid);

            try
            {
                string output;

                if (typeItem.StartsWith("RPT-01"))
                    output = FormatTurnover(await Instance.HttpService.GetReportTurnoverAsync(from, to));
                else if (typeItem.StartsWith("RPT-02"))
                    output = FormatMerchantSummary(await Instance.HttpService.GetReportMerchantSummaryAsync(mid, from, to));
                else if (typeItem.StartsWith("RPT-03"))
                    output = FormatMerchantDetailed(await Instance.HttpService.GetReportMerchantDetailedAsync(mid, from, to));
                else if (typeItem.StartsWith("RPT-04"))
                    output = FormatInvoiceList(await Instance.HttpService.GetReportInvoiceListAsync(mid, from, to), false);
                else if (typeItem.StartsWith("RPT-05"))
                    output = FormatInvoiceList(await Instance.HttpService.GetReportAllInvoicesAsync(from, to), true);
                else if (typeItem.StartsWith("RPT-07"))
                    output = FormatLowStock(await Instance.HttpService.GetReportLowStockAsync());
                else
                    output = "Select a report type.";

                ReportOutput.Text = output;
            }
            catch (Exception ex)
            {
                ReportOutput.Text = "Error: " + ex.Message;
            }
        }

        private static string FormatTurnover(TurnoverReportDTO r)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"TURNOVER REPORT  {r.From:dd/MM/yyyy} – {r.To:dd/MM/yyyy}");
            sb.AppendLine(new string('-', 70));
            sb.AppendLine($"{"Product",-15} {"Description",-30} {"Qty",8} {"Revenue",12}");
            sb.AppendLine(new string('-', 70));

            foreach (var l in r.Lines)
                sb.AppendLine($"{l.ProductId,-15} {l.Description,-30} {l.TotalQuantitySold,8} £{l.TotalRevenue,10:N2}");

            sb.AppendLine(new string('-', 70));
            sb.AppendLine($"{"TOTAL",-46} £{r.TotalRevenue,10:N2}");
            return sb.ToString();
        }

        private static string FormatMerchantSummary(MerchantOrdersSummaryDTO r)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"MERCHANT ORDERS SUMMARY  {r.From:dd/MM/yyyy} – {r.To:dd/MM/yyyy}");
            sb.AppendLine($"{r.CompanyName}  |  {r.Phone}  |  {r.Address}");
            sb.AppendLine(new string('-', 80));
            sb.AppendLine($"{"Order ID",-22} {"Date",-14} {"Amount",10} {"Dispatched",-14} {"Pay Status",-12}");
            sb.AppendLine(new string('-', 80));

            foreach (var o in r.Orders)
                sb.AppendLine($"{o.OrderId,-22} {o.OrderedDate:dd/MM/yyyy} £{o.Amount,8:N2} {o.DispatchedDate?.ToString("dd/MM/yyyy") ?? "-",-14} {o.PaymentStatus,-12}");

            sb.AppendLine(new string('-', 80));
            sb.AppendLine($"Orders: {r.TotalOrders}   Value: £{r.TotalValue:N2}   Paid: {r.TotalPaid}");
            return sb.ToString();
        }

        private static string FormatMerchantDetailed(MerchantDetailedReportDTO r)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"MERCHANT DETAILED REPORT  {r.From:dd/MM/yyyy} – {r.To:dd/MM/yyyy}");
            sb.AppendLine($"{r.CompanyName}  |  {r.Phone}  |  {r.Email}");
            sb.AppendLine(r.Address);

            foreach (var o in r.Orders)
            {
                sb.AppendLine($"\n  Order {o.OrderId}  {o.OrderedDate:dd/MM/yyyy}  Discount: £{o.DiscountAmount:N2}  Total: £{o.OrderTotal:N2}  [{o.PaymentStatus}]");

                foreach (var i in o.Items)
                    sb.AppendLine($"    {i.ProductId,-15} x{i.Quantity,4}  @ £{i.UnitCost:N2}  = £{i.Amount:N2}");
            }

            sb.AppendLine($"\nGrand Total: £{r.GrandTotal:N2}");
            return sb.ToString();
        }

        private static string FormatInvoiceList(InvoiceListReportDTO r, bool allMerchants)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"INVOICE LIST  {r.From:dd/MM/yyyy} – {r.To:dd/MM/yyyy}");
            sb.AppendLine(new string('-', 80));
            sb.AppendLine(allMerchants
                ? $"{"Invoice",-22} {"Order",-22} {"Merchant",-20} {"Due",10} {"Status",-10}"
                : $"{"Invoice",-22} {"Order",-22} {"Due",10} {"Paid",10} {"Status",-10}");
            sb.AppendLine(new string('-', 80));

            foreach (var inv in r.Invoices)
            {
                sb.AppendLine(allMerchants
                    ? $"{inv.InvoiceId,-22} {inv.OrderId,-22} {inv.MerchantName,-20} £{inv.AmountDue,8:N2} {inv.PaymentStatus,-10}"
                    : $"{inv.InvoiceId,-22} {inv.OrderId,-22} £{inv.AmountDue,8:N2} £{inv.TotalPaid,8:N2} {inv.PaymentStatus,-10}");
            }

            return sb.ToString();
        }

        private static string FormatLowStock(LowStockReportDTO r)
        {
            var sb = new StringBuilder();
            sb.AppendLine($"LOW STOCK REPORT  (generated {r.GeneratedAt:dd/MM/yyyy HH:mm})");
            sb.AppendLine(new string('-', 70));
            sb.AppendLine($"{"Product",-15} {"Description",-30} {"In Stock",9} {"Min",6} {"Reorder",8}");
            sb.AppendLine(new string('-', 70));

            foreach (var i in r.Items)
                sb.AppendLine($"{i.ProductId,-15} {i.Description,-30} {i.Availability,9} {i.MinStockLevel,6} {i.RecommendedOrderQty,8}");

            return sb.ToString();
        }

        private void Report_Print(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(ReportOutput.Text)) return;

            var pd = new PrintDialog();
            if (pd.ShowDialog() != true) return;

            var doc = new FlowDocument(new Paragraph(new Run(ReportOutput.Text)))
            {
                PageWidth = pd.PrintableAreaWidth,
                PageHeight = pd.PrintableAreaHeight,
                FontFamily = ReportOutput.FontFamily,
                FontSize = 11
            };

            pd.PrintDocument(((IDocumentPaginatorSource)doc).DocumentPaginator, "GOATflow Report");
        }

        #endregion

        #region Audit Log

        private async void AuditLog_Load(object sender, RoutedEventArgs e)
        {
            try
            {
                var from = AuditFrom.SelectedDate?.ToString("yyyy-MM-ddT00:00:00");
                var to = AuditTo.SelectedDate?.ToString("yyyy-MM-ddT23:59:59");

                var logs = await Instance.HttpService.GetAuditLogAsync(
                    AuditUserId.Text.Trim().NullIfEmpty(),
                    AuditTargetType.Text.Trim().NullIfEmpty(),
                    AuditTargetId.Text.Trim().NullIfEmpty(),
                    from, to);

                AuditGrid.ItemsSource = logs;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error: " + ex.Message);
            }
        }

        #endregion
    }

    internal static class StringExt
    {
        public static string? NullIfEmpty(this string s) =>
            string.IsNullOrWhiteSpace(s) ? null : s;
    }
}