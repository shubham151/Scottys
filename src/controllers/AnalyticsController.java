package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
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
    @FXML private TableColumn<AnalyticsData, Double> colCost;
    @FXML private TableColumn<AnalyticsData, Double> colRetail;
    @FXML private TableColumn<AnalyticsData, Double> colTotalCost;
    @FXML private TableColumn<AnalyticsData, Double> colTotalRetail;
    @FXML private TableColumn<AnalyticsData, Integer> colQuantity;
    @FXML private TableColumn<AnalyticsData, Double> colSales;
    // Header filters: for string/integer columns, use TextFields; for numeric columns, use comparator + TextField.
    @FXML private TextField tfAnalyticsItemNumberFilter;
    @FXML private TextField tfAnalyticsLabelFilter;
    @FXML private TextField tfAnalyticsCategoryFilter;
    @FXML private ComboBox<String> cbCostComparator;
    @FXML private TextField tfCostFilter;
    @FXML private ComboBox<String> cbRetailComparator;
    @FXML private TextField tfRetailFilter;
    @FXML private ComboBox<String> cbTotalCostComparator;
    @FXML private TextField tfTotalCostFilter;
    @FXML private ComboBox<String> cbTotalRetailComparator;
    @FXML private TextField tfTotalRetailFilter;
    @FXML private ComboBox<String> cbQuantityComparator;
    @FXML private TextField tfQuantityAnalyticsFilter;
    // Removed cbSalesComparator and tfSalesFilter as they're not used

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();
    private FilteredList<AnalyticsData> filteredAnalyticsData;
    // This list will hold the currently selected categories from the custom dropdown.
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();
    // List of checkboxes used in the custom dropdown.
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        colItemNumber.setCellValueFactory(data -> data.getValue().itemNumberProperty().asObject());
        colLabel.setCellValueFactory(data -> data.getValue().labelProperty());
        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colCost.setCellValueFactory(data -> data.getValue().costProperty().asObject());
        colRetail.setCellValueFactory(data -> data.getValue().retailProperty().asObject());
        colTotalCost.setCellValueFactory(data -> data.getValue().totalCostProperty().asObject());
        colTotalRetail.setCellValueFactory(data -> data.getValue().totalRetailProperty().asObject());
        colQuantity.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());

        // Initialize filteredAnalyticsData with an empty list so that updateAnalyticsFilters() won't throw a NullPointerException.
        filteredAnalyticsData = new FilteredList<>(FXCollections.observableArrayList(), a -> true);
        analyticsTable.setItems(filteredAnalyticsData);

        setupCategoryDropdown();

        // Delay setting defaults until the scene is fully loaded.
        javafx.application.Platform.runLater(() -> {
            cbCostComparator.setValue("=");
            cbRetailComparator.setValue("=");
            cbTotalCostComparator.setValue("=");
            cbTotalRetailComparator.setValue("=");
            cbQuantityComparator.setValue("=");
        });

        setupAnalyticsHeaderFilters();
    }


    /**
     * Custom multi-select category dropdown that mimics the Charts dropdown.
     * It creates a context menu with a VBox containing a search field and a ListView of checkboxes.
     */
    private void setupCategoryDropdown() {
        // Fetch available categories from the service.
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(Category::getCategoryName)
                .collect(Collectors.toList());

        // Set default prompt.
        categoryDropdown.setPromptText("Select Categories");

        // Create a VBox container.
        VBox categoryContainer = new VBox();

        // Create a search field for filtering.
        TextField searchField = new TextField();
        searchField.setPromptText("Search category...");

        // Create a ListView for checkboxes.
        ListView<CheckBox> categoryListView = new ListView<>();
        categoryCheckBoxes.clear();

        // Create an "ALL" checkbox.
        CheckBox allCheckBox = new CheckBox("ALL");
        allCheckBox.setSelected(false);
        allCheckBox.setOnAction(event -> {
            boolean isSelected = allCheckBox.isSelected();
            for (CheckBox cb : categoryCheckBoxes) {
                cb.setSelected(isSelected);
            }
            updateSelectedCategories();
        });
        categoryListView.getItems().add(allCheckBox);

        // Add each category as a checkbox.
        for (String cat : categories) {
            CheckBox checkBox = new CheckBox(cat);
            checkBox.setSelected(false);
            checkBox.setOnAction(event -> {
                updateSelectedCategories();
                if (!checkBox.isSelected()) {
                    allCheckBox.setSelected(false);
                }
            });
            categoryCheckBoxes.add(checkBox);
            categoryListView.getItems().add(checkBox);
        }

        // Add a listener to the search field to filter the checkboxes.
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<CheckBox> filtered = categoryCheckBoxes.stream()
                    .filter(cb -> cb.getText().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            categoryListView.getItems().setAll(filtered);
            // Re-add the "ALL" checkbox at the top.
            categoryListView.getItems().add(0, allCheckBox);
        });

        // Add the search field and the ListView to the container.
        categoryContainer.getChildren().addAll(searchField, categoryListView);

        // Wrap the container in a CustomMenuItem.
        CustomMenuItem customMenuItem = new CustomMenuItem(categoryContainer);
        customMenuItem.setHideOnClick(false);

        // Create a ContextMenu and attach it to the dropdown.
        ContextMenu contextMenu = new ContextMenu(customMenuItem);
        categoryDropdown.setOnMouseClicked(event -> {
            contextMenu.show(categoryDropdown,
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getX(),
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getY());
        });

        // Update the button cell to display the selected categories.
        categoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedCategories.isEmpty() ? "Select Categories" : selectedCategories.toString());
            }
        });
    }

    /**
     * Updates the list of selected categories based on the checkbox states.
     */
    private void updateSelectedCategories() {
        selectedCategories.clear();
        for (CheckBox cb : categoryCheckBoxes) {
            if (cb.isSelected()) {
                selectedCategories.add(cb.getText());
            }
        }
        categoryDropdown.setPromptText(selectedCategories.isEmpty() ? "Select Categories" : selectedCategories.toString());
    }

    @FXML
    private void applyFilters() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        // If no date range or no categories selected, clear the table.
        if (fromDate == null || toDate == null || selectedCategories.isEmpty()) {
            analyticsTable.getItems().clear();
            return;
        }
        // Pass the selected categories (which now come from the custom dropdown) to the service.
        List<AnalyticsData> data = analyticsService.getAnalyticsData(fromDate, toDate, selectedCategories);
        filteredAnalyticsData = new FilteredList<>(FXCollections.observableArrayList(data), a -> true);
        analyticsTable.setItems(filteredAnalyticsData);
        setupAnalyticsHeaderFilters();
    }

    private void setupAnalyticsHeaderFilters() {
        tfAnalyticsItemNumberFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfAnalyticsLabelFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfAnalyticsCategoryFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        cbCostComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfCostFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        cbRetailComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfRetailFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        cbTotalCostComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfTotalCostFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        cbTotalRetailComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfTotalRetailFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        cbQuantityComparator.valueProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
        tfQuantityAnalyticsFilter.textProperty().addListener((obs, oldVal, newVal) -> updateAnalyticsFilters());
    }

    private void updateAnalyticsFilters() {
        filteredAnalyticsData.setPredicate(a -> {
            boolean matchesItem = true;
            String itemFilter = tfAnalyticsItemNumberFilter.getText().trim();
            if (!itemFilter.isEmpty()) {
                try {
                    int filterVal = Integer.parseInt(itemFilter);
                    matchesItem = a.getItemNumber() == filterVal;
                } catch (NumberFormatException e) {
                    matchesItem = false;
                }
            }
            String labelFilter = tfAnalyticsLabelFilter.getText().trim().toLowerCase();
            boolean matchesLabel = labelFilter.isEmpty() || a.getLabel().toLowerCase().contains(labelFilter);
            String categoryFilter = tfAnalyticsCategoryFilter.getText().trim().toLowerCase();
            boolean matchesCategory = categoryFilter.isEmpty() || a.getCategory().toLowerCase().contains(categoryFilter);
            boolean matchesCost = true;
            String costFilter = tfCostFilter.getText().trim();
            String costComp = cbCostComparator.getValue();
            if (costComp == null || costComp.isEmpty()) costComp = "=";
            if (!costFilter.isEmpty()) {
                try {
                    double filterCost = Double.parseDouble(costFilter);
                    switch (costComp) {
                        case ">": matchesCost = a.getCost() > filterCost; break;
                        case "<": matchesCost = a.getCost() < filterCost; break;
                        case "=": matchesCost = Math.abs(a.getCost() - filterCost) < 0.001; break;
                    }
                } catch (NumberFormatException e) {
                    matchesCost = false;
                }
            }
            boolean matchesRetail = true;
            String retailFilter = tfRetailFilter.getText().trim();
            String retailComp = cbRetailComparator.getValue();
            if (retailComp == null || retailComp.isEmpty()) retailComp = "=";
            if (!retailFilter.isEmpty()) {
                try {
                    double filterRetail = Double.parseDouble(retailFilter);
                    switch (retailComp) {
                        case ">": matchesRetail = a.getRetail() > filterRetail; break;
                        case "<": matchesRetail = a.getRetail() < filterRetail; break;
                        case "=": matchesRetail = Math.abs(a.getRetail() - filterRetail) < 0.001; break;
                    }
                } catch (NumberFormatException e) {
                    matchesRetail = false;
                }
            }
            boolean matchesTotalCost = true;
            String totalCostFilter = tfTotalCostFilter.getText().trim();
            String totalCostComp = cbTotalCostComparator.getValue();
            if (totalCostComp == null || totalCostComp.isEmpty()) totalCostComp = "=";
            if (!totalCostFilter.isEmpty()) {
                try {
                    double filterTotalCost = Double.parseDouble(totalCostFilter);
                    switch (totalCostComp) {
                        case ">": matchesTotalCost = a.getTotalCost() > filterTotalCost; break;
                        case "<": matchesTotalCost = a.getTotalCost() < filterTotalCost; break;
                        case "=": matchesTotalCost = Math.abs(a.getTotalCost() - filterTotalCost) < 0.001; break;
                    }
                } catch (NumberFormatException e) {
                    matchesTotalCost = false;
                }
            }
            boolean matchesTotalRetail = true;
            String totalRetailFilter = tfTotalRetailFilter.getText().trim();
            String totalRetailComp = cbTotalRetailComparator.getValue();
            if (totalRetailComp == null || totalRetailComp.isEmpty()) totalRetailComp = "=";
            if (!totalRetailFilter.isEmpty()) {
                try {
                    double filterTotalRetail = Double.parseDouble(totalRetailFilter);
                    switch (totalRetailComp) {
                        case ">": matchesTotalRetail = a.getTotalRetail() > filterTotalRetail; break;
                        case "<": matchesTotalRetail = a.getTotalRetail() < filterTotalRetail; break;
                        case "=": matchesTotalRetail = Math.abs(a.getTotalRetail() - filterTotalRetail) < 0.001; break;
                    }
                } catch (NumberFormatException e) {
                    matchesTotalRetail = false;
                }
            }
            boolean matchesQuantity = true;
            String quantityFilter = tfQuantityAnalyticsFilter.getText().trim();
            String quantityComp = cbQuantityComparator.getValue();
            if (quantityComp == null || quantityComp.isEmpty()) quantityComp = "=";
            if (!quantityFilter.isEmpty()) {
                try {
                    int filterQty = Integer.parseInt(quantityFilter);
                    switch (quantityComp) {
                        case ">": matchesQuantity = a.getQuantity() > filterQty; break;
                        case "<": matchesQuantity = a.getQuantity() < filterQty; break;
                        case "=": matchesQuantity = a.getQuantity() == filterQty; break;
                    }
                } catch (NumberFormatException e) {
                    matchesQuantity = false;
                }
            }
            return matchesItem && matchesLabel && matchesCategory && matchesCost &&
                    matchesRetail && matchesTotalCost && matchesTotalRetail &&
                    matchesQuantity;
        });
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
                writer.write("\uFEFF");
                writer.write("\"Item Number\",\"Label\",\"Category\",\"Price\",\"Quantity\",\"Sales\"\n");
                for (AnalyticsData data : currentData) {
                    writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%.2f\",\"%d\",\"%.2f\"\n",
                            data.getItemNumber(),
                            data.getLabel().replace("\"", "\"\""),
                            data.getCategory().replace("\"", "\"\""),
                            data.getRetail(),
                            data.getQuantity(),
                            data.getRetail() * data.getQuantity()));
                }
                showAlert("Export Successful", "CSV file has been saved successfully.");
            } catch (IOException e) {
                showAlert("Export Error", "Error exporting CSV: " + e.getMessage());
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
                            double price = data.getRetail();
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
