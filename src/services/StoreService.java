package services;

import database.DatabaseHelper;
import models.Store;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoreService {

    public List<Store> getAllStores() {
        List<Store> stores = new ArrayList<>();
        String query = "SELECT id, store_name, location FROM Store ORDER BY store_name";

        try (Connection conn = DatabaseHelper.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                stores.add(new Store(
                        rs.getInt("id"),
                        rs.getString("store_name"),
                        rs.getString("location"),
                        false));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching stores: " + e.getMessage());
        }

        return stores;
    }

    public boolean addStore(Store store) {
        if (store.getStoreName() == null || store.getLocation() == null ||
                store.getStoreName().isBlank() || store.getLocation().isBlank()) return false;

        String query = "INSERT INTO Store (store_name, location) VALUES (?, ?)";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, store.getStoreName());
            pstmt.setString(2, store.getLocation());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding store: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStoreById(int id, Store updatedStore) {
        if (updatedStore.getStoreName() == null || updatedStore.getLocation() == null ||
                updatedStore.getStoreName().isBlank() || updatedStore.getLocation().isBlank()) return false;

        String query = "UPDATE Store SET store_name = ?, location = ? WHERE id = ?";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, updatedStore.getStoreName());
            pstmt.setString(2, updatedStore.getLocation());
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating store: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStoreById(int id) {
        String query = "DELETE FROM Store WHERE id = ?";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting store: " + e.getMessage());
            return false;
        }
    }

    public boolean addStoreDirect(String name, String location) {
        if (name == null || location == null || name.isBlank() || location.isBlank()) return false;
        return addStore(new Store(name, location));
    }

    public boolean updateStoreDirect(Store store, String newName, String newLocation) {
        if (store == null || newName == null || newLocation == null || newName.isBlank() || newLocation.isBlank()) return false;
        return updateStoreById(store.getId(), new Store(newName, newLocation));
    }

    public boolean addOrUpdateIfNew(Store store) {
        if (store == null) return false;
        if (store.isNew()) {
            return addStore(store);
        } else {
            return updateStoreById(store.getId(), store);
        }
    }
}