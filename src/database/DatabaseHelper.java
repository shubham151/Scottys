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
//            stmt.execute("DROP TABLE IF EXISTS Store;");

            // Recreate tables
            createCategoryTable();
            createProductTable();
            createSalesTable();
            createStoreTable();

        } catch (SQLException e) {
            System.err.println("Error resetting database: " + e.getMessage());
        }
    }


    private static void createStoreTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Store (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "store_name TEXT, " +
                "location TEXT" +
                ")";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating Store table: " + e.getMessage());
        }
    }




    private static void createProductTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Product (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT, " +
                "item_number INTEGER UNIQUE, " +
                "label TEXT, " +
                "subcategory TEXT, " +
                "price REAL, " +
                "rec_status TEXT DEFAULT 'Active', " +
                "FOREIGN KEY (category) REFERENCES Category(category)" +
                ")";
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
                "price REAL, " +
                "from_date DATE, " +
                "to_date DATE, " +
                "store TEXT, " +
                "FOREIGN KEY (item_number) REFERENCES Product(item_number)" +
                ")";
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
                "category TEXT, " +
                "subcategory TEXT, " +
                "UNIQUE(category, subcategory)" +
                ")";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating Category table: " + e.getMessage());
        }
    }
}
