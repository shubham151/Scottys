package database;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    private static final String DB_FOLDER = System.getProperty("user.home") + "/IdeaProjects/Scottys/db/";
    private static final String DB_PATH = DB_FOLDER + "product_database.db";
    private static final String RESOURCE_DB = "/db/product_database.db"; // Must be inside resources/db/

    // Ensure DB exists in the writable directory
    public static void initializeDatabase() {
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            try {
                Files.createDirectories(new File(DB_FOLDER).toPath());
                InputStream is = DatabaseHelper.class.getResourceAsStream(RESOURCE_DB);

                if (is == null) {
                    System.err.println("Database resource not found in resources/db/!");
                    return;
                }

                Files.copy(is, dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database copied to: " + DB_PATH);
            } catch (IOException e) {
                System.err.println("Failed to copy database: " + e.getMessage());
            }
        }
    }

    // Connect to SQLite Database
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

    // Create the Product Table (if it doesn't exist)
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Product (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "class TEXT, " +
                "item_number INTEGER UNIQUE, " +
                "label TEXT, " +
                "tax_rate_1 TEXT, " +
                "tax_rate_2 TEXT, " +
                "tax_rate_3 TEXT, " +
                "tax_rate_4 TEXT, " +
                "price REAL, " +
                "rec_status TEXT DEFAULT 'Active')";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }
}
