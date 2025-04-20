package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import models.Store;
import services.StoreService;

public class StoreController {
    @FXML private TableView<Store> storeTable;
    @FXML private TableColumn<Store, String> colStoreName;
    @FXML private TableColumn<Store, String> colLocation;
    @FXML private TextField tfStoreName;
    @FXML private TextField tfLocation;
    @FXML private TextField tfStoreFilter;
    @FXML private Button btnAddStore;
    @FXML private Button btnDeleteStore;

    private final StoreService storeService = new StoreService();
    private final ObservableList<Store> storeList = FXCollections.observableArrayList();
    private FilteredList<Store> filteredStores;

    @FXML
    public void initialize() {
        storeTable.setEditable(true);

        colStoreName.setCellValueFactory(data -> data.getValue().storeNameProperty());
        colStoreName.setCellFactory(TextFieldTableCell.forTableColumn());
        colStoreName.setOnEditCommit(event -> {
            Store store = event.getRowValue();
            String newName = event.getNewValue().trim();
            if (!newName.isEmpty()) {
                store.setStoreName(newName);
                saveOrUpdate(store);
            }
        });

        colLocation.setCellValueFactory(data -> data.getValue().locationProperty());
        colLocation.setCellFactory(TextFieldTableCell.forTableColumn());
        colLocation.setOnEditCommit(event -> {
            Store store = event.getRowValue();
            String newLoc = event.getNewValue().trim();
            if (!newLoc.isEmpty()) {
                store.setLocation(newLoc);
                saveOrUpdate(store);
            }
        });

        filteredStores = new FilteredList<>(storeList, s -> true);
        storeTable.setItems(filteredStores);

        tfStoreFilter.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase().trim();
            filteredStores.setPredicate(store ->
                    store.getStoreName().toLowerCase().contains(filter) ||
                            store.getLocation().toLowerCase().contains(filter));
        });

        btnAddStore.setOnAction(e -> addStore());
        btnDeleteStore.setOnAction(e -> deleteSelectedStore());

        loadStores();
    }

    private void loadStores() {
        storeList.setAll(storeService.getAllStores());
    }

    @FXML
    private void addStore() {
        Store newStore = new Store(0, "", "", true);
        storeList.add(newStore);
        storeTable.layout();
        int rowIndex = storeList.indexOf(newStore);
        storeTable.getSelectionModel().select(rowIndex);
        storeTable.scrollTo(newStore);
        storeTable.edit(rowIndex, colStoreName);
    }


    @FXML
    private void deleteSelectedStore() {
        Store selected = storeTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean success = storeService.deleteStoreById(selected.getId());
            if (success) {
                storeList.remove(selected);
            } else {
                showAlert("Error", "Failed to delete store.");
            }
        }
    }

    @FXML
    private void updateSelectedStore() {
        Store selected = storeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a store to update.");
            return;
        }

        String newName = tfStoreName.getText().trim();
        String newLocation = tfLocation.getText().trim();

        if (newName.isEmpty() || newLocation.isEmpty()) {
            showAlert("Validation Error", "Store name and location cannot be empty.");
            return;
        }

        boolean updated = storeService.updateStoreById(selected.getId(), new Store(selected.getId(), newName, newLocation, false));
        if (updated) {
            loadStores();
            tfStoreName.clear();
            tfLocation.clear();
            showAlert("Success", "Store updated successfully.");
        } else {
            showAlert("Error", "Failed to update store.");
        }
    }


    private void saveOrUpdate(Store store) {
        if (!store.getStoreName().isEmpty() && !store.getLocation().isEmpty()) {
            if (store.isNew()) {
                if (storeService.addStore(store)) {
                    store.markSaved();
                    loadStores();
                }
            } else {
                storeService.updateStoreById(store.getId(), store);
                loadStores();
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}