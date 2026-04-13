using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GOATflow.Utilities
{
    public class Instance
    {
        public static string UserID;
        public static string Username;
        public static string Role;
        public static string PaymentReminderDueToken;
        public static string MerchantID;
        public static string Token;

        public static HttpService HttpService;

        public Instance()
        {
            HttpService = new HttpService();
        }
    }
}
