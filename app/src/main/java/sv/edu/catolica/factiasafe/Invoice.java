package sv.edu.catolica.factiasafe;

public class Invoice {
    private int id;
    private String companyName;
    private String ExternalId;
    private String date;
    private double total;
    private String currency;
    private String thumbnailPath;
    private String notes;
    private String itemsPreview;

    // Constructor vacío para facilidad
    public Invoice() {}

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getExternalId() {return ExternalId;}

    public void setExternalId(String externalId) { this.ExternalId = externalId; }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getItemsPreview() {
        return itemsPreview;
    }

    public void setItemsPreview(String itemsPreview) {
        this.itemsPreview = itemsPreview;
    }
}