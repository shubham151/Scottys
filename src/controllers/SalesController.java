package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.SalesService;
import utils.CSVImporter;
import models.Sale;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.List;


public class SalesController {
    @FXML private Button btnImportSales;
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, Integer> colItemNumber;
    @FXML private TableColumn<Sale, Integer> colQuantity;
    @FXML private TableColumn<Sale, Double> colPrice;
    @FXML private TableColumn<Sale, String> colFromDate;
    @FXML private TableColumn<Sale, String> colToDate;

    private final SalesService salesService = new SalesService();

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colFromDate.setCellValueFactory(new PropertyValueFactory<>("fromDate"));
        colToDate.setCellValueFactory(new PropertyValueFactory<>("toDate"));

        loadSales();
    }

    private void loadSales() {
        List<Sale> sales = salesService.getAllSales();
        salesTable.setItems(FXCollections.observableArrayList(sales));
    }

    @FXML
    private void importSalesCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Sales CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importSalesCSV(selectedFile.getAbsolutePath());
            loadSales(); // Refresh the table
        }
    }
}
