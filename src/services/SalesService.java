package services;

import database.DatabaseHelper;
import models.Sale;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalesService {
    public List<Sale> getAllSales() {
        List<Sale> sales = new ArrayList<>();
        String query = "SELECT item_number, quantity, date, amount FROM Sales";

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sales.add(new Sale(
                        rs.getInt("item_number"),
                        rs.getInt("quantity"),
                        rs.getString("date"),
                        rs.getDouble("amount")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching sales: " + e.getMessage());
        }
        return sales;
    }
}
