package controllers;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import utils.CSVImporter;
import java.io.File;
import models.Product;
import services.ProductService;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductController {

    @FXML private Button btnImport;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colItemNumber;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colTaxRate1;
    @FXML private TableColumn<Product, String> colTaxRate2;
    @FXML private TableColumn<Product, String> colTaxRate3;
    @FXML private TableColumn<Product, String> colTaxRate4;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colStatus;

    @FXML private TextField filterItemNumber;
    @FXML private TextField filterLabel;
    @FXML private TextField filterTaxRate1;
    @FXML private TextField filterTaxRate2;
    @FXML private TextField filterTaxRate3;
    @FXML private TextField filterTaxRate4;
    @FXML private TextField filterPrice;
    @FXML private TextField filterStatus;

    private final ProductService productService = new ProductService();
    private FilteredList<Product> filteredData;

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colTaxRate1.setCellValueFactory(new PropertyValueFactory<>("taxRate1"));
        colTaxRate2.setCellValueFactory(new PropertyValueFactory<>("taxRate2"));
        colTaxRate3.setCellValueFactory(new PropertyValueFactory<>("taxRate3"));
        colTaxRate4.setCellValueFactory(new PropertyValueFactory<>("taxRate4"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("recStatus"));

        loadProducts();
        setupFilters();
    }

    private void loadProducts() {
        filteredData = new FilteredList<>(FXCollections.observableArrayList(productService.getAllProducts()), p -> true);
        productTable.setItems(filteredData);
    }

    private void setupFilters() {
        filterItemNumber.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterLabel.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterTaxRate1.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterTaxRate2.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterTaxRate3.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterTaxRate4.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterPrice.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterStatus.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(product ->
                (filterItemNumber.getText().isEmpty() || String.valueOf(product.getItemNumber()).contains(filterItemNumber.getText())) &&
                        (filterLabel.getText().isEmpty() || product.getLabel().toLowerCase().contains(filterLabel.getText().toLowerCase())) &&
                        (filterTaxRate1.getText().isEmpty() || product.getTaxRate1().toLowerCase().contains(filterTaxRate1.getText().toLowerCase())) &&
                        (filterTaxRate2.getText().isEmpty() || product.getTaxRate2().toLowerCase().contains(filterTaxRate2.getText().toLowerCase())) &&
                        (filterTaxRate3.getText().isEmpty() || product.getTaxRate3().toLowerCase().contains(filterTaxRate3.getText().toLowerCase())) &&
                        (filterTaxRate4.getText().isEmpty() || product.getTaxRate4().toLowerCase().contains(filterTaxRate4.getText().toLowerCase())) &&
                        (filterPrice.getText().isEmpty() || String.valueOf(product.getPrice()).contains(filterPrice.getText())) &&
                        (filterStatus.getText().isEmpty() || product.getRecStatus().toLowerCase().contains(filterStatus.getText().toLowerCase()))
        );
    }

    @FXML
    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            System.out.println("Importing: " + selectedFile.getAbsolutePath());
            CSVImporter.importCSV(selectedFile.getAbsolutePath());
            loadProducts(); // Ensure new data is shown
        } else {
            System.out.println("No file selected.");
        }
    }
}
