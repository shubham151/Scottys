package models;

public class Product {
    private int id;
    private String productClass;
    private int itemNumber;
    private String label;
    private String taxRate1;
    private String taxRate2;
    private String taxRate3;
    private String taxRate4;
    private double price;
    private String recStatus; // Active/Inactive

    public Product(int id, String productClass, int itemNumber, String label,
                   String taxRate1, String taxRate2, String taxRate3, String taxRate4,
                   double price, String recStatus) {
        this.id = id;
        this.productClass = productClass;
        this.itemNumber = itemNumber;
        this.label = label;
        this.taxRate1 = taxRate1;
        this.taxRate2 = taxRate2;
        this.taxRate3 = taxRate3;
        this.taxRate4 = taxRate4;
        this.price = price;
        this.recStatus = recStatus;
    }

    // Getters
    public int getId() { return id; }
    public String getProductClass() { return productClass; }
    public int getItemNumber() { return itemNumber; }
    public String getLabel() { return label; }
    public String getTaxRate1() { return taxRate1; }
    public String getTaxRate2() { return taxRate2; }
    public String getTaxRate3() { return taxRate3; }
    public String getTaxRate4() { return taxRate4; }
    public double getPrice() { return price; }
    public String getRecStatus() { return recStatus; }
}
