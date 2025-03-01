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
}
