package database;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    private static final String DB_FOLDER = System.getProperty("user.home") + "/IdeaProjects/Scottys/db/";
    private static final String DB_PATH = DB_FOLDER + "product_database.db";

    public static void initializeDatabase() {
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            try {
                Files.createDirectories(new File(DB_FOLDER).toPath());
                System.out.println("Database initialized at: " + DB_PATH);
            } catch (Exception e) {
                System.err.println("Failed to initialize database: " + e.getMessage());
            }
        }
    }

    public static Connection connect() {
        initializeDatabase();
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        return conn;
    }

    public static void createTables() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Drop existing tables (forces schema update)
//            stmt.execute("DROP TABLE IF EXISTS Product;");
//            stmt.execute("DROP TABLE IF EXISTS Sales;");
//            stmt.execute("DROP TABLE IF EXISTS Category;");

            // Recreate tables
            createCategoryTable();
            createProductTable();
            createSalesTable();

        } catch (SQLException e) {
            System.err.println("Error resetting database: " + e.getMessage());
        }
    }


    private static void createProductTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Product (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_id TEXT, " +
                "item_number INTEGER UNIQUE, " +
                "label TEXT, " +
                "tax_rate_1 TEXT DEFAULT 'N/A', " + // ✅ Default value for missing tax rates
                "tax_rate_2 TEXT DEFAULT 'N/A', " +
                "tax_rate_3 TEXT DEFAULT 'N/A', " +
                "tax_rate_4 TEXT DEFAULT 'N/A', " +
                "price REAL, " +
                "rec_status TEXT DEFAULT 'Active', " + // ✅ Ensure rec_status is added
                "FOREIGN KEY (category_id) REFERENCES Category(id))";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating Product table: " + e.getMessage());
        }
    }


    private static void createSalesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_number INTEGER, " +
                "quantity INTEGER, " +
                "date TEXT, " +
                "amount REAL, " +
                "FOREIGN KEY (item_number) REFERENCES Product(item_number))";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating Sales table: " + e.getMessage());
        }
    }

    private static void createCategoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Category (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_code TEXT UNIQUE, " +
                "category_name TEXT UNIQUE)";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating Category table: " + e.getMessage());
        }
    }
}
