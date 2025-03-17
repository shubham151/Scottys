package models;

import javafx.beans.property.*;

public class Product {
    private final IntegerProperty itemNumber;
    private final StringProperty label;
    private final StringProperty productClass;
    private final StringProperty taxRate1;
    private final StringProperty taxRate2;
    private final StringProperty taxRate3;
    private final StringProperty taxRate4;
    private final DoubleProperty price;
    private final StringProperty recStatus;

    public Product(int itemNumber, String label, String productClass, String taxRate1,
                   String taxRate2, String taxRate3, String taxRate4, double price, String recStatus) {
        this.itemNumber = new SimpleIntegerProperty(itemNumber);
        this.label = new SimpleStringProperty(label);
        this.productClass = new SimpleStringProperty(productClass);
        this.taxRate1 = new SimpleStringProperty(taxRate1);
        this.taxRate2 = new SimpleStringProperty(taxRate2);
        this.taxRate3 = new SimpleStringProperty(taxRate3);
        this.taxRate4 = new SimpleStringProperty(taxRate4);
        this.price = new SimpleDoubleProperty(price);
        this.recStatus = new SimpleStringProperty(recStatus);
    }

    // Getters
    public int getItemNumber() { return itemNumber.get(); }
    public String getLabel() { return label.get(); }
    public String getProductClass() { return productClass.get(); }
    public String getTaxRate1() { return taxRate1.get(); }
    public String getTaxRate2() { return taxRate2.get(); }
    public String getTaxRate3() { return taxRate3.get(); }
    public String getTaxRate4() { return taxRate4.get(); }
    public double getPrice() { return price.get(); }
    public String getRecStatus() { return recStatus.get(); }

    // Setters
    public void setItemNumber(int itemNumber) { this.itemNumber.set(itemNumber); }
    public void setLabel(String label) { this.label.set(label); }
    public void setProductClass(String productClass) { this.productClass.set(productClass); }
    public void setTaxRate1(String taxRate1) { this.taxRate1.set(taxRate1); }
    public void setTaxRate2(String taxRate2) { this.taxRate2.set(taxRate2); }
    public void setTaxRate3(String taxRate3) { this.taxRate3.set(taxRate3); }
    public void setTaxRate4(String taxRate4) { this.taxRate4.set(taxRate4); }
    public void setPrice(double price) { this.price.set(price); }
    public void setRecStatus(String recStatus) { this.recStatus.set(recStatus); }

    // Property getters for binding
    public IntegerProperty itemNumberProperty() { return itemNumber; }
    public StringProperty labelProperty() { return label; }
    public StringProperty productClassProperty() { return productClass; }
    public StringProperty taxRate1Property() { return taxRate1; }
    public StringProperty taxRate2Property() { return taxRate2; }
    public StringProperty taxRate3Property() { return taxRate3; }
    public StringProperty taxRate4Property() { return taxRate4; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty recStatusProperty() { return recStatus; }
}
