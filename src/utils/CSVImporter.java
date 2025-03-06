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
                if (firstLine) { // ✅ Skip header row
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // ✅ Correctly handle CSV commas

                if (values.length < 8) { // ✅ Ensure correct column count
                    System.err.println("Skipping invalid row: " + line);
                    continue;
                }

                try {
                    String productClass = values[0].trim(); // ✅ Category Name
                    int itemNumber = Integer.parseInt(values[1].trim()); // ✅ Item Number
                    String label = values[2].trim(); // ✅ Product Name
                    String taxRate1 = values[3].trim().equalsIgnoreCase("Not Assigned") ? "N/A" : values[3].trim();
                    String taxRate2 = values[4].trim().equalsIgnoreCase("Not Assigned") ? "N/A" : values[4].trim();
                    String taxRate3 = values[5].trim().equalsIgnoreCase("Not Assigned") ? "N/A" : values[5].trim();
                    String taxRate4 = values[6].trim().equalsIgnoreCase("Not Assigned") ? "N/A" : values[6].trim();
                    double price = Double.parseDouble(values[7].replace("$", "").trim()); // ✅ Remove "$"
                    String recStatus = "Active"; // ✅ Default value

                    if (productExists(conn, itemNumber)) {
                        updateProduct(conn, productClass, itemNumber, label, taxRate1, taxRate2, taxRate3, taxRate4, price, recStatus);
                        updatedCount++;
                    } else {
                        insertProduct(conn, productClass, itemNumber, label, taxRate1, taxRate2, taxRate3, taxRate4, price, recStatus);
                        addedCount++;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Skipping row due to incorrect number format: " + line);
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
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void updateProduct(Connection conn, String productClass, int itemNumber, String label,
                                      String taxRate1, String taxRate2, String taxRate3, String taxRate4,
                                      double price, String recStatus) throws SQLException {
        String updateSQL = "UPDATE Product SET class = ?, label = ?, tax_rate_1 = ?, tax_rate_2 = ?, " +
                "tax_rate_3 = ?, tax_rate_4 = ?, price = ?, rec_status = ? WHERE item_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, productClass);
            pstmt.setString(2, label);
            pstmt.setString(3, taxRate1);
            pstmt.setString(4, taxRate2);
            pstmt.setString(5, taxRate3);
            pstmt.setString(6, taxRate4);
            pstmt.setDouble(7, price);
            pstmt.setString(8, recStatus);
            pstmt.setInt(9, itemNumber);
            pstmt.executeUpdate();
        }
    }

    private static void insertProduct(Connection conn, String categoryCode, int itemNumber, String label,
                                      String taxRate1, String taxRate2, String taxRate3, String taxRate4,
                                      double price, String recStatus) throws SQLException {

        String categoryId = getCategoryId(conn, categoryCode);

        String insertSQL = "INSERT INTO Product (category_id, item_number, label, tax_rate_1, tax_rate_2, " +
                "tax_rate_3, tax_rate_4, price, rec_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, categoryId);
            pstmt.setInt(2, itemNumber);
            pstmt.setString(3, label);
            pstmt.setString(4, taxRate1);
            pstmt.setString(5, taxRate2);
            pstmt.setString(6, taxRate3);
            pstmt.setString(7, taxRate4);
            pstmt.setDouble(8, price);
            pstmt.setString(9, recStatus);
            pstmt.executeUpdate();
        }
    }

    private static String getCategoryId(Connection conn, String categoryCode) throws SQLException {
        String query = "SELECT category_name FROM Category WHERE category_code = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, categoryCode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("category_name");
            }
        }

        // If category doesn't exist, insert it
        String insertSQL = "INSERT INTO Category (category_code, category_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoryCode);
            pstmt.setString(2, categoryCode); // Use category_code as name if missing
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "NA";
    }


    // === 2. IMPORT SALES CSV ===
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy"); // CSV Format

    public static void importSalesCSV(String filePath) {
        int addedCount = 0;

        try (Connection conn = DatabaseHelper.connect();
             BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { // ✅ Skip header row
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // ✅ Handle CSV commas

                if (values.length < 5) { // ✅ Ensure correct column count
                    System.err.println("Skipping invalid row: " + line);
                    continue;
                }

                try {
                    int itemNumber = Integer.parseInt(values[0].trim());
                    int quantity = Integer.parseInt(values[1].trim());
                    double price = Double.parseDouble(values[2].replace("$", "").trim());

                    // ✅ Convert date strings to LocalDate
                    LocalDate fromDate = LocalDate.parse(values[3].trim(), DATE_FORMATTER);
                    LocalDate toDate = LocalDate.parse(values[4].trim(), DATE_FORMATTER);

                    insertSale(conn, itemNumber, quantity, price, fromDate, toDate);
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

    private static void insertSale(Connection conn, int itemNumber, int quantity, double price, LocalDate fromDate, LocalDate toDate) throws SQLException {
        String insertSQL = "INSERT INTO Sales (item_number, quantity, price, from_date, to_date) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setInt(1, itemNumber);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.setDate(4, java.sql.Date.valueOf(fromDate)); // ✅ Convert LocalDate to SQL Date
            pstmt.setDate(5, java.sql.Date.valueOf(toDate));   // ✅ Convert LocalDate to SQL Date
            pstmt.executeUpdate();
        }
    }


    // === 3. IMPORT PRODUCT CATEGORY CSV ===
    public static void importProductCategoryCSV(String filePath) {
        int addedCount = 0;
        Set<String> existingCategories = new HashSet<>();

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT category_code FROM Category")) {
            while (rs.next()) {
                existingCategories.add(rs.getString("category_code"));
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
                String categoryCode, categoryName;

                if (values.length == 1) {
                    categoryCode = values[0].trim(); // Use same value for both
                    categoryName = values[0].trim();
                } else if (values.length >= 2) {
                    categoryCode = values[0].trim();
                    categoryName = values[1].trim();
                } else {
                    continue; // Skip invalid rows
                }

                if (!existingCategories.contains(categoryCode)) {
                    insertCategory(conn, categoryCode, categoryName);
                    addedCount++;
                }
            }

            System.out.println(addedCount + " categories added.");

        } catch (IOException | SQLException e) {
            System.err.println("Error importing Category CSV: " + e.getMessage());
        }
    }

    private static void insertCategory(Connection conn, String categoryCode, String categoryName) throws SQLException {
        String insertSQL = "INSERT INTO Category (category_code, category_name) VALUES (?, ?) " +
                "ON CONFLICT(category_code) DO NOTHING";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, categoryCode);
            pstmt.setString(2, categoryName);
            pstmt.executeUpdate();
        }
    }

}
