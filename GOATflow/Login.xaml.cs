using GOATflow.Utilities;
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
using System.Windows.Shapes;

namespace GOATflow
{
    /// <summary>
    /// Interaction logic for Concept2.xaml
    /// </summary>
    public partial class Concept2 : Window
    {
        public Concept2()
        {
            InitializeComponent();
            
        }

        private async void ButtonHandler(object sender, RoutedEventArgs e)
        {
            switch (((Button)sender).Name)
            {
                case "Exit":
                    Environment.Exit(0);
                    break;
                case "Minimize":
                    this.WindowState = WindowState.Minimized;
                    break;
                case "LoginButton":
                    string json = await Instance.HttpService.PostAuthLoginAsync(UserBox.Text, PasswordBox.Text);
                    break;
            }
        }

        private void Border_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.LeftButton == MouseButtonState.Pressed)
            {
                this.DragMove();
            }
        }
    }
}
