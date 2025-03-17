package services;

import database.DatabaseHelper;
import models.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT p.item_number, p.label, c.category_name AS productClass, " +
                "p.tax_rate_1, p.tax_rate_2, p.tax_rate_3, p.tax_rate_4, " +
                "p.price, p.rec_status " +
                "FROM Product p LEFT JOIN Category c ON p.category_id = c.id";

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("item_number"),
                        rs.getString("label"),
                        rs.getString("productClass"),
                        rs.getString("tax_rate_1"),
                        rs.getString("tax_rate_2"),
                        rs.getString("tax_rate_3"),
                        rs.getString("tax_rate_4"),
                        rs.getDouble("price"),
                        rs.getString("rec_status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }

    public void updateProductCategory(int itemNumber, String newCategoryName) {
        String query = "UPDATE Product SET category_id = (SELECT id FROM Category WHERE category_name = ?) WHERE item_number = ?";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newCategoryName);
            pstmt.setInt(2, itemNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error updating product category: " + e.getMessage());
        }
    }


    // Returns a page of products based on filter criteria.
    public List<Product> getProducts(int limit, int offset, String filterQuery, List<Object> filterParams) {
        List<Product> products = new ArrayList<>();
        String query = "SELECT p.item_number, p.label, c.category_name AS productClass, " +
                "p.tax_rate_1, p.tax_rate_2, p.tax_rate_3, p.tax_rate_4, " +
                "p.price, p.rec_status " +
                "FROM Product p LEFT JOIN Category c ON p.category_id = c.id " +
                "WHERE 1=1 " + filterQuery + " " +
                "ORDER BY p.item_number LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            int paramIndex = 1;
            if (filterParams != null) {
                for (Object param : filterParams) {
                    pstmt.setObject(paramIndex++, param);
                }
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("item_number"),
                        rs.getString("label"),
                        rs.getString("productClass"),
                        rs.getString("tax_rate_1"),
                        rs.getString("tax_rate_2"),
                        rs.getString("tax_rate_3"),
                        rs.getString("tax_rate_4"),
                        rs.getDouble("price"),
                        rs.getString("rec_status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }

    // Updates an existing product record.
    public void updateProduct(Product product) {
        String query = "UPDATE Product SET label = ?, category_id = (SELECT id FROM Category WHERE category_name = ?), " +
                "tax_rate_1 = ?, tax_rate_2 = ?, tax_rate_3 = ?, tax_rate_4 = ?, price = ?, rec_status = ? " +
                "WHERE item_number = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, product.getLabel());
            pstmt.setString(2, product.getProductClass());
            pstmt.setString(3, product.getTaxRate1());
            pstmt.setString(4, product.getTaxRate2());
            pstmt.setString(5, product.getTaxRate3());
            pstmt.setString(6, product.getTaxRate4());
            pstmt.setDouble(7, product.getPrice());
            pstmt.setString(8, product.getRecStatus());
            pstmt.setInt(9, product.getItemNumber());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating product: " + e.getMessage());
        }
    }

    // Inserts a new product record.
    public boolean insertProduct(Product product) {
        String query = "INSERT INTO Product (category_id, item_number, label, tax_rate_1, tax_rate_2, " +
                "tax_rate_3, tax_rate_4, price, rec_status) " +
                "VALUES ((SELECT id FROM Category WHERE category_name = ?), ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, product.getProductClass());
            pstmt.setInt(2, product.getItemNumber());
            pstmt.setString(3, product.getLabel());
            pstmt.setString(4, product.getTaxRate1());
            pstmt.setString(5, product.getTaxRate2());
            pstmt.setString(6, product.getTaxRate3());
            pstmt.setString(7, product.getTaxRate4());
            pstmt.setDouble(8, product.getPrice());
            pstmt.setString(9, product.getRecStatus());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting product: " + e.getMessage());
            return false;
        }
    }

    // Deletes a product record.
    public boolean deleteProduct(int itemNumber) {
        String query = "DELETE FROM Product WHERE item_number = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, itemNumber);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting product: " + e.getMessage());
            return false;
        }
    }
}
