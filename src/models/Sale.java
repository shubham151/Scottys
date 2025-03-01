package models;

public class Sale {
    private int itemNumber;
    private int quantity;
    private String date;
    private double amount;

    public Sale(int itemNumber, int quantity, String date, double amount) {
        this.itemNumber = itemNumber;
        this.quantity = quantity;
        this.date = date;
        this.amount = amount;
    }

    // Getters
    public int getItemNumber() { return itemNumber; }
    public int getQuantity() { return quantity; }
    public String getDate() { return date; }
    public double getAmount() { return amount; }
}
