package models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.IntegerProperty;

public class Store {
    private final IntegerProperty id;
    private final StringProperty storeName;
    private final StringProperty location;
    private boolean isNew;

    public Store(int id, String storeName, String location, boolean isNew) {
        this.id = new SimpleIntegerProperty(id);
        this.storeName = new SimpleStringProperty(storeName);
        this.location = new SimpleStringProperty(location);
        this.isNew = isNew;
    }

    public Store(String storeName, String location) {
        this(0, storeName, location, true);
    }

    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getStoreName() { return storeName.get(); }
    public void setStoreName(String storeName) { this.storeName.set(storeName); }
    public StringProperty storeNameProperty() { return storeName; }

    public String getLocation() { return location.get(); }
    public void setLocation(String location) { this.location.set(location); }
    public StringProperty locationProperty() { return location; }

    public boolean isNew() { return isNew; }
    public void markSaved() { this.isNew = false; }

    @Override
    public String toString() {
        return getStoreName() + " - " + getLocation();
    }
}