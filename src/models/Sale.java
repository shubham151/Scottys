package models;

import java.time.LocalDate;

public class Sale {
    private int itemNumber;
    private int quantity;
    private double price;
    private LocalDate fromDate;
    private LocalDate toDate;

    public Sale(int itemNumber, int quantity, double price, LocalDate fromDate, LocalDate toDate) {
        this.itemNumber = itemNumber;
        this.quantity = quantity;
        this.price = price;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    // Getters
    public int getItemNumber() { return itemNumber; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
}
