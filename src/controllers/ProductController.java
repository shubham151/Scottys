package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import models.Product;
import services.CategoryService;
import services.ProductService;
import utils.CSVImporter;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProductController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private Button btnImport;
    @FXML private Button btnAddProduct;
    @FXML private Button btnDeleteProduct;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Number> colItemNumber;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colTaxRate1;
    @FXML private TableColumn<Product, String> colTaxRate2;
    @FXML private TableColumn<Product, String> colTaxRate3;
    @FXML private TableColumn<Product, String> colTaxRate4;
    @FXML private TableColumn<Product, Number> colPrice;
    @FXML private TableColumn<Product, String> colStatus;

    // Header filters for local criteria.
    @FXML private TextField tfItemNumberFilter;
    @FXML private TextField tfLabelFilter;
    @FXML private TextField tfCategoryFilter;
    @FXML private TextField tfStatusFilter;
    // For Price filter.
    @FXML private ComboBox<String> cbPriceComparator;
    @FXML private TextField tfPriceFilter;

    private final int PAGE_SIZE = 1000;
    private int currentOffset = 0;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private ObservableList<Product> productsList = FXCollections.observableArrayList();

    // List to track newly added products that haven't been saved.
    private final List<Product> newProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        productTable.setEditable(true);
        loadCategoriesForDropdown();
        setupTableColumns();
        loadInitialProducts();
        setupFilters();
        setupScrollListener();
    }

    private void setupTableColumns() {
        // Item Number column.
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colItemNumber.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter("0")));
        colItemNumber.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setItemNumber(event.getNewValue().intValue());
            attemptSaveProduct(p);
        });

        // Label column.
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colLabel.setCellFactory(TextFieldTableCell.forTableColumn());
        colLabel.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setLabel(event.getNewValue());
            attemptSaveProduct(p);
        });

        // Category column.
        colCategory.setCellValueFactory(new PropertyValueFactory<>("productClass"));
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(
                categoryService.getAllCategories()
                        .stream()
                        .map(cat -> cat.getCategoryName())
                        .toArray(String[]::new)
        ));
        colCategory.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setProductClass(event.getNewValue());
            attemptSaveProduct(p);
        });

        // Tax Rates columns.
        colTaxRate1.setCellValueFactory(new PropertyValueFactory<>("taxRate1"));
        colTaxRate2.setCellValueFactory(new PropertyValueFactory<>("taxRate2"));
        colTaxRate3.setCellValueFactory(new PropertyValueFactory<>("taxRate3"));
        colTaxRate4.setCellValueFactory(new PropertyValueFactory<>("taxRate4"));

        // Price column.
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        colPrice.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setPrice(event.getNewValue().doubleValue());
            attemptSaveProduct(p);
        });

        // Status column.
        colStatus.setCellValueFactory(new PropertyValueFactory<>("recStatus"));
    }

    private void loadCategoriesForDropdown() {
        List<String> cats = categoryService.getAllCategories()
                .stream()
                .map(cat -> cat.getCategoryName())
                .toList();
        categoryDropdown.setItems(FXCollections.observableArrayList(cats));
    }

    private String buildFilterQuery() {
        StringBuilder sb = new StringBuilder();
        String search = searchField.getText().trim();
        if (!search.isEmpty()) {
            sb.append(" AND lower(p.label) LIKE ?");
        }
        if (categoryDropdown.getValue() != null && !categoryDropdown.getValue().isEmpty()) {
            sb.append(" AND lower(c.category_name) = ?");
        }
        return sb.toString();
    }

    private List<Object> buildFilterParameters() {
        List<Object> params = new ArrayList<>();
        String search = searchField.getText().trim();
        if (!search.isEmpty()) {
            params.add("%" + search.toLowerCase() + "%");
        }
        String cat = categoryDropdown.getValue();
        if (cat != null && !cat.isEmpty()) {
            params.add(cat.toLowerCase());
        }
        return params;
    }

    private void loadInitialProducts() {
        currentOffset = 0;
        productsList.clear();
        String filterQuery = buildFilterQuery();
        List<Object> filterParams = buildFilterParameters();
        productsList.addAll(productService.getProducts(PAGE_SIZE, currentOffset, filterQuery, filterParams));
        productTable.setItems(productsList);
    }

    private void loadMoreProducts() {
        String filterQuery = buildFilterQuery();
        List<Object> filterParams = buildFilterParameters();
        currentOffset += PAGE_SIZE;
        List<Product> more = productService.getProducts(PAGE_SIZE, currentOffset, filterQuery, filterParams);
        productsList.addAll(more);
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        categoryDropdown.valueProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfItemNumberFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfLabelFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfCategoryFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfStatusFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        cbPriceComparator.valueProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfPriceFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
    }

    private void setupScrollListener() {
        productTable.setOnScroll(event -> {
            if (productTable.getItems().size() % PAGE_SIZE == 0 &&
                    productTable.getItems().size() > 0 &&
                    productTable.getFocusModel().getFocusedIndex() >= productTable.getItems().size() - 1) {
                loadMoreProducts();
            }
        });
    }

    /**
     * Helper method to attempt saving a product.
     * For new products (itemNumber == 0 or present in newProducts), it calls insert.
     * Otherwise, it calls update.
     * Only proceeds if required fields (e.g. label and category) are nonempty.
     */
    private void attemptSaveProduct(Product p) {
        // Ensure that required fields are provided.
        if (p.getLabel().isEmpty() || p.getProductClass().isEmpty()) {
            return;
        }
        if (p.getItemNumber() == 0 || newProducts.contains(p)) {
            boolean inserted = productService.insertProduct(p);
            if (inserted) {
                newProducts.remove(p);
                showAlert("Success", "Product added successfully.");
            } else {
                showAlert("Save Error", "Failed to add new product.");
            }
        } else {
            productService.updateProduct(p);
            showAlert("Success", "Product updated successfully.");
        }
        loadInitialProducts();
    }

    @FXML
    private void addProduct() {
        // Create a new product with default values.
        Product newProduct = new Product(0, "", "", "N/A", "N/A", "N/A", "N/A", 0.0, "Active");
        newProducts.add(newProduct);
        productsList.add(0, newProduct);
        productTable.requestFocus();
        productTable.getSelectionModel().clearAndSelect(0);
        productTable.getFocusModel().focus(0);
        productTable.edit(0, colLabel);
    }

    @FXML
    private void deleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (productService.deleteProduct(selected.getItemNumber())) {
                productsList.remove(selected);
                newProducts.remove(selected);
            } else {
                showAlert("Delete Error", "Could not delete product.");
            }
        } else {
            showAlert("Selection Error", "No product selected.");
        }
    }

    @FXML
    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductsCSV(selectedFile.getAbsolutePath());
            loadInitialProducts();
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
