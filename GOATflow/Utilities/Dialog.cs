using GOATflow.Models;
using GOATflow.Utilities;
using System;
using System.Windows;
using System.Windows.Controls;

namespace GOATflow
{
    public class EditMerchantDialog : Window
    {
        private readonly MerchantDTO _row;
        private readonly TextBox _company, _address, _email, _phone, _fax, _credit, _planId;

        public EditMerchantDialog(MerchantDTO row)
        {
            _row = row;
            Title = $"Edit Merchant — {row.CompanyName}";
            Width = 440;
            Height = 500;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var sp = new StackPanel { Margin = new Thickness(16) };
            _company = Field(sp, "Company Name", row.CompanyName);
            _address = Field(sp, "Address", row.Address);
            _email = Field(sp, "Email", row.Email);
            _phone = Field(sp, "Phone", row.Phone);
            _fax = Field(sp, "Fax", row.Fax ?? "");
            _credit = Field(sp, "Credit Limit (£)", row.CreditLimit.ToString("N2"));
            _planId = Field(sp, "Discount Plan ID", row.DiscountPlanId?.ToString() ?? "");

            var btn = new Button
            {
                Content = "Save",
                Width = 80,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(0, 12, 0, 0)
            };
            btn.Click += Save_Click;
            sp.Children.Add(btn);
            Content = sp;
        }

        private static TextBox Field(StackPanel sp, string label, string value)
        {
            sp.Children.Add(new Label { Content = label });
            var tb = new TextBox { Text = value, Margin = new Thickness(0, 0, 0, 4) };
            sp.Children.Add(tb);
            return tb;
        }

        private async void Save_Click(object sender, RoutedEventArgs e)
        {
            if (!decimal.TryParse(_credit.Text, out decimal credit))
            { MessageBox.Show("Enter a valid credit limit."); return; }
            int? planId = int.TryParse(_planId.Text, out int pid) ? pid : null;

            try
            {
                await Instance.HttpService.PutMerchantAsync(
                    _row.MerchantId,
                    _company.Text.Trim().NullIfEmpty(),
                    _address.Text.Trim().NullIfEmpty(),
                    _phone.Text.Trim().NullIfEmpty(),
                    _fax.Text.Trim().NullIfEmpty(),
                    _email.Text.Trim().NullIfEmpty(),
                    credit,
                    planId);
                DialogResult = true;
                Close();
            }
            catch (Exception ex) { MessageBox.Show("Error: " + ex.Message); }
        }
    }
    public class AddProductDialog : Window
    {
        private readonly TextBox _id, _desc, _pkgType, _unit, _upp, _price, _avail, _minStock, _buffer;

        public AddProductDialog()
        {
            Title = "Add Product";
            Width = 420; 
            Height = 600;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var sp = new StackPanel { Margin = new Thickness(16) };
            _id = Field(sp, "Product Code *");
            _desc = Field(sp, "Description *");
            _pkgType = Field(sp, "Package Type");
            _unit = Field(sp, "Unit");
            _upp = Field(sp, "Units Per Pack *", "1");
            _price = Field(sp, "Unit Price (£) *");
            _avail = Field(sp, "Initial Stock *", "0");
            _minStock = Field(sp, "Min Stock Level *", "0");
            _buffer = Field(sp, "Reorder Buffer % (default 10)", "10");

            var btn = new Button
            {
                Content = "Create",
                Width = 80,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(0, 12, 0, 0)
            };
            btn.Click += Save_Click;
            sp.Children.Add(btn);
            Content = new System.Windows.Controls.ScrollViewer { Content = sp };
        }

        private static TextBox Field(StackPanel sp, string label, string val = "")
        {
            sp.Children.Add(new Label { Content = label });
            var tb = new TextBox { Text = val, Margin = new Thickness(0, 0, 0, 4) };

            sp.Children.Add(tb);
            return tb;
        }

        private async void Save_Click(object sender, RoutedEventArgs e)
        {
            var errors = new List<string>();

            if (string.IsNullOrWhiteSpace(_id.Text) || string.IsNullOrWhiteSpace(_desc.Text))
                errors.Add("Product code and description are required.");

            if (!int.TryParse(_upp.Text, out int upp) || upp < 1)
                errors.Add("Enter valid units per pack.");

            if (!decimal.TryParse(_price.Text, out decimal price) || price <= 0)
                errors.Add("Enter a valid unit price.");

            if (!int.TryParse(_avail.Text, out int avail) || avail < 0)
                errors.Add("Enter valid availability.");

            if (!int.TryParse(_minStock.Text, out int minStock) || minStock < 0)
                errors.Add("Enter a valid min stock level.");

            if (errors.Any())
            {
                MessageBox.Show(string.Join(Environment.NewLine, errors));
                return;
            }

            decimal buffer = decimal.TryParse(_buffer.Text, out decimal b) ? b : 10m;

            try
            {
                await Instance.HttpService.PostCatalogueAsync(
                    _id.Text.Trim(), _desc.Text.Trim(),
                    _pkgType.Text.Trim().NullIfEmpty(),
                    _unit.Text.Trim().NullIfEmpty(),
                    upp, price, avail, minStock, buffer);

                DialogResult = true;
                Close();
            }
            catch (Exception ex) { MessageBox.Show("Error: " + ex.Message); }
        }
    }

    public class EditProductDialog : Window
    {
        private readonly AdminCatalogueItemDTO _item;
        private readonly TextBox _desc, _pkgType, _unit, _upp, _price, _minStock, _buffer;

        public EditProductDialog(AdminCatalogueItemDTO item)
        {
            _item = item;
            Title = $"Edit Product — {item.ProductId}";
            Width = 420; Height = 440;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var sp = new StackPanel { Margin = new Thickness(16) };
            sp.Children.Add(new Label { Content = $"Product ID: {item.ProductId}" });

            _desc = Field(sp, "Description", item.Description);
            _pkgType = Field(sp, "Package Type", item.PackageType ?? "");
            _unit = Field(sp, "Unit", item.Unit ?? "");
            _upp = Field(sp, "Units Per Pack", item.UnitsPerPack.ToString());
            _price = Field(sp, "Unit Price (£)", item.UnitPrice.ToString("N2"));
            _minStock = Field(sp, "Min Stock Level", item.MinStockLevel.ToString());
            _buffer = Field(sp, "Reorder Buffer %", item.ReorderBufferPct.ToString());

            var btn = new Button
            {
                Content = "Save",
                Width = 80,
                HorizontalAlignment = HorizontalAlignment.Right,
                Margin = new Thickness(0, 12, 0, 0)
            };
            btn.Click += Save_Click;
            sp.Children.Add(btn);
            Content = new System.Windows.Controls.ScrollViewer { Content = sp };
        }

        private static TextBox Field(StackPanel sp, string label, string val = "")
        {
            sp.Children.Add(new Label { Content = label });
            var tb = new TextBox { Text = val, Margin = new Thickness(0, 0, 0, 4) };
            sp.Children.Add(tb);
            return tb;
        }

        private async void Save_Click(object sender, RoutedEventArgs e)
        {
            int? upp = int.TryParse(_upp.Text, out int u) ? u : null;
            decimal? price = decimal.TryParse(_price.Text, out decimal p) ? p : null;
            int? minSt = int.TryParse(_minStock.Text, out int ms) ? ms : null;
            decimal? buffer = decimal.TryParse(_buffer.Text, out decimal bf) ? bf : null;

            try
            {
                await Instance.HttpService.PutCatalogueAsync(
                    _item.ProductId,
                    _desc.Text.Trim().NullIfEmpty(),
                    _pkgType.Text.Trim().NullIfEmpty(),
                    _unit.Text.Trim().NullIfEmpty(),
                    upp, price, minSt, buffer);
                DialogResult = true;
                Close();
            }
            catch (Exception ex) { MessageBox.Show("Error: " + ex.Message); }
        }
    }

    public class SelectionDialog : Window
    {
        public string Selected { get; private set; } = "";
        private readonly ComboBox _combo;

        public SelectionDialog(string title, string prompt, string[] options)
        {
            Title = title;
            Width = 300; 
            Height = 160;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var sp = new StackPanel { Margin = new Thickness(16) };
            sp.Children.Add(new Label { Content = prompt });
            _combo = new ComboBox { Margin = new Thickness(0, 0, 0, 12) };
            foreach (var o in options) _combo.Items.Add(o);
            _combo.SelectedIndex = 0;
            sp.Children.Add(_combo);

            var btn = new Button
            {
                Content = "OK",
                Width = 70,
                HorizontalAlignment = HorizontalAlignment.Right
            };
            btn.Click += (_, _) =>
            {
                Selected = _combo.SelectedItem?.ToString() ?? "";
                DialogResult = true;
                Close();
            };
            sp.Children.Add(btn);
            Content = sp;
        }
    }

    public class DispatchDialog : Window
    {
        private readonly string _orderId;
        private readonly TextBox _courier, _ref, _dispatch, _expected;

        public DispatchDialog(string orderId)
        {
            _orderId = orderId;
            Title = $"Dispatch Order {orderId}";
            Width = 400; Height = 300;
            WindowStartupLocation = WindowStartupLocation.CenterOwner;

            var sp = new StackPanel { Margin = new Thickness(16) };

            sp.Children.Add(new Label { Content = "Courier *" });
            _courier = new TextBox { Margin = new Thickness(0, 0, 0, 4) };
            sp.Children.Add(_courier);

            sp.Children.Add(new Label { Content = "Courier Reference *" });
            _ref = new TextBox { Margin = new Thickness(0, 0, 0, 4) };
            sp.Children.Add(_ref);

            sp.Children.Add(new Label { Content = "Dispatch Date (yyyy-MM-ddTHH:mm:ss) *" });
            _dispatch = new TextBox
            {
                Text = DateTime.Now.ToString("yyyy-MM-ddTHH:mm:ss"),
                Margin = new Thickness(0, 0, 0, 4)
            };
            sp.Children.Add(_dispatch);

            sp.Children.Add(new Label { Content = "Expected Delivery (yyyy-MM-ddTHH:mm:ss) *" });
            _expected = new TextBox
            {
                Text = DateTime.Now.AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss"),
                Margin = new Thickness(0, 0, 0, 12)
            };
            sp.Children.Add(_expected);

            var btn = new Button
            {
                Content = "Dispatch",
                Width = 80,
                HorizontalAlignment = HorizontalAlignment.Right
            };
            btn.Click += Dispatch_Click;
            sp.Children.Add(btn);
            Content = sp;
        }

        private async void Dispatch_Click(object sender, RoutedEventArgs e)
        {
            if (string.IsNullOrWhiteSpace(_courier.Text) || string.IsNullOrWhiteSpace(_ref.Text))
            { 
                MessageBox.Show("Courier and reference are required."); 
                return; 
            }

            try
            {
                await Instance.HttpService.PutOrderDispatchAsync(_orderId, _courier.Text, _ref.Text, _dispatch.Text, _expected.Text);
                MessageBox.Show($"Order {_orderId} dispatched.");

                DialogResult = true;
                Close();
            }
            catch (Exception ex) { MessageBox.Show("Error: " + ex.Message); }
        }
    }
}
