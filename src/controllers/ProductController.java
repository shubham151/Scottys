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

        colCategory.setCellFactory(column -> new ComboBoxTableCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle(""); // Reset style
                } else {
                    setText(item);
                    setTextFill(Color.BLACK);
                }
            }
        });
        colTaxRate1.setCellValueFactory(new PropertyValueFactory<>("taxRate1"));
        colTaxRate2.setCellValueFactory(new PropertyValueFactory<>("taxRate2"));
        colTaxRate3.setCellValueFactory(new PropertyValueFactory<>("taxRate3"));
        colTaxRate4.setCellValueFactory(new PropertyValueFactory<>("taxRate4"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("recStatus"));

        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(categoryList));
        colCategory.setOnEditCommit(event -> updateProductCategory(event.getRowValue(), event.getNewValue()));
    }

    private void loadCategories() {
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(Category::getCategoryName)
                .toList();
        if(categories.isEmpty()) {
            System.out.println("No category");

        } else {
          System.out.println("Category size: "+ categories.size());
        }
//        categoryDropdown.setItems(FXCollections.observableArrayList(categoryNames));
        categoryList.setAll(categories);
    }

    private void updateProductCategory(Product product, String newCategory) {
        product.setProductClass(newCategory);
        productService.updateProductCategory(product.getItemNumber(), newCategory);
        productTable.refresh();
    }

    private void loadProducts() {
        List<Product> productList = productService.getAllProducts();

        if (productList.isEmpty()) {
            System.out.println("No products found in the database.");
        } else {
            System.out.println(productList.size() + " products loaded.");
        }

        filteredData = new FilteredList<>(FXCollections.observableArrayList(productList), p -> true);
        productTable.setItems(filteredData);
        productTable.refresh(); // ✅ Force UI refresh
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryDropdown.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categoryDropdown.getValue();

        filteredData.setPredicate(product ->
                (searchText.isEmpty() || product.getLabel().toLowerCase().contains(searchText)) &&
                        (selectedCategory == null || selectedCategory.isEmpty() || product.getProductClass().equals(selectedCategory))
        );
    }

    @FXML
    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductsCSV(selectedFile.getAbsolutePath());
            loadProducts(); // Refresh product list
            productTable.refresh();
        }
    }
}
