package services;

import database.DatabaseHelper;
import models.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        // Updated query: retrieve main category (as productClass) and subcategory.
        String query = "SELECT p.item_number, p.label, p.category as productClass, p.subcategory, " +
                "p.price, p.rec_status " +
                "FROM Product p LEFT JOIN Category c ON " +
                "p.category = c.category";
        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("item_number"),
                        rs.getString("label"),
                        rs.getString("productClass"),
                        rs.getString("subcategory"),
                        rs.getDouble("price"),
                        rs.getString("rec_status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }

    // Returns a page of products based on filter criteria.
    public List<Product> getProducts(int limit, int offset, String filterQuery, List<Object> filterParams) {
        List<Product> products = new ArrayList<>();
        // Updated query to include subcategory
        String query = "SELECT p.item_number, p.label, p.category as productClass, p.subcategory, " +
                "p.price, p.rec_status " +
                "FROM Product p LEFT JOIN Category c ON p.category = c.category " +
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
                        rs.getString("subcategory"),
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
    public boolean updateProduct(Product product) {
        String query = "UPDATE Product SET label = ?, " +
                "category = ?, " +
                "subcategory = ?, price = ?, rec_status = ? " +
                "WHERE item_number = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, product.getLabel());
            // Lookup Category id using both main category and subcategory
            pstmt.setString(2, product.getProductClass());
            pstmt.setString(3, product.getSubcategory());
            pstmt.setDouble(4, product.getPrice());
            pstmt.setString(5, product.getRecStatus());
            pstmt.setInt(6, product.getItemNumber());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Error updating product: " + e.getMessage());
        }
        return false;
    }

    // Inserts a new product record.
    public boolean insertProduct(Product product) {
        String query = "INSERT INTO Product (category, subcategory, item_number, label, price, rec_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, product.getProductClass());
            pstmt.setString(2, product.getSubcategory());
            pstmt.setInt(3, product.getItemNumber());
            pstmt.setString(4, product.getLabel());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setString(6, product.getRecStatus());
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
