package controllers;

import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Sale;
import services.SalesService;
import utils.CSVImporter;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class SalesController {
    @FXML private Button btnImportSales;
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, Integer> colItemNumber;
    @FXML private TableColumn<Sale, Integer> colQuantity;
    @FXML private TableColumn<Sale, Double> colPrice;
    @FXML private TableColumn<Sale, String> colFromDate;
    @FXML private TableColumn<Sale, String> colToDate;
    @FXML private TextField tfSalesItemNumberFilter;
    @FXML private TextField tfQuantityFilter;
    @FXML private TextField tfSalesPriceFilter;
    @FXML private ComboBox<String> cbFromDateComparator;
    @FXML private DatePicker dpFromDateFilter;
    @FXML private ComboBox<String> cbToDateComparator;
    @FXML private DatePicker dpToDateFilter;

    private final SalesService salesService = new SalesService();
    private FilteredList<Sale> filteredSales;

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colFromDate.setCellValueFactory(new PropertyValueFactory<>("fromDate"));
        colToDate.setCellValueFactory(new PropertyValueFactory<>("toDate"));
        loadSales();
        setupFilters();
    }

    private void loadSales() {
        List<Sale> sales = salesService.getAllSales();
        filteredSales = new FilteredList<>(FXCollections.observableArrayList(sales), s -> true);
        salesTable.setItems(filteredSales);
    }

    private void setupFilters() {
        tfSalesItemNumberFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfQuantityFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        tfSalesPriceFilter.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        cbFromDateComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        dpFromDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        cbToDateComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        dpToDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
    }

    private void updateFilters() {
        filteredSales.setPredicate(sale -> {
            boolean matchesItem = true;
            String itemFilter = tfSalesItemNumberFilter.getText().trim();
            if (!itemFilter.isEmpty()) {
                try {
                    int filterVal = Integer.parseInt(itemFilter);
                    matchesItem = sale.getItemNumber() == filterVal;
                } catch (NumberFormatException e) {
                    matchesItem = false;
                }
            }
            boolean matchesQuantity = true;
            String qtyFilter = tfQuantityFilter.getText().trim();
            if (!qtyFilter.isEmpty()) {
                try {
                    int filterQty = Integer.parseInt(qtyFilter);
                    matchesQuantity = sale.getQuantity() == filterQty;
                } catch (NumberFormatException e) {
                    matchesQuantity = false;
                }
            }
            boolean matchesPrice = true;
            String priceFilter = tfSalesPriceFilter.getText().trim();
            if (!priceFilter.isEmpty()) {
                try {
                    double filterPrice = Double.parseDouble(priceFilter);
                    matchesPrice = sale.getPrice() == filterPrice;
                } catch (NumberFormatException e) {
                    matchesPrice = false;
                }
            }
            boolean matchesFromDate = true;
            LocalDate filterFrom = dpFromDateFilter.getValue();
            String fromComparator = cbFromDateComparator.getValue();
            if (fromComparator == null || fromComparator.isEmpty()) {
                fromComparator = "=";
            }
            if (filterFrom != null) {
                switch (fromComparator) {
                    case ">": matchesFromDate = sale.getFromDate().isAfter(filterFrom); break;
                    case "<": matchesFromDate = sale.getFromDate().isBefore(filterFrom); break;
                    case "=": matchesFromDate = sale.getFromDate().equals(filterFrom); break;
                }
            }
            boolean matchesToDate = true;
            LocalDate filterTo = dpToDateFilter.getValue();
            String toComparator = cbToDateComparator.getValue();
            if (toComparator == null || toComparator.isEmpty()) {
                toComparator = "=";
            }
            if (filterTo != null) {
                switch (toComparator) {
                    case ">": matchesToDate = sale.getToDate().isAfter(filterTo); break;
                    case "<": matchesToDate = sale.getToDate().isBefore(filterTo); break;
                    case "=": matchesToDate = sale.getToDate().equals(filterTo); break;
                }
            }
            return matchesItem && matchesQuantity && matchesPrice && matchesFromDate && matchesToDate;
        });
    }

    @FXML
    private void importSalesCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Sales CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importSalesCSV(selectedFile.getAbsolutePath());
            loadSales();
        }
    }
}
