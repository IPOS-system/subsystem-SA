package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/ipos_sa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER  = "root";
    private static final String DB_PASS  = "TeamProject";

    private static DBConnection instance;
    private Connection connection;

    private DBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            System.out.println("[DBConnection] Connected to ipos_sa database.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL driver not found: " + e.getMessage());
            throw new RuntimeException("MySQL driver not found.", e);
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection failed: " + e.getMessage());
            throw new RuntimeException("Database connection failed.", e);
        }
    }


    public static DBConnection getInstance() {
        try {
            if (instance == null || instance.getConnection().isClosed()) {
                instance = new DBConnection();
            }
        } catch (SQLException e) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public static void close() {
        if (instance != null) {
            try {
                instance.connection.close();
                System.out.println("[DBConnection] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }
}
