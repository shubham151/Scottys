package services;

import database.DatabaseHelper;
import models.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryService {

    public void addCategory(String categoryCode, String categoryName) {
        String query = "INSERT INTO Category (category_code, category_name) VALUES (?, ?) " +
                "ON CONFLICT(category_code) DO NOTHING";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, categoryCode);
            pstmt.setString(2, categoryName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding category: " + e.getMessage());
        }
    }

    // Returns a page of categories based on filter criteria.
    public List<Category> getCategories(int limit, int offset, String filterQuery, List<Object> filterParams) {
        List<Category> categories = new ArrayList<>();
        String query = "SELECT category_code, category_name FROM Category WHERE 1=1 " +
                filterQuery + " ORDER BY category_name LIMIT ? OFFSET ?";
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
                categories.add(new Category(
                        rs.getString("category_code"),
                        rs.getString("category_name")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching categories: " + e.getMessage());
        }
        return categories;
    }

    // Convenience method: get all categories (without paging)
    public List<Category> getAllCategories() {
        return getCategories(100000, 0, "", null);
    }

    // Inserts a new category.
    public boolean insertCategory(Category category) {
        String query = "INSERT INTO Category (category_code, category_name) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, category.getCategoryCode());
            pstmt.setString(2, category.getCategoryName());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error inserting category: " + e.getMessage());
            return false;
        }
    }

    // Updates an existing category.
    public boolean updateCategory(Category category) {
        String query = "UPDATE Category SET category_name = ? WHERE category_code = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getCategoryCode());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    // Deletes a category.
    public boolean deleteCategory(String categoryCode) {
        String query = "DELETE FROM Category WHERE category_code = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, categoryCode);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }
}
