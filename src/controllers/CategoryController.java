package controllers;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import models.Category;
import services.CategoryService;
import utils.CSVImporter;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;

public class CategoryController {
    @FXML private Button btnImportCategories;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> colCategoryCode;
    @FXML private TableColumn<Category, String> colCategoryName;
    @FXML private TextField tfCategoryCodeFilter;
    @FXML private TextField tfCategoryNameFilter;

    private final CategoryService categoryService = new CategoryService();
    private FilteredList<Category> filteredCategories;

    @FXML
    public void initialize() {
        colCategoryCode.setCellValueFactory(new PropertyValueFactory<>("categoryCode"));
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        loadCategories();
        setupFilters();
    }

    private void loadCategories() {
        List<Category> categories = categoryService.getAllCategories();
        filteredCategories = new FilteredList<>(FXCollections.observableArrayList(categories), p -> true);
        categoryTable.setItems(filteredCategories);
        categoryTable.refresh();
    }

    private void setupFilters() {
        tfCategoryCodeFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfCategoryNameFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
    }

    private void updateFilters() {
        filteredCategories.setPredicate(category -> {
            boolean matchesCode = tfCategoryCodeFilter.getText().isEmpty() ||
                    category.getCategoryCode().toLowerCase().contains(tfCategoryCodeFilter.getText().toLowerCase());
            boolean matchesName = tfCategoryNameFilter.getText().isEmpty() ||
                    category.getCategoryName().toLowerCase().contains(tfCategoryNameFilter.getText().toLowerCase());
            return matchesCode && matchesName;
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
            loadCategories();
        }
    }
}
