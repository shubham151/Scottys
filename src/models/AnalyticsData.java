package models;

import javafx.beans.property.*;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsData {
    private final IntegerProperty itemNumber;
    private final StringProperty label;
    private final StringProperty category;
    private final StringProperty subcategory; // New property
    private final DoubleProperty cost;
    private final DoubleProperty retail;
    private final DoubleProperty total_cost;
    private final DoubleProperty total_retail;
    private final IntegerProperty quantity;
    private Map<String, Integer> weeklySales = new HashMap<>();

    public AnalyticsData(int itemNumber, String label, String category, String subcategory,
                         double cost, double retail, double total_cost, double total_retail, int quantity) {
        this.itemNumber = new SimpleIntegerProperty(itemNumber);
        this.label = new SimpleStringProperty(label);
        this.category = new SimpleStringProperty(category);
        this.subcategory = new SimpleStringProperty(subcategory);
        this.cost = new SimpleDoubleProperty(cost);
        this.retail = new SimpleDoubleProperty(retail);
        this.total_cost = new SimpleDoubleProperty(total_cost);
        this.total_retail = new SimpleDoubleProperty(total_retail);
        this.quantity = new SimpleIntegerProperty(quantity);
    }
    // Overloaded constructor if subcategory is not provided.
    public AnalyticsData(int itemNumber, String label, String category,
                         double cost, double retail, double total_cost, double total_retail, int quantity) {
        this(itemNumber, label, category, "", cost, retail, total_cost, total_retail, quantity);
    }
    // Getters
    public int getItemNumber() { return itemNumber.get(); }
    public String getLabel() { return label.get(); }
    public String getCategory() { return category.get(); }
    public String getSubcategory() { return subcategory.get(); }
    public double getCost() { return cost.get(); }
    public double getRetail() { return retail.get(); }
    public double getTotalCost() { return total_cost.get(); }
    public double getTotalRetail() { return total_retail.get(); }
    public int getQuantity() { return quantity.get(); }
    // Property getters
    public IntegerProperty itemNumberProperty() { return itemNumber; }
    public StringProperty labelProperty() { return label; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty subcategoryProperty() { return subcategory; }
    public DoubleProperty costProperty() { return cost; }
    public DoubleProperty retailProperty() { return retail; }
    public DoubleProperty totalCostProperty() { return total_cost; }
    public DoubleProperty totalRetailProperty() { return total_retail; }
    public IntegerProperty quantityProperty() { return quantity; }
    public Map<String, Integer> getWeeklySales() { return weeklySales; }
    public void setWeeklySales(Map<String, Integer> weeklySales) { this.weeklySales = weeklySales; }
    public void mergeWeeklySales(Map<String, Integer> newWeeklySales) {
        for (Map.Entry<String, Integer> entry : newWeeklySales.entrySet()) {
            this.weeklySales.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
}
