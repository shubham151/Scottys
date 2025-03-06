package models;

import javafx.beans.property.*;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsData {
    private final IntegerProperty itemNumber;
    private final StringProperty label;
    private final StringProperty category;
    private final DoubleProperty price;
    private final IntegerProperty quantity;
    private final DoubleProperty sales;
    private Map<String, Integer> weeklySales = new HashMap<>();

    public AnalyticsData(int itemNumber, String label, String category, double price, int quantity, double sales) {
        this.itemNumber = new SimpleIntegerProperty(itemNumber);
        this.label = new SimpleStringProperty(label);
        this.category = new SimpleStringProperty(category);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.sales = new SimpleDoubleProperty(sales);
    }

    // Getters
    public int getItemNumber() { return itemNumber.get(); }
    public String getLabel() { return label.get(); }
    public String getCategory() { return category.get(); }
    public double getPrice() { return price.get(); }
    public int getQuantity() { return quantity.get(); }
    public double getSales() { return sales.get(); }

    // JavaFX Properties for TableView binding
    public IntegerProperty itemNumberProperty() { return itemNumber; }
    public StringProperty labelProperty() { return label; }
    public StringProperty categoryProperty() { return category; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty quantityProperty() { return quantity; }
    public DoubleProperty salesProperty() { return sales; }

    public Map<String, Integer> getWeeklySales() {
        return weeklySales;
    }

    public void setWeeklySales(Map<String, Integer> weeklySales) {
        this.weeklySales = weeklySales;
    }

    public void mergeWeeklySales(Map<String, Integer> newWeeklySales) {
        for (Map.Entry<String, Integer> entry : newWeeklySales.entrySet()) {
            this.weeklySales.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }
}
