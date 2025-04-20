package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import models.Category;
import models.Product;
import services.CategoryService;
import services.ProductService;
import utils.CSVImporter;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ProductController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private ComboBox<String> subcategoryDropdown;
    @FXML private Button btnImport;
    @FXML private Button btnAddProduct;
    @FXML private Button btnDeleteProduct;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Number> colItemNumber;
    @FXML private TableColumn<Product, String> colLabel;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colSubcategory;
    @FXML private TableColumn<Product, Number> colPrice;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TextField tfItemNumberFilter;
    @FXML private TextField tfLabelFilter;
    @FXML private TextField tfCategoryFilter;
    @FXML private TextField tfSubcategoryFilter;
    @FXML private TextField tfStatusFilter;
    @FXML private ComboBox<String> cbPriceComparator;
    @FXML private TextField tfPriceFilter;

    private final int PAGE_SIZE = 1000;
    private int currentOffset = 0;

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final ObservableList<Product> productsList = FXCollections.observableArrayList();
    private final List<Product> newProducts = new ArrayList<>();
    private final Map<String, List<String>> categorySubcategoryMap = new HashMap<>();
    private final ObservableList<String> selectedCategories = FXCollections.observableArrayList();
    private final ObservableList<String> selectedSubcategories = FXCollections.observableArrayList();
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private final List<CheckBox> subcategoryCheckBoxes = new ArrayList<>();


    @FXML
    public void initialize() {
        productTable.setEditable(true);
        setupMultiSelectDropdown();
        setupTableColumns();
        loadInitialProducts();
        setupFilters();
        setupScrollListener();
    }

    private void setupTableColumns() {
        colItemNumber.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colItemNumber.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter("0")));
        colItemNumber.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setItemNumber(event.getNewValue().intValue());
            attemptSaveProduct(p);
        });

        colLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colLabel.setCellFactory(TextFieldTableCell.forTableColumn());
        colLabel.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setLabel(event.getNewValue());
            attemptSaveProduct(p);
        });

        colCategory.setCellValueFactory(new PropertyValueFactory<>("productClass"));
        List<String> categories = new ArrayList<>(categorySubcategoryMap.keySet());
        Collections.sort(categories);
        colCategory.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(categories)));
        colCategory.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setProductClass(event.getNewValue());
            attemptSaveProduct(p);
        });

        colSubcategory.setCellValueFactory(new PropertyValueFactory<>("subcategory"));
        Set<String> allSubcategories = new HashSet<>();
        categorySubcategoryMap.values().forEach(allSubcategories::addAll);
        List<String> subcategoryList = new ArrayList<>(allSubcategories);
        Collections.sort(subcategoryList);
        colSubcategory.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(subcategoryList)));
        colSubcategory.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setSubcategory(event.getNewValue());
            attemptSaveProduct(p);
        });

        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(TextFieldTableCell.forTableColumn(new NumberStringConverter()));
        colPrice.setOnEditCommit(event -> {
            Product p = event.getRowValue();
            p.setPrice(event.getNewValue().doubleValue());
            attemptSaveProduct(p);
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("recStatus"));
    }

    private void setupMultiSelectDropdown() {
        List<Category> allCategories = categoryService.getAllCategories();
        categorySubcategoryMap.clear();
        for (Category cat : allCategories) {
            categorySubcategoryMap.computeIfAbsent(cat.getCategory(), k -> new ArrayList<>()).add(cat.getSubcategory());
        }

        categoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedCategories.isEmpty() ? "Select Categories" : String.join(", ", selectedCategories));
            }
        });
        subcategoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedSubcategories.isEmpty() ? "Select Subcategories" : String.join(", ", selectedSubcategories));
            }
        });

        // CATEGORY DROPDOWN
        VBox categoryContainer = new VBox();
        TextField categorySearch = new TextField();
        ListView<CheckBox> categoryListView = new ListView<>();
        CheckBox allCategoryCheck = new CheckBox("ALL");
        allCategoryCheck.setOnAction(e -> {
            boolean selected = allCategoryCheck.isSelected();
            categoryCheckBoxes.forEach(cb -> cb.setSelected(selected));
            updateSelectedCategories();
        });
        categoryCheckBoxes.clear();
        categoryListView.getItems().add(allCategoryCheck);
        for (String cat : categorySubcategoryMap.keySet()) {
            CheckBox cb = new CheckBox(cat);
            cb.setOnAction(e -> {
                updateSelectedCategories();
                if (!cb.isSelected()) allCategoryCheck.setSelected(false);
            });
            categoryCheckBoxes.add(cb);
            categoryListView.getItems().add(cb);
        }
        categorySearch.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<CheckBox> filtered = categoryCheckBoxes.stream().filter(cb -> cb.getText().toLowerCase().contains(filter)).collect(Collectors.toList());
            categoryListView.getItems().setAll(filtered);
            categoryListView.getItems().add(0, allCategoryCheck);
        });
        categoryContainer.getChildren().addAll(categorySearch, categoryListView);
        CustomMenuItem catItem = new CustomMenuItem(categoryContainer);
        catItem.setHideOnClick(false);
        ContextMenu catMenu = new ContextMenu(catItem);
        categoryDropdown.setOnMouseClicked(e -> catMenu.show(categoryDropdown, categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getX(), categoryDropdown.localToScreen(0, categoryDropdown.getHeight()).getY()));

        // SUBCATEGORY DROPDOWN
        VBox subcategoryContainer = new VBox();
        TextField subcategorySearch = new TextField();
        ListView<CheckBox> subcategoryListView = new ListView<>();
        CheckBox allSubcategoryCheck = new CheckBox("ALL");
        allSubcategoryCheck.setOnAction(e -> {
            boolean selected = allSubcategoryCheck.isSelected();
            subcategoryCheckBoxes.forEach(cb -> cb.setSelected(selected));
            updateSelectedSubcategories();
        });
        subcategorySearch.setPromptText("Search subcategory...");
        subcategoryContainer.getChildren().addAll(subcategorySearch, subcategoryListView);
        CustomMenuItem subcatItem = new CustomMenuItem(subcategoryContainer);
        subcatItem.setHideOnClick(false);
        ContextMenu subcatMenu = new ContextMenu(subcatItem);
        subcategoryDropdown.setOnMouseClicked(e -> subcatMenu.show(subcategoryDropdown, subcategoryDropdown.localToScreen(0, subcategoryDropdown.getHeight()).getX(), subcategoryDropdown.localToScreen(0, subcategoryDropdown.getHeight()).getY()));

        selectedCategories.addListener((ListChangeListener<String>) change -> {
            selectedSubcategories.clear();
            subcategoryCheckBoxes.clear();
            subcategoryListView.getItems().clear();
            subcategoryListView.getItems().add(allSubcategoryCheck);
            Set<String> allSubcats = new HashSet<>();
            for (String cat : selectedCategories) {
                allSubcats.addAll(categorySubcategoryMap.getOrDefault(cat, Collections.emptyList()));
            }
            for (String subcat : allSubcats) {
                CheckBox cb = new CheckBox(subcat);
                cb.setOnAction(e -> {
                    updateSelectedSubcategories();
                    if (!cb.isSelected()) allSubcategoryCheck.setSelected(false);
                });
                subcategoryCheckBoxes.add(cb);
                subcategoryListView.getItems().add(cb);
            }
        });

        subcategorySearch.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();
            List<CheckBox> filtered = subcategoryCheckBoxes.stream().filter(cb -> cb.getText().toLowerCase().contains(filter)).collect(Collectors.toList());
            subcategoryListView.getItems().setAll(filtered);
            subcategoryListView.getItems().add(0, allSubcategoryCheck);
        });
    }

    private void updateSelectedCategories() {
        selectedCategories.clear();
        for (CheckBox cb : categoryCheckBoxes) {
            if (cb.isSelected()) selectedCategories.add(cb.getText());
        }
        categoryDropdown.setPromptText(selectedCategories.isEmpty() ? "Select Categories" : String.join(", ", selectedCategories));
        categoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedCategories.isEmpty() ? "Select Categories" : String.join(", ", selectedCategories));
            }
        });
        loadInitialProducts();
    }


    private void updateSelectedSubcategories() {
        selectedSubcategories.clear();
        for (CheckBox cb : subcategoryCheckBoxes) {
            if (cb.isSelected()) selectedSubcategories.add(cb.getText());
        }
        subcategoryDropdown.setPromptText(selectedSubcategories.isEmpty() ? "Select Subcategories" : String.join(", ", selectedSubcategories));
        subcategoryDropdown.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(selectedSubcategories.isEmpty() ? "Select Subcategories" : String.join(", ", selectedSubcategories));
            }
        });
        loadInitialProducts();
    }


    private String buildFilterQuery() {
        StringBuilder sb = new StringBuilder();
        if (!searchField.getText().trim().isEmpty()) sb.append(" AND lower(p.label) LIKE ?");
        if (!selectedCategories.isEmpty()) sb.append(" AND lower(c.category) IN (").append("?,".repeat(selectedCategories.size() - 1)).append("?)");
        if (!selectedSubcategories.isEmpty()) sb.append(" AND lower(p.subcategory) IN (").append("?,".repeat(selectedSubcategories.size() - 1)).append("?)");
        return sb.toString();
    }

    private List<Object> buildFilterParameters() {
        List<Object> params = new ArrayList<>();
        if (!searchField.getText().trim().isEmpty()) params.add("%" + searchField.getText().trim().toLowerCase() + "%");
        selectedCategories.forEach(c -> params.add(c.toLowerCase()));
        selectedSubcategories.forEach(sc -> params.add(sc.toLowerCase()));
        return params;
    }


    private void loadInitialProducts() {
        currentOffset = 0;
        productsList.clear();
        productsList.addAll(productService.getProducts(PAGE_SIZE, currentOffset, buildFilterQuery(), buildFilterParameters()));
        productTable.setItems(productsList);
    }

    private void loadMoreProducts() {
        currentOffset += PAGE_SIZE;
        List<Product> more = productService.getProducts(PAGE_SIZE, currentOffset, buildFilterQuery(), buildFilterParameters());
        productsList.addAll(more);
    }

    private void setupFilters() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        categoryDropdown.valueProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfItemNumberFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfLabelFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfCategoryFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfSubcategoryFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfStatusFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        cbPriceComparator.valueProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
        tfPriceFilter.textProperty().addListener((obs, oldVal, newVal) -> loadInitialProducts());
    }

    private void setupScrollListener() {
        productTable.setOnScroll(event -> {
            if (productTable.getItems().size() % PAGE_SIZE == 0 &&
                    productTable.getItems().size() > 0 &&
                    productTable.getFocusModel().getFocusedIndex() >= productTable.getItems().size() - 1) {
                loadMoreProducts();
            }
        });
    }

//    private void attemptSaveProduct(Product p) {
//        if (p.getLabel().isEmpty() || p.getProductClass().isEmpty()) return;
//        if (p.getItemNumber() == 0 || newProducts.contains(p)) {
//            if (productService.insertProduct(p)) {
//                newProducts.remove(p);
//                showAlert("Success", "Product added successfully.");
//            } else {
//                showAlert("Save Error", "Failed to add new product.");
//            }
//        } else {
//            productService.updateProduct(p);
//            showAlert("Success", "Product updated successfully.");
//        }
//        loadInitialProducts();
//    }

    private void attemptSaveProduct(Product p) {
        if (p.getLabel().isEmpty() || p.getProductClass().isEmpty()) return;

        boolean success;
        if (p.getItemNumber() == 0 || newProducts.contains(p)) {
            success = productService.insertProduct(p);
            if (success) {
                newProducts.remove(p);
                showAlert("Success", "Product added successfully.");
            } else {
                showAlert("Save Error", "Failed to add new product.");
            }
        } else {
            success = productService.updateProduct(p);
            if (success) {
                showAlert("Success", "Product updated successfully.");
            } else {
                showAlert("Save Error", "Failed to update product.");
            }
        }

        // ✅ Refresh table only if update was successful
        if (success) {
            productTable.refresh(); // Just redraws the table, no data reload
        }
    }


    @FXML
    private void addProduct() {
        Product newProduct = new Product(0, "", "", "", 0.0, "Active");
        newProducts.add(newProduct);
        productsList.add(0, newProduct);
        productTable.requestFocus();
        productTable.getSelectionModel().clearAndSelect(0);
        productTable.getFocusModel().focus(0);
        productTable.edit(0, colLabel);
    }

    @FXML
    private void deleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null && productService.deleteProduct(selected.getItemNumber())) {
            productsList.remove(selected);
            newProducts.remove(selected);
        } else {
            showAlert("Delete Error", "Could not delete product.");
        }
    }

    @FXML
    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            CSVImporter.importProductsCSV(selectedFile.getAbsolutePath());
            loadInitialProducts();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}