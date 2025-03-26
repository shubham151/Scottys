package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsController {
    public TextField tfAnalyticsItemNumberFilter;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private ComboBox<String> subcategoryDropdown;
    @FXML private TableView<AnalyticsData> analyticsTable;
    @FXML private TableColumn<AnalyticsData, Integer> colItemNumber;
    @FXML private TableColumn<AnalyticsData, String> colLabel;
    @FXML private TableColumn<AnalyticsData, String> colCategory;
    @FXML private TableColumn<AnalyticsData, String> colSubcategory;
    @FXML private TableColumn<AnalyticsData, Double> colCost;
    @FXML private TableColumn<AnalyticsData, Double> colRetail;
    @FXML private TableColumn<AnalyticsData, Double> colTotalCost;
    @FXML private TableColumn<AnalyticsData, Double> colTotalRetail;
    @FXML private TableColumn<AnalyticsData, Integer> colQuantity;

    private final AnalyticsService analyticsService = new AnalyticsService();
    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();
    private final ObservableList<String> selectedSubcategories = FXCollections.observableArrayList();
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final List<CheckBox> subcategoryCheckBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        setupColumns();
        setupCategoryDropdown();
        setupSubcategoryDropdown();
    }

    private void setupColumns() {
        colItemNumber.setCellValueFactory(data -> data.getValue().itemNumberProperty().asObject());
        colLabel.setCellValueFactory(data -> data.getValue().labelProperty());
        colCategory.setCellValueFactory(data -> data.getValue().categoryProperty());
        colSubcategory.setCellValueFactory(data -> data.getValue().subcategoryProperty());
        colCost.setCellValueFactory(data -> data.getValue().costProperty().asObject());
        colRetail.setCellValueFactory(data -> data.getValue().retailProperty().asObject());
        colTotalCost.setCellValueFactory(data -> data.getValue().totalCostProperty().asObject());
        colTotalRetail.setCellValueFactory(data -> data.getValue().totalRetailProperty().asObject());
        colQuantity.setCellValueFactory(data -> data.getValue().quantityProperty().asObject());
    }

    private void setupCategoryDropdown() {
        List<String> categories = categoryService.getAllCategories().stream()
                .map(Category::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        setupMultiSelectDropdown(categoryDropdown, categories, categoryCheckBoxes, selectedCategories, "Categories");
    }

    private void setupSubcategoryDropdown() {
        List<String> subcategories = categoryService.getAllCategories().stream()
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
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (fromDate == null || toDate == null) return;
        if (selectedCategories.isEmpty() || selectedSubcategories.isEmpty()) return;

        List<AnalyticsData> data = analyticsService.getAnalyticsData(fromDate, toDate, selectedCategories, selectedSubcategories);
        analyticsTable.setItems(FXCollections.observableArrayList(data));
        analyticsTable.setRowFactory(tv -> new TableRow<AnalyticsData>() {{
            itemProperty().addListener((obs, oldItem, newItem) -> setStyle(newItem != null && "Sub Total".equals(newItem.getLabel()) ? "-fx-background-color: #d4fcd4;" : ""));
        }});

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
                writer.write("\"Item Number\",\"Label\",\"Category\",\"Cost\",\"Quantity\",\"Sales\"\n");
                for (AnalyticsData data : currentData) {
                    writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%.2f\",\"%d\",\"%.2f\"\n",
                            data.getItemNumber(),
                            data.getLabel().replace("\"", "\"\""),
                            (data.getCategory() + " - " + data.getSubcategory()).replace("\"", "\"\""),
                            data.getRetail(),    // Assuming cost is retail in this context
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
                            (data.getCategory() + " - " + data.getSubcategory()).replace("\"", "\"\"")));

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
