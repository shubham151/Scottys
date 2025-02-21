package utils;

import database.DatabaseHelper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CSVImporter {

    public static void importCSV(String filePath) {
        int addedCount = 0;
        int updatedCount = 0;

        try (Connection conn = DatabaseHelper.connect();
             BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {

            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { // Skip header row
                    firstLine = false;
                    continue;
                }

                String[] values = line.split(",");

                if (values.length < 8) {
                    System.out.println("Skipping invalid row: " + line);
                    continue;
                }

                String productClass = values[0].trim();
                int itemNumber = Integer.parseInt(values[1].trim());
                String label = values[2].trim();
                String taxRate1 = values[3].trim();
                String taxRate2 = values[4].trim();
                String taxRate3 = values[5].trim();
                String taxRate4 = values[6].trim();
                double price = Double.parseDouble(values[7].replace("$", "").trim());

                if (productExists(conn, itemNumber)) {
                    updateProduct(conn, itemNumber, productClass, label, taxRate1, taxRate2, taxRate3, taxRate4, price);
                    updatedCount++;
                } else {
                    insertProduct(conn, productClass, itemNumber, label, taxRate1, taxRate2, taxRate3, taxRate4, price);
                    addedCount++;
                }
            }

            System.out.println(addedCount + " products added, " + updatedCount + " products updated.");

        } catch (IOException | SQLException e) {
            System.err.println("Error importing CSV: " + e.getMessage());
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

    private static void updateProduct(Connection conn, int itemNumber, String productClass, String label,
                                      String taxRate1, String taxRate2, String taxRate3, String taxRate4, double price) throws SQLException {
        String updateSQL = "UPDATE Product SET class = ?, label = ?, tax_rate_1 = ?, tax_rate_2 = ?, " +
                "tax_rate_3 = ?, tax_rate_4 = ?, price = ? WHERE item_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            pstmt.setString(1, productClass);
            pstmt.setString(2, label);
            pstmt.setString(3, taxRate1);
            pstmt.setString(4, taxRate2);
            pstmt.setString(5, taxRate3);
            pstmt.setString(6, taxRate4);
            pstmt.setDouble(7, price);
            pstmt.setInt(8, itemNumber);
            pstmt.executeUpdate();
        }
    }

    private static void insertProduct(Connection conn, String productClass, int itemNumber, String label,
                                      String taxRate1, String taxRate2, String taxRate3, String taxRate4, double price) throws SQLException {
        String insertSQL = "INSERT INTO Product (class, item_number, label, tax_rate_1, tax_rate_2, tax_rate_3, tax_rate_4, price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, productClass);
            pstmt.setInt(2, itemNumber);
            pstmt.setString(3, label);
            pstmt.setString(4, taxRate1);
            pstmt.setString(5, taxRate2);
            pstmt.setString(6, taxRate3);
            pstmt.setString(7, taxRate4);
            pstmt.setDouble(8, price);
            pstmt.executeUpdate();
        }
    }
}
