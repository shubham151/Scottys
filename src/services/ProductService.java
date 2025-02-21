package services;

import database.DatabaseHelper;
import models.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM Product";

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                products.add(new Product(
                        rs.getInt("id"),
                        rs.getString("class"),
                        rs.getInt("item_number"),
                        rs.getString("label"),
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
}
