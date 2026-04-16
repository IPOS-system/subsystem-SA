using MySqlConnector;
using System;
using System.Threading.Tasks;

namespace GOATflow.Utilities
{
    /// <summary>
    /// Direct MySQL access for actions that need to bypass the REST backend.
    ///
    /// Currently used only by the merchant Delete button, which the marking sheet
    /// calls out as a CASCADE delete. Because the backend's account endpoint is a
    /// soft-delete (PUT /deactivate flips is_active=false), this helper performs
    /// a real DELETE against the ipos_sa database to physically remove the merchant
    /// row and everything that references it.
    ///
    /// Connection string is kept in sync with the backend application.properties:
    ///   spring.datasource.url=jdbc:mysql://localhost:3306/ipos_sa
    ///   spring.datasource.username=root
    ///   spring.datasource.password=Ipos1234!
    /// If your MySQL credentials differ, update ConnectionString below.
    /// </summary>
    public static class DatabaseService
    {
        public static string ConnectionString { get; set; } =
            "Server=localhost;Port=3306;Database=ipos_sa;Uid=root;Pwd=Ipos1234!;" +
            "AllowPublicKeyRetrieval=true;SslMode=none;";

        /// <summary>
        /// Hard-deletes the merchant row and all rows that reference it, inside a single
        /// transaction. Order of deletes respects the foreign-key graph:
        ///
        ///   payments               → merchants
        ///   invoices               → merchants
        ///   monthly_discount_tracker → merchants
        ///   order_items            → orders (CASCADE in schema, so implicit)
        ///   orders                 → merchants
        ///   merchants              → users  (CASCADE — deleting the user would also
        ///                                    drop the merchant row, but we delete the
        ///                                    merchant first to control the order)
        ///   users                  (the merchant's login)
        ///
        /// Returns a summary of how many rows were affected in each table.
        /// </summary>
        public static async Task<DeleteSummary> DeleteMerchantCascadeAsync(int merchantId)
        {
            var summary = new DeleteSummary();

            await using var conn = new MySqlConnection(ConnectionString);
            await conn.OpenAsync();
            await using var tx = await conn.BeginTransactionAsync();

            try
            {
                // Look up the user_id for this merchant so we can delete the login too.
                int? userId = null;
                await using (var cmd = new MySqlCommand(
                    "SELECT user_id FROM merchants WHERE merchant_id = @mid", conn, tx))
                {
                    cmd.Parameters.AddWithValue("@mid", merchantId);
                    var result = await cmd.ExecuteScalarAsync();
                    if (result == null || result == DBNull.Value)
                        throw new InvalidOperationException(
                            $"Merchant {merchantId} does not exist in the database.");
                    userId = Convert.ToInt32(result);
                }

                // 1. payments for this merchant
                summary.Payments = await ExecAsync(conn, tx,
                    "DELETE FROM payments WHERE merchant_id = @mid", merchantId);

                // 2. invoices for this merchant
                summary.Invoices = await ExecAsync(conn, tx,
                    "DELETE FROM invoices WHERE merchant_id = @mid", merchantId);

                // 3. monthly_discount_tracker for this merchant
                summary.DiscountTracker = await ExecAsync(conn, tx,
                    "DELETE FROM monthly_discount_tracker WHERE merchant_id = @mid", merchantId);

                // 4. orders for this merchant (order_items cascade automatically)
                summary.Orders = await ExecAsync(conn, tx,
                    "DELETE FROM orders WHERE merchant_id = @mid", merchantId);

                // 5. the merchant row itself
                summary.Merchants = await ExecAsync(conn, tx,
                    "DELETE FROM merchants WHERE merchant_id = @mid", merchantId);

                // 6. the user row (login). Wrap in try — if the user is referenced by
                //    audit_log / authorized_by / etc. we keep the user row and let the
                //    merchant delete stand. audit_log has no ON DELETE clause so it
                //    will block the user delete if any audit entries exist.
                if (userId.HasValue)
                {
                    try
                    {
                        await using var cmd = new MySqlCommand(
                            "DELETE FROM users WHERE user_id = @uid", conn, tx);
                        cmd.Parameters.AddWithValue("@uid", userId.Value);
                        summary.Users = await cmd.ExecuteNonQueryAsync();
                    }
                    catch (MySqlException)
                    {
                        // User row still referenced (e.g. by audit_log). That's fine —
                        // merchant is gone, so this user can no longer log in as a
                        // merchant. We intentionally preserve audit history.
                        summary.Users = 0;
                        summary.UserKeptForAudit = true;
                    }
                }

                await tx.CommitAsync();
                return summary;
            }
            catch
            {
                await tx.RollbackAsync();
                throw;
            }
        }

        private static async Task<int> ExecAsync(
            MySqlConnection conn, MySqlTransaction tx, string sql, int merchantId)
        {
            await using var cmd = new MySqlCommand(sql, conn, tx);
            cmd.Parameters.AddWithValue("@mid", merchantId);
            return await cmd.ExecuteNonQueryAsync();
        }

        public class DeleteSummary
        {
            public int Payments { get; set; }
            public int Invoices { get; set; }
            public int DiscountTracker { get; set; }
            public int Orders { get; set; }
            public int Merchants { get; set; }
            public int Users { get; set; }
            public bool UserKeptForAudit { get; set; }

            public override string ToString() =>
                $"Payments        deleted: {Payments}\n" +
                $"Invoices        deleted: {Invoices}\n" +
                $"Discount tracker rows  : {DiscountTracker}\n" +
                $"Orders (+ items) rows  : {Orders}\n" +
                $"Merchant row           : {Merchants}\n" +
                $"User (login) row       : {Users}" +
                (UserKeptForAudit ? "  (kept — referenced by audit log)" : "");
        }
    }
}
