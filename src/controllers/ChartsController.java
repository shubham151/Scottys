package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
    @FXML private ComboBox<String> subcategoryDropdown;
    @FXML private ComboBox<String> viewByPieChart;
    @FXML private ComboBox<String> viewByLineChart;
    @FXML private PieChart pieChart;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();

    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final List<CheckBox> subcategoryCheckBoxes = new ArrayList<>();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();
    private final ObservableList<String> selectedSubcategories = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupCategoryDropdown();
        setupSubcategoryDropdown();

        viewByPieChart.setItems(FXCollections.observableArrayList("Category", "Subcategory"));
        viewByPieChart.setValue("Category");
        viewByPieChart.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        viewByLineChart.setItems(FXCollections.observableArrayList("Category", "Subcategory", "Product"));
        viewByLineChart.setValue("Category");
        viewByLineChart.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupCategoryDropdown() {
        List<String> categories = categoryService.getAllCategories().stream()
                .map(Category::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        setupMultiSelectDropdown(categoryDropdown, categories, categoryCheckBoxes, selectedCategories, "Categories");

        selectedCategories.addListener((ListChangeListener<String>) change -> {
            updateSubcategoryOptions();
            applyFilters();
        });
    }

    private void setupSubcategoryDropdown() {
        updateSubcategoryOptions();

        selectedSubcategories.addListener((ListChangeListener<String>) change -> applyFilters());
    }

    private void updateSubcategoryOptions() {
        List<String> subcategories = categoryService.getAllCategories().stream()
                .filter(cat -> selectedCategories.contains(cat.getCategory()))
                .map(Category::getSubcategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        setupMultiSelectDropdown(subcategoryDropdown, subcategories, subcategoryCheckBoxes, selectedSubcategories, "Subcategories");
    }

    private void setupMultiSelectDropdown(ComboBox<String> dropdown, List<String> options, List<CheckBox> checkboxes,
                                          ObservableList<String> selectedList, String labelText) {
        dropdown.setPromptText("Select " + labelText);
        dropdown.setEditable(false);
        dropdown.setVisibleRowCount(1);

        VBox container = new VBox();
        TextField searchField = new TextField();
        searchField.setPromptText("Search " + labelText.toLowerCase());

        ListView<CheckBox> listView = new ListView<>();
        checkboxes.clear();

        CheckBox allBox = new CheckBox("ALL");
        allBox.setOnAction(e -> {
            boolean selected = allBox.isSelected();
            checkboxes.forEach(cb -> cb.setSelected(selected));
            updateSelectedList(checkboxes, selectedList, dropdown);
        });
        listView.getItems().add(allBox);

        for (String option : options) {
            CheckBox cb = new CheckBox(option);
            cb.setOnAction(e -> {
                updateSelectedList(checkboxes, selectedList, dropdown);
                if (!cb.isSelected()) allBox.setSelected(false);
            });
            checkboxes.add(cb);
            listView.getItems().add(cb);
        }

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String term = newVal.toLowerCase();
            List<CheckBox> filtered = checkboxes.stream()
                    .filter(cb -> cb.getText().toLowerCase().contains(term))
                    .collect(Collectors.toList());
            listView.getItems().setAll(allBox);
            listView.getItems().addAll(filtered);
        });

        container.getChildren().addAll(searchField, listView);
        CustomMenuItem menuItem = new CustomMenuItem(container);
        menuItem.setHideOnClick(false);

        ContextMenu contextMenu = new ContextMenu(menuItem);
        dropdown.setOnMouseClicked(e -> {
            contextMenu.show(dropdown,
                    dropdown.localToScreen(0, dropdown.getHeight()).getX(),
                    dropdown.localToScreen(0, dropdown.getHeight()).getY());
        });

        dropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedList.isEmpty() ? "Select " + labelText : String.join(", ", selectedList));
            }
        });
    }

    private void updateSelectedList(List<CheckBox> checkboxes, ObservableList<String> list, ComboBox<String> dropdown) {
        list.clear();
        checkboxes.stream().filter(CheckBox::isSelected).forEach(cb -> list.add(cb.getText()));
        dropdown.setPromptText(list.isEmpty() ? dropdown.getId() : String.join(", ", list));
    }

    @FXML
    private void applyFilters() {
        applyPieChart();
        applyLineChart();
    }

    private void applyPieChart() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) return;

        String viewBy = viewByPieChart.getValue();
        Map<String, Integer> data;

        if ("Subcategory".equals(viewBy)) {
            if (selectedSubcategories.isEmpty() || selectedCategories.isEmpty()) return;
            List<String> selectedPairs = new ArrayList<>();
            for (String cat : selectedCategories) {
                for (String sub : selectedSubcategories) {
                    selectedPairs.add(cat + " - " + sub);
                }
            }
            data = analyticsService.getQuantityBySubcategory(fromDate, toDate, selectedPairs);
        } else {
            if (selectedCategories.isEmpty()) return;
            data = analyticsService.getQuantityByCategory(fromDate, toDate, selectedCategories);
        }

        pieChart.getData().clear();
        data.forEach((key, value) -> pieChart.getData().add(new PieChart.Data(key, value)));
    }


    private void applyLineChart() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) return;

        String viewBy = viewByLineChart.getValue();
        lineChart.getData().clear();

        if ("Category".equals(viewBy)) {
            for (String cat : selectedCategories) {
                Map<String, Map<String, Integer>> trends = analyticsService.getSalesTrendByCategory(fromDate, toDate, cat);
                plotLineChart(trends);
            }
        } else if ("Subcategory".equals(viewBy)) {
            for (String sub : selectedSubcategories) {
                Map<String, Map<String, Integer>> trends = analyticsService.getSalesTrendBySubcategory(fromDate, toDate, sub);
                plotLineChart(trends);
            }
        } else {
            for (String cat : selectedCategories) {
                Map<String, Map<String, Integer>> trends = analyticsService.getSalesTrendByLabel(fromDate, toDate, cat);
                plotLineChart(trends);
            }
        }
    }

    private void plotLineChart(Map<String, Map<String, Integer>> data) {
        for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(entry.getKey());
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
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