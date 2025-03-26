package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import models.Category;
import services.AnalyticsService;
import services.CategoryService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ChartsController {
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private ComboBox<String> viewByPieChart;
    @FXML private ComboBox<String> viewByLineChart;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();

    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCategoryDropdown();

        viewByPieChart.setItems(FXCollections.observableArrayList("Category", "Subcategory"));
        viewByPieChart.setValue("Category");
        viewByPieChart.valueProperty().addListener((obs, oldVal, newVal) -> applyPieChart());

        viewByLineChart.setItems(FXCollections.observableArrayList("Category", "Subcategory", "Product"));
        viewByLineChart.setValue("Category");
        viewByLineChart.valueProperty().addListener((obs, oldVal, newVal) -> applyLineChart());
    }

    private void setupCategoryDropdown() {
        List<String> categories = categoryService.getAllCategories()
                .stream()
                .map(cat -> cat.getCategory() + " - " + cat.getSubcategory())
                .collect(Collectors.toList());

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
            for (CheckBox cb : categoryCheckBoxes) {
                cb.setSelected(isSelected);
            }
            updateSelectedCategories();
        });
        categoryListView.getItems().add(allCheckBox);

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

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<CheckBox> filtered = categoryCheckBoxes.stream()
                    .filter(cb -> cb.getText().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            categoryListView.getItems().setAll(filtered);
            categoryListView.getItems().add(0, allCheckBox);
        });

        categoryContainer.getChildren().addAll(searchField, categoryListView);
        CustomMenuItem customMenuItem = new CustomMenuItem(categoryContainer);
        customMenuItem.setHideOnClick(false);
        ContextMenu contextMenu = new ContextMenu(customMenuItem);
        categoryDropdown.setOnMouseClicked(event -> {
            contextMenu.show(categoryDropdown,
                    categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getX(),
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
        for (CheckBox cb : categoryCheckBoxes) {
            if (cb.isSelected()) {
                selectedCategories.add(cb.getText());
            }
        }
        categoryDropdown.setPromptText(selectedCategories.isEmpty() ? "Select Categories" : selectedCategories.toString());
    }

    @FXML
    private void applyFilters() {
        applyPieChart();
        applyLineChart();
    }

    private void applyPieChart() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null || selectedCategories.isEmpty()) return;

        pieChart.getData().clear();
        String pieViewBy = viewByPieChart.getValue();

        Map<String, Integer> quantityMap = "Subcategory".equals(pieViewBy)
                ? analyticsService.getQuantityBySubcategory(fromDate, toDate, selectedCategories)
                : analyticsService.getQuantityByCategory(fromDate, toDate, selectedCategories);

        renderPieChart(quantityMap);
    }

    private void applyLineChart() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null || selectedCategories.isEmpty()) return;

        lineChart.getData().clear();
        String lineViewBy = viewByLineChart.getValue();
        Map<String, Map<String, Integer>> combinedTrend = new HashMap<>();

        for (String selected : selectedCategories) {
            String[] parts = selected.split(" - ", 2);
            String category = parts[0];
            String subcategory = parts.length > 1 ? parts[1] : "";

            Map<String, Map<String, Integer>> trendData;

            if ("Category".equals(lineViewBy)) {
                trendData = analyticsService.getSalesTrendByCategory(fromDate, toDate, category);
            } else if ("Subcategory".equals(lineViewBy)) {
                trendData = analyticsService.getSalesTrendBySubcategory(fromDate, toDate, subcategory);
            } else {
                trendData = analyticsService.getSalesTrendByLabel(fromDate, toDate, category);
            }

            for (Map.Entry<String, Map<String, Integer>> entry : trendData.entrySet()) {
                String seriesName = entry.getKey();
                Map<String, Integer> dataPoints = entry.getValue();
                combinedTrend.computeIfAbsent(seriesName, k -> new HashMap<>());
                Map<String, Integer> combinedSeries = combinedTrend.get(seriesName);
                for (Map.Entry<String, Integer> dp : dataPoints.entrySet()) {
                    combinedSeries.merge(dp.getKey(), dp.getValue(), Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Map<String, Integer>> entry : combinedTrend.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
            lineChart.getData().add(series);
        }
    }

    private void renderPieChart(Map<String, Integer> quantityMap) {
        int total = quantityMap.values().stream().mapToInt(Integer::intValue).sum();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : quantityMap.entrySet()) {
            double percent = total > 0 ? (entry.getValue() * 100.0) / total : 0;
            String label = entry.getKey() + " (" + String.format("%.1f", percent) + "%)";
            pieData.add(new PieChart.Data(label, entry.getValue()));
        }

        pieChart.setData(pieData);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}