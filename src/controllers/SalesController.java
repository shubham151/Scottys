package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.SalesService;
import models.Sale;
import java.util.List;

public class SalesController {
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, Integer> colItemNumber;
    @FXML private TableColumn<Sale, String> colDate;
    @FXML private TableColumn<Sale, Double> colAmount;

    private final SalesService salesService = new SalesService();

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        loadSales();
    }

    private void loadSales() {
        List<Sale> sales = salesService.getAllSales();
        salesTable.setItems(FXCollections.observableArrayList(sales));
    }
}
