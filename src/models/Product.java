package models;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty itemNumber;
    private final StringProperty label;
    private final StringProperty productClass; // Main category
    private final StringProperty subcategory;    // Subcategory
    private final DoubleProperty price;
    private final StringProperty recStatus;

    public Product(int itemNumber, String label, String productClass, String subcategory, double price, String recStatus) {
        this.itemNumber = new SimpleIntegerProperty(itemNumber);
        this.label = new SimpleStringProperty(label);
        this.productClass = new SimpleStringProperty(productClass);
        this.subcategory = new SimpleStringProperty(subcategory);
        this.price = new SimpleDoubleProperty(price);
        this.recStatus = new SimpleStringProperty(recStatus);
    }

    // Getters
    public int getItemNumber() { return itemNumber.get(); }
    public String getLabel() { return label.get(); }
    public String getProductClass() { return productClass.get(); }
    public String getSubcategory() { return subcategory.get(); }
    public double getPrice() { return price.get(); }
    public String getRecStatus() { return recStatus.get(); }

    // Setters
    public void setItemNumber(int itemNumber) { this.itemNumber.set(itemNumber); }
    public void setLabel(String label) { this.label.set(label); }
    public void setProductClass(String productClass) { this.productClass.set(productClass); }
    public void setSubcategory(String subcategory) { this.subcategory.set(subcategory); }
    public void setPrice(double price) { this.price.set(price); }
    public void setRecStatus(String recStatus) { this.recStatus.set(recStatus); }

    // Property getters for binding
    public IntegerProperty itemNumberProperty() { return itemNumber; }
    public StringProperty labelProperty() { return label; }
    public StringProperty productClassProperty() { return productClass; }
    public StringProperty subcategoryProperty() { return subcategory; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty recStatusProperty() { return recStatus; }
}
