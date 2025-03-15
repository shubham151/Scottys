package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import models.Category;
import models.Product;
import services.CategoryService;
import services.ProductService;
import utils.CSVImporter;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;

public class ProductController {

    @FXML private ComboBox<String> categoryDropdown;
    @FXML private TextField searchField;
    @FXML private Button btnImport;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colItemNumber;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colTaxRate1;
    @FXML private TableColumn<Product, String> colTaxRate2;
    @FXML private TableColumn<Product, String> colTaxRate3;
    @FXML private TableColumn<Product, String> colTaxRate4;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;

    // Header filters for simple text/integer columns
    @FXML private TextField tfItemNumberFilter;
    @FXML private TextField tfLabelFilter;
    @FXML private TextField tfCategoryFilter;
    @FXML private TextField tfStatusFilter;
    // For Price, we now have a comparator and a value field.
    @FXML private ComboBox<String> cbPriceComparator;
    @FXML private TextField tfPriceFilter;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private FilteredList<Product> filteredData;
    private ObservableList<String> categoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        productTable.setEditable(true);
        loadCategories();
        setupTableColumns();
        loadProducts();
        setupFilters();
    }

    private void setupTableColumns() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("productClass"));
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(categoryList));
        colCategory.setOnEditCommit(event -> updateProductCategory(event.getRowValue(), event.getNewValue()));
        colTaxRate1.setCellValueFactory(new PropertyValueFactory<>("taxRate1"));
        colTaxRate2.setCellValueFactory(new PropertyValueFactory<>("taxRate2"));
        colTaxRate3.setCellValueFactory(new PropertyValueFactory<>("taxRate3"));
        colTaxRate4.setCellValueFactory(new PropertyValueFactory<>("taxRate4"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("recStatus"));
    }

    private void loadCategories() {
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(Category::getCategoryName)
                .toList();
        categoryList.setAll(categories);
        categoryDropdown.setItems(FXCollections.observableArrayList(categories));
    }

    private void updateProductCategory(Product product, String newCategory) {
        product.setProductClass(newCategory);
        productService.updateProductCategory(product.getItemNumber(), newCategory);
        productTable.refresh();
    }

    private void loadProducts() {
        List<Product> productList = productService.getAllProducts();
        filteredData = new FilteredList<>(FXCollections.observableArrayList(productList), p -> true);
        productTable.setItems(filteredData);
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        categoryDropdown.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfItemNumberFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfLabelFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfCategoryFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfStatusFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        cbPriceComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfPriceFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
    }

    private void updateFilters() {
        filteredData.setPredicate(product -> {
            String searchText = searchField.getText().trim().toLowerCase();
            boolean matchesSearch = searchText.isEmpty() || product.getLabel().toLowerCase().contains(searchText);
            String selectedCategory = categoryDropdown.getValue();
            boolean matchesDropdown = selectedCategory == null || selectedCategory.isEmpty() ||
                    product.getProductClass().equalsIgnoreCase(selectedCategory);
            boolean matchesItemNumber = true;
            String itemNumberFilter = tfItemNumberFilter.getText().trim();
            if (!itemNumberFilter.isEmpty()) {
                try {
                    int filterVal = Integer.parseInt(itemNumberFilter);
                    matchesItemNumber = product.getItemNumber() == filterVal;
                } catch (NumberFormatException e) {
                    matchesItemNumber = false;
                }
            }
            String labelFilter = tfLabelFilter.getText().trim().toLowerCase();
            boolean matchesLabel = labelFilter.isEmpty() || product.getLabel().toLowerCase().contains(labelFilter);
            String categoryFilter = tfCategoryFilter.getText().trim().toLowerCase();
            boolean matchesCategory = categoryFilter.isEmpty() || product.getProductClass().toLowerCase().contains(categoryFilter);
            boolean matchesPrice = true;
            String priceFilter = tfPriceFilter.getText().trim();
            String comparator = cbPriceComparator.getValue();
            if (comparator == null || comparator.isEmpty()) {
                comparator = "=";
            }
            if (!priceFilter.isEmpty()) {
                try {
                    double filterPrice = Double.parseDouble(priceFilter);
                    switch (comparator) {
                        case ">": matchesPrice = product.getPrice() > filterPrice; break;
                        case "<": matchesPrice = product.getPrice() < filterPrice; break;
                        case "=": matchesPrice = product.getPrice() == filterPrice; break;
                        default: matchesPrice = true;
                    }
                } catch (NumberFormatException e) {
                    matchesPrice = false;
                }
            }
            String statusFilter = tfStatusFilter.getText().trim().toLowerCase();
            boolean matchesStatus = statusFilter.isEmpty() || product.getRecStatus().toLowerCase().contains(statusFilter);
            return matchesSearch && matchesDropdown && matchesItemNumber &&
                    matchesLabel && matchesCategory && matchesPrice && matchesStatus;
        });
    }

    @FXML
    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductsCSV(selectedFile.getAbsolutePath());
            loadProducts();
            productTable.refresh();
        }
    }
}
