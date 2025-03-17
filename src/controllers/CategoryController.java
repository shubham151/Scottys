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
    @FXML private TableColumn<Category, String> colCategoryCode;
    @FXML private TableColumn<Category, String> colCategoryName;
    @FXML private TextField tfCategoryCodeFilter;
    @FXML private TextField tfCategoryNameFilter;

    private final int PAGE_SIZE = 1000;
    private int currentOffset = 0;
    private final CategoryService categoryService = new CategoryService();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();
    private FilteredList<Category> filteredCategories;

    // Track new categories added via addCategory that haven't been saved.
    private final List<Category> newCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        categoryTable.setEditable(true);
        setupTableColumns();
        loadInitialCategories();
        setupScrollListener();
    }

    private void setupTableColumns() {
        // Configure Category Code column with in-place editing.
        colCategoryCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryCode()));
        colCategoryCode.setCellFactory(TextFieldTableCell.forTableColumn());
        colCategoryCode.setOnEditCommit(event -> {
            Category cat = event.getRowValue();
            cat.setCategoryCode(event.getNewValue().trim());
            attemptSaveCategory(cat);
        });

        // Configure Category Name column with in-place editing.
        colCategoryName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategoryName()));
        colCategoryName.setCellFactory(TextFieldTableCell.forTableColumn());
        colCategoryName.setOnEditCommit(event -> {
            Category cat = event.getRowValue();
            cat.setCategoryName(event.getNewValue().trim());
            attemptSaveCategory(cat);
        });
    }

    /**
     * Attempts to save the category if both fields are nonempty.
     * If the category is tracked as new, then it calls insert;
     * otherwise, it calls update.
     */
    private void attemptSaveCategory(Category cat) {
        // Only attempt a save if both fields are filled.
        if (cat.getCategoryCode().isEmpty() || cat.getCategoryName().isEmpty()) {
            return;
        }
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
        // Clear the newCategories list since these rows now come from the DB.
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
        String codeFilter = tfCategoryCodeFilter.getText().trim();
        if (!codeFilter.isEmpty()) {
            sb.append(" AND lower(category_code) LIKE ?");
        }
        String nameFilter = tfCategoryNameFilter.getText().trim();
        if (!nameFilter.isEmpty()) {
            sb.append(" AND lower(category_name) LIKE ?");
        }
        return sb.toString();
    }

    private List<Object> buildFilterParameters() {
        List<Object> params = new ArrayList<>();
        String codeFilter = tfCategoryCodeFilter.getText().trim();
        if (!codeFilter.isEmpty()) {
            params.add("%" + codeFilter.toLowerCase() + "%");
        }
        String nameFilter = tfCategoryNameFilter.getText().trim();
        if (!nameFilter.isEmpty()) {
            params.add("%" + nameFilter.toLowerCase() + "%");
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
        // Create a new category with empty fields.
        Category newCategory = new Category("", "");
        newCategories.add(newCategory);
        categoryList.add(0, newCategory);
        categoryTable.refresh();
        categoryTable.requestFocus();
        categoryTable.getSelectionModel().clearAndSelect(0);
        categoryTable.getFocusModel().focus(0);
        // Start inline editing on the Category Code column.
        categoryTable.edit(0, colCategoryCode);
    }

    @FXML
    private void deleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (categoryService.deleteCategory(selected.getCategoryCode())) {
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
