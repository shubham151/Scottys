package models;

public class Category {
    private int id;
    private String category;
    private String subcategory;

    public Category(int id, String category, String subcategory) {
        this.id = id;
        this.category = category;
        this.subcategory = subcategory;
    }

    // Convenience constructor if you don’t have an ID (like when adding new)
    public Category(String category, String subcategory) {
        this(0, category, subcategory);
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }
}
