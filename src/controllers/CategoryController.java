package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import services.CategoryService;
import utils.CSVImporter;
import models.Category;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.File;
import java.util.List;

public class CategoryController {
    @FXML private Button btnImportCategories;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> colCategoryCode;
    @FXML private TableColumn<Category, String> colCategoryName;

    private final CategoryService categoryService = new CategoryService();

    @FXML
    public void initialize() {
        colCategoryCode.setCellValueFactory(new PropertyValueFactory<>("categoryCode"));
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        loadCategories();
    }

    private void loadCategories() {
        List<Category> categories = categoryService.getAllCategories();
        categoryTable.setItems(FXCollections.observableArrayList(categories));
        categoryTable.refresh();
    }

    @FXML
    private void importCategoryCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Category CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductCategoryCSV(selectedFile.getAbsolutePath());
            loadCategories(); // Refresh category list
        }
    }
}
