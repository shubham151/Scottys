package services;

import database.DatabaseHelper;
import models.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryService {

    // Adds a new category record.
    public void addCategory(String category, String subcategory) {
        String query = "INSERT INTO Category (category, subcategory) VALUES (?, ?) " +
                "ON CONFLICT(category, subcategory) DO NOTHING";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, category);
            pstmt.setString(2, subcategory);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding category: " + e.getMessage());
        }
    }

    // Returns a page of categories based on filter criteria.
    public List<Category> getCategories(int limit, int offset, String filterQuery, List<Object> filterParams) {
        List<Category> categories = new ArrayList<>();
        // Updated query now selects the id along with category and subcategory.
        String query = "SELECT id, category, subcategory FROM Category WHERE 1=1 " +
                filterQuery + " ORDER BY category, subcategory LIMIT ? OFFSET ?";
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
                // Create a Category with id, category, and subcategory.
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getString("subcategory")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching categories: " + e.getMessage());
        }
        return categories;
    }

    // Convenience method: get all categories.
    public List<Category> getAllCategories() {
        return getCategories(100000, 0, "", null);
    }

    // Inserts a new category.
    public boolean insertCategory(Category category) {
        String query = "INSERT INTO Category (category, subcategory) VALUES (?, ?)";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category.getCategory());
            pstmt.setString(2, category.getSubcategory());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                // Retrieve the generated id and set it in the Category model.
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    category.setId(rs.getInt(1));
                }
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error inserting category: " + e.getMessage());
            return false;
        }
    }

    // Updates an existing category.
    // This method updates both fields (in case the main category is also allowed to change).
    public boolean updateCategory(Category category) {
        String query = "UPDATE Category SET category = ?, subcategory = ? WHERE id = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, category.getCategory());
            pstmt.setString(2, category.getSubcategory());
            pstmt.setInt(3, category.getId());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    // Deletes a category based on its id.
    public boolean deleteCategory(int id) {
        String query = "DELETE FROM Category WHERE id = ?";
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }
}
