package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import services.AnalyticsService;
import services.CategoryService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ChartsController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    // Multi-select category dropdown (customized ComboBox)
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();

    // For multi-select support: list of checkboxes and selected category names.
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCategoryDropdown();
    }

    private void setupCategoryDropdown() {
        // Load available categories from the service.
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(cat -> cat.getCategoryName())
                .collect(Collectors.toList());

        categoryDropdown.setPromptText("Select Categories");

        // Create a container (VBox) with a search field and a ListView of CheckBoxes.
        VBox categoryContainer = new VBox();
        TextField searchField = new TextField();
        searchField.setPromptText("Search category...");
        ListView<CheckBox> categoryListView = new ListView<>();
        categoryCheckBoxes.clear();

        // Create an "ALL" checkbox to select/deselect all options.
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

        // Filter the ListView based on search input.
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<CheckBox> filtered = categoryCheckBoxes.stream()
                    .filter(cb -> cb.getText().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            categoryListView.getItems().setAll(filtered);
            // Re-add the "ALL" checkbox at the top.
            categoryListView.getItems().add(0, allCheckBox);
        });

        categoryContainer.getChildren().addAll(searchField, categoryListView);

        // Wrap the container in a CustomMenuItem for use in the dropdown.
        CustomMenuItem customMenuItem = new CustomMenuItem(categoryContainer);
        customMenuItem.setHideOnClick(false);
        ContextMenu contextMenu = new ContextMenu(customMenuItem);

        // When the dropdown is clicked, show the custom context menu.
        categoryDropdown.setOnMouseClicked(event -> {
            contextMenu.show(categoryDropdown,
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getX(),
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getY());
        });

        // Display selected categories as the button text.
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

        if (fromDate == null || toDate == null || selectedCategories.isEmpty()) {
            showAlert("Input Error", "Please select a valid date range and at least one category.");
            return;
        }

        // Clear previous chart data.
        pieChart.getData().clear();
        lineChart.getData().clear();

        // --- PIE CHART DATA ---
        // Instead of product distribution, we now want a category-based aggregation.
        // We assume AnalyticsService.getQuantityByCategory(...) returns a Map<String, Integer>
        // where each key is a category name and the value is the total quantity.
        Map<String, Integer> categoryQuantities = analyticsService.getQuantityByCategory(fromDate, toDate, selectedCategories);
        int totalQuantity = categoryQuantities.values().stream().mapToInt(Integer::intValue).sum();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : categoryQuantities.entrySet()) {
            double percent = totalQuantity > 0 ? (entry.getValue() * 100.0) / totalQuantity : 0;
            // Display the category name and percentage in the label.
            String label = entry.getKey() + " (" + String.format("%.1f", percent) + "%)";
            pieData.add(new PieChart.Data(label, entry.getValue()));
        }
        pieChart.setData(pieData);

        // --- LINE CHART DATA ---
        // For the line chart, we keep showing trends by product label.
        // We'll combine trend data from each selected category.
        Map<String, Map<String, Integer>> combinedTrend = new HashMap<>();
        for (String cat : selectedCategories) {
            Map<String, Map<String, Integer>> trendData = analyticsService.getSalesTrendByLabel(fromDate, toDate, cat);
            for (Map.Entry<String, Map<String, Integer>> seriesEntry : trendData.entrySet()) {
                String prodLabel = seriesEntry.getKey();
                Map<String, Integer> trendForLabel = seriesEntry.getValue();
                combinedTrend.computeIfAbsent(prodLabel, k -> new HashMap<>());
                Map<String, Integer> existingTrend = combinedTrend.get(prodLabel);
                for (Map.Entry<String, Integer> trendEntry : trendForLabel.entrySet()) {
                    existingTrend.merge(trendEntry.getKey(), trendEntry.getValue(), Integer::sum);
                }
            }
        }
        // Create a series for each product label.
        for (Map.Entry<String, Map<String, Integer>> entry : combinedTrend.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            Map<String, Integer> dateQuantityMap = entry.getValue();
            List<String> dates = new ArrayList<>(dateQuantityMap.keySet());
            dates.sort(String::compareTo);
            for (String dateStr : dates) {
                series.getData().add(new XYChart.Data<>(dateStr, dateQuantityMap.get(dateStr)));
            }
            lineChart.getData().add(series);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
