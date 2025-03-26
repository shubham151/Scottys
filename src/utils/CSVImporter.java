package utils;

import database.DatabaseHelper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class CSVImporter {

    // === 1. IMPORT PRODUCT DETAILS CSV ===
    public static void importProductsCSV(String filePath) {
        int addedCount = 0;
        int updatedCount = 0;
        try (Connection conn = DatabaseHelper.connect();
             BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (values.length < 5) {
                    System.err.println("Skipping invalid row: " + line);
                    continue;
                }

                try {
                    int itemNumber = Integer.parseInt(values[0].trim());
                    String label = values[1].trim();
                    String category = values[2].trim();
                    String subcategory = values[3].trim();
                    double price = Double.parseDouble(values[4].replace("$", "").trim());
                    String recStatus = "Active";

                    if (productExists(conn, itemNumber)) {
                        updateProduct(conn, category, subcategory, itemNumber, label, price, recStatus);
                        updatedCount++;
                    } else {
                        insertProduct(conn, category, subcategory, itemNumber, label, price, recStatus);
                        addedCount++;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping row due to number format: " + line);
                }
            }

            System.out.println(addedCount + " products added, " + updatedCount + " products updated.");
        } catch (IOException | SQLException e) {
            System.err.println("Error importing Products CSV: " + e.getMessage());
        }
    }
    private static boolean productExists(Connection conn, int itemNumber) throws SQLException {
        String query = "SELECT COUNT(*) FROM Product WHERE item_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, itemNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


    private static void updateProduct(Connection conn, String category, String subcategory,
                                      int itemNumber, String label, double price, String recStatus) throws SQLException {
        String updateSQL = """
        UPDATE Product
        SET category = ?,
            label = ?, subcategory = ?, price = ?, rec_status = ?
        WHERE item_number = ?
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, category);
            pstmt.setString(3, label);
            pstmt.setString(4, subcategory);
            pstmt.setDouble(5, price);
            pstmt.setString(6, recStatus);
            pstmt.setInt(7, itemNumber);
            pstmt.executeUpdate();
        }
    }

    private static void insertProduct(Connection conn, String category, String subcategory,
                                      int itemNumber, String label, double price, String recStatus) throws SQLException {
//        String categoryId = getCategoryId(conn, category, subcategory);
        String insertSQL = """
        INSERT INTO Product (category, item_number, label, subcategory, price, rec_status)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, category);
            pstmt.setInt(2, itemNumber);
            pstmt.setString(3, label);
            pstmt.setString(4, subcategory);
            pstmt.setDouble(5, price);
            pstmt.setString(6, recStatus);
            pstmt.executeUpdate();
        }
    }


    private static String getCategoryId(Connection conn, String category, String subcategory) throws SQLException {
        String query = "SELECT id FROM Category WHERE category = ? AND subcategory = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, category);
            pstmt.setString(2, subcategory);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        }
        // If category doesn't exist, insert it.
        String insertSQL = "INSERT INTO Category (category, subcategory) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category);
            pstmt.setString(2, subcategory);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "NA";
    }

    // === 2. IMPORT SALES CSV ===
    // Assume new format: Item Number, Quantity, Price, From Date, To Date, Store
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");

    public static void importSalesCSV(String filePath) {
        int addedCount = 0;
        try (Connection conn = DatabaseHelper.connect();
             BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (values.length < 6) {
                    System.err.println("Skipping invalid row: " + line);
                    continue;
                }
                try {
                    int itemNumber = Integer.parseInt(values[0].trim());
                    int quantity = Integer.parseInt(values[1].trim());
                    double price = Double.parseDouble(values[2].replace("$", "").trim());
                    LocalDate fromDate = LocalDate.parse(values[3].trim(), DATE_FORMATTER);
                    LocalDate toDate = LocalDate.parse(values[4].trim(), DATE_FORMATTER);
                    String store = values[5].trim();
                    insertSale(conn, itemNumber, quantity, price, fromDate, toDate, store);
                    addedCount++;
                } catch (Exception e) {
                    System.err.println("Skipping row due to error: " + line + " | Error: " + e.getMessage());
                }
            }
            System.out.println(addedCount + " sales records added.");
        } catch (IOException | SQLException e) {
            System.err.println("Error importing Sales CSV: " + e.getMessage());
        }
    }

    private static void insertSale(Connection conn, int itemNumber, int quantity, double price, LocalDate fromDate, LocalDate toDate, String store) throws SQLException {
        String insertSQL = "INSERT INTO Sales (item_number, quantity, price, from_date, to_date, store) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setInt(1, itemNumber);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.setDate(4, java.sql.Date.valueOf(fromDate));
            pstmt.setDate(5, java.sql.Date.valueOf(toDate));
            pstmt.setString(6, store);
            pstmt.executeUpdate();
        }
    }

    // === 3. IMPORT PRODUCT CATEGORY CSV ===
    // New CSV should include two columns: Category and Subcategory.
    public static void importProductCategoryCSV(String filePath) {
        int addedCount = 0;
        Set<String> existingCategories = new HashSet<>();
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT category, subcategory FROM Category")) {
            while (rs.next()) {
                String key = rs.getString("category") + " - " + rs.getString("subcategory");
                existingCategories.add(key);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching existing categories: " + e.getMessage());
        }
        try (Connection conn = DatabaseHelper.connect();
             BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] values = line.split(",");
                String category, subcategory;
                if (values.length == 1) {
                    category = values[0].trim();
                    subcategory = values[0].trim();
                } else if (values.length >= 2) {
                    category = values[0].trim();
                    subcategory = values[1].trim();
                } else {
                    continue;
                }
                String key = category + " - " + subcategory;
                if (!existingCategories.contains(key)) {
                    insertCategory(conn, category, subcategory);
                    addedCount++;
                }
            }
            System.out.println(addedCount + " categories added.");
        } catch (IOException | SQLException e) {
            System.err.println("Error importing Category CSV: " + e.getMessage());
        }
    }

    private static void insertCategory(Connection conn, String category, String subcategory) throws SQLException {
        String insertSQL = "INSERT INTO Category (category, subcategory) VALUES (?, ?) " +
                "ON CONFLICT(category, subcategory) DO NOTHING";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, category);
            pstmt.setString(2, subcategory);
            pstmt.executeUpdate();
        }
    }
}
