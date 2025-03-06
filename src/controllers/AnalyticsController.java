package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import models.AnalyticsData;
import models.Category;
import services.AnalyticsService;
import services.CategoryService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnalyticsController {
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private TableView<AnalyticsData> analyticsTable;
    @FXML private TableColumn<AnalyticsData, Integer> colItemNumber;
    @FXML private TableColumn<AnalyticsData, String> colLabel;
    @FXML private TableColumn<AnalyticsData, String> colCategory;
    @FXML private TableColumn<AnalyticsData, Double> colPrice;
    @FXML private TableColumn<AnalyticsData, Integer> colQuantity;
    @FXML private TableColumn<AnalyticsData, Double> colSales;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(data -> data.getValue().itemNumberProperty().asObject());
        colLabel.setCellValueFactory(data -> data.getValue().labelProperty());
        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colPrice.setCellValueFactory(data -> data.getValue().priceProperty().asObject());
        colQuantity.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());
        colSales.setCellValueFactory(data -> data.getValue().salesProperty().asObject());

        analyticsTable.setItems(FXCollections.observableArrayList()); // Start with an empty table
        setupCategoryDropdown();
    }

    private void setupCategoryDropdown() {
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(Category::getCategoryName)
                .toList();

        categoryDropdown.setPromptText("Select Categories");

        VBox categoryContainer = new VBox();
        TextField searchField = new TextField();
        searchField.setPromptText("Search category...");
        ListView<CheckBox> categoryListView = new ListView<>();
        categoryCheckBoxes.clear();

        CheckBox allCheckBox = new CheckBox("ALL");
        allCheckBox.setSelected(false);
        allCheckBox.setOnAction(event -> {
            boolean isSelected = allCheckBox.isSelected();
            categoryCheckBoxes.forEach(cb -> cb.setSelected(isSelected));
            updateSelectedCategories();
        });

        categoryListView.getItems().add(allCheckBox);

        for (String category : categories) {
            CheckBox checkBox = new CheckBox(category);
            checkBox.setSelected(false);

            checkBox.setOnAction(event -> {
                updateSelectedCategories();
                if (!checkBox.isSelected()) {
                    allCheckBox.setSelected(false); // Uncheck "ALL" if any category is unchecked
                }
            });

            categoryCheckBoxes.add(checkBox);
            categoryListView.getItems().add(checkBox);
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String filter = newValue.toLowerCase();
            categoryListView.getItems().setAll(categoryCheckBoxes.stream()
                    .filter(cb -> cb.getText().toLowerCase().contains(filter))
                    .collect(Collectors.toList()));
            categoryListView.getItems().add(0, allCheckBox);
        });

        categoryContainer.getChildren().addAll(searchField, categoryListView);

        // Create a custom popup
        CustomMenuItem customMenuItem = new CustomMenuItem(categoryContainer);
        customMenuItem.setHideOnClick(false);

        ContextMenu contextMenu = new ContextMenu(customMenuItem);

        categoryDropdown.setOnMouseClicked(event -> {
            contextMenu.show(categoryDropdown, categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getX(),
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getY());
        });

        categoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedCategories.isEmpty() ? "Select Categories" : selectedCategories.toString());
            }
        });
    }

    private void updateSelectedCategories() {
        selectedCategories.clear();
        for (CheckBox checkBox : categoryCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedCategories.add(checkBox.getText());
            }
        }
        categoryDropdown.setPromptText(selectedCategories.isEmpty() ? "Select Categories" : selectedCategories.toString());
    }


    @FXML
    private void applyFilters() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null || selectedCategories.isEmpty()) {
            analyticsTable.getItems().clear(); // Clear table if any filter is missing
            return;
        }

        List<AnalyticsData> data = analyticsService.getAnalyticsData(fromDate, toDate, selectedCategories);
        analyticsTable.setItems(FXCollections.observableArrayList(data));
    }


    @FXML
    private void exportToCSV() {
        ObservableList<AnalyticsData> currentData = analyticsTable.getItems();

        if (currentData.isEmpty()) {
            showAlert("No data to export", "Please apply filters and generate data before exporting.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Analytics Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write("\uFEFF"); // UTF-8 BOM (helps Excel recognize UTF-8)

                writer.write("\"Item Number\",\"Label\",\"Category\",\"Price\",\"Quantity\",\"Sales\"\n");

                for (AnalyticsData data : currentData) {
                    // ✅ Ensure values are enclosed in quotes to prevent CSV structure breaking
                    writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%.2f\",\"%d\",\"%.2f\"\n",
                            data.getItemNumber(),
                            data.getLabel().replace("\"", "\"\""),  // Escape double quotes
                            data.getCategory().replace("\"", "\"\""),
                            data.getPrice(),
                            data.getQuantity(),
                            data.getSales()));
                }

                showAlert("Export Successful", "CSV file has been saved successfully.");
            } catch (IOException e) {
                showAlert("Export Error", "Error exporting CSV: " + e.getMessage());
                System.err.println("Error exporting CSV: " + e.getMessage());
            }
        }
    }


    @FXML
    private void exportWeeklyCSV() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) {
            showAlert("Invalid Date Range", "Please select a valid date range.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Weekly Analytics Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write("\uFEFF"); // UTF-8 BOM for Excel compatibility


                List<String> weekRanges = getWeekRanges(fromDate, toDate);

                List<AnalyticsData> weeklyData = analyticsService.getWeeklyAnalyticsData(fromDate, toDate, selectedCategories, weekRanges);

                writer.write("\"Item Number\",\"Label\",\"Category\"");
                for (String week : weekRanges) {
                    writer.write(",\"" + week + "\",\"\",\"\"");
                }
                writer.write("\n");

                writer.write("\"\",\"\",\"\",");
                for (int i = 0; i < weekRanges.size(); i++) {
                    writer.write("\"Price\",\"Quantity\",\"Sales\",");
                }
                writer.write("\n");

                for (AnalyticsData data : weeklyData) {
                    writer.write(String.format("\"%d\",\"%s\",\"%s\"",
                            data.getItemNumber(),
                            data.getLabel().replace("\"", "\"\""),
                            data.getCategory().replace("\"", "\"\"")));

                    for (String week : weekRanges) {
                        if (data.getWeeklySales().containsKey(week)) {
                            double price = data.getPrice();
                            int quantity = data.getWeeklySales().get(week);
                            double sales = price * quantity;
                            writer.write(String.format(",\"%.2f\",\"%d\",\"%.2f\"", price, quantity, sales));
                        } else {
                            writer.write(",\"0.00\",\"0\",\"0.00\"");
                        }
                    }
                    writer.write("\n");
                }

                showAlert("Export Successful", "Weekly CSV file has been saved successfully.");
            } catch (IOException e) {
                showAlert("Export Error", "Error exporting CSV: " + e.getMessage());
                System.err.println("Error exporting CSV: " + e.getMessage());
            }
        }
    }





    private List<String> getWeekRanges(LocalDate startDate, LocalDate endDate) {
        List<String> weekRanges = new ArrayList<>();
        LocalDate currentStart = startDate;

        while (!currentStart.isAfter(endDate)) {
            LocalDate currentEnd = currentStart.plusDays(6);
            if (currentEnd.isAfter(endDate)) {
                currentEnd = endDate;
            }

            weekRanges.add(currentStart + " to " + currentEnd);
            currentStart = currentEnd.plusDays(1);
        }

        return weekRanges;
    }






    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
