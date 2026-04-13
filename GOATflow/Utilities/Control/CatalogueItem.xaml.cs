using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace GOATflow.Utilities.Control
{
    /// <summary>
    /// Interaction logic for CatalogueItem.xaml
    /// </summary>
    public partial class CatalogueItem : UserControl
    {
        public CatalogueItem(string ProductID, string PackageType, string Unit, string UnitsPerPack, string UnitPrice, string Availability)
        {
            InitializeComponent();

            ProductIDLabel.Content = ProductID;
            PackageTypeLabel.Content = PackageType;
            UnitLabel.Content = Unit;
            UnitsPerPackLabel.Content = UnitsPerPack;
            PriceLabel.Content = UnitPrice;
            AvailabilityLabel.Content = Availability;
        }
    }
}
