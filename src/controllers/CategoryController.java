package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import models.Category;
import services.CategoryService;
import utils.CSVImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CategoryController {

    @FXML private Button btnAddCategory;
    @FXML private Button btnDeleteCategory;
    @FXML private Button btnImportCategories;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> colCategory;
    @FXML private TableColumn<Category, String> colSubcategory;
    @FXML private TextField tfCategoryFilter;
    @FXML private TextField tfSubcategoryFilter;

    private final int PAGE_SIZE = 1000;
    private int currentOffset = 0;
    private final CategoryService categoryService = new CategoryService();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private FilteredList<Category> filteredCategories;
    private final List<Category> newCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        categoryTable.setEditable(true);
        setupTableColumns();
        loadInitialCategories();
        setupScrollListener();
    }

    private void setupTableColumns() {
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory()));
        colCategory.setCellFactory(TextFieldTableCell.forTableColumn());
        colCategory.setOnEditCommit(event -> {
            Category cat = event.getRowValue();
            cat.setCategory(event.getNewValue().trim());
            attemptSaveCategory(cat);
        });

        colSubcategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSubcategory()));
        colSubcategory.setCellFactory(TextFieldTableCell.forTableColumn());
        colSubcategory.setOnEditCommit(event -> {
            Category cat = event.getRowValue();
            cat.setSubcategory(event.getNewValue().trim());
            attemptSaveCategory(cat);
        });
    }

    private void attemptSaveCategory(Category cat) {
        if (cat.getCategory().isEmpty() || cat.getSubcategory().isEmpty()) return;

        boolean success;
        if (newCategories.contains(cat)) {
            success = categoryService.insertCategory(cat);
            if (success) {
                newCategories.remove(cat);
                showAlert("Success", "Category added successfully.");
            } else {
                showAlert("Save Error", "Failed to add new category.");
            }
        } else {
            success = categoryService.updateCategory(cat);
            if (success) {
                showAlert("Success", "Category updated successfully.");
            } else {
                showAlert("Save Error", "Failed to update category.");
            }
        }
        loadInitialCategories();
    }

    private void loadInitialCategories() {
        currentOffset = 0;
        List<Category> cats = categoryService.getCategories(PAGE_SIZE, currentOffset, buildFilterQuery(), buildFilterParameters());
        categoryList.setAll(cats);
        newCategories.clear();
        filteredCategories = new FilteredList<>(categoryList, p -> true);
        categoryTable.setItems(filteredCategories);
    }

    private void loadMoreCategories() {
        currentOffset += PAGE_SIZE;
        List<Category> more = categoryService.getCategories(PAGE_SIZE, currentOffset, buildFilterQuery(), buildFilterParameters());
        categoryList.addAll(more);
    }

    private String buildFilterQuery() {
        StringBuilder sb = new StringBuilder();
        if (!tfCategoryFilter.getText().trim().isEmpty()) {
            sb.append(" AND lower(category) LIKE ?");
        }
        if (!tfSubcategoryFilter.getText().trim().isEmpty()) {
            sb.append(" AND lower(subcategory) LIKE ?");
        }
        return sb.toString();
    }

    private List<Object> buildFilterParameters() {
        List<Object> params = new ArrayList<>();
        String catFilter = tfCategoryFilter.getText().trim();
        if (!catFilter.isEmpty()) {
            params.add("%" + catFilter.toLowerCase() + "%");
        }
        String subcatFilter = tfSubcategoryFilter.getText().trim();
        if (!subcatFilter.isEmpty()) {
            params.add("%" + subcatFilter.toLowerCase() + "%");
        }
        return params;
    }

    private void setupScrollListener() {
        categoryTable.setOnScroll(event -> {
            if (categoryTable.getItems().size() % PAGE_SIZE == 0 &&
                    categoryTable.getItems().size() > 0 &&
                    categoryTable.getFocusModel().getFocusedIndex() >= categoryTable.getItems().size() - 1) {
                loadMoreCategories();
            }
        });
    }

    @FXML
    private void importCategoryCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Category CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductCategoryCSV(selectedFile.getAbsolutePath());
            loadInitialCategories();
        }
    }

    @FXML
    private void addCategory() {
        Category newCategory = new Category("", "");
        newCategories.add(newCategory);
        categoryList.add(0, newCategory);
        categoryTable.refresh();
        categoryTable.requestFocus();
        categoryTable.getSelectionModel().clearAndSelect(0);
        categoryTable.getFocusModel().focus(0);
        categoryTable.edit(0, colCategory);
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getId() == 0) {
                // It's a new unsaved row
                categoryList.remove(selected);
                newCategories.remove(selected);
                return;
            }
            if (categoryService.deleteCategory(selected.getId())) {
                categoryList.remove(selected);
                newCategories.remove(selected);
            } else {
                showAlert("Delete Error", "Could not delete category.");
            }
        } else {
            showAlert("Selection Error", "No category selected.");
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
