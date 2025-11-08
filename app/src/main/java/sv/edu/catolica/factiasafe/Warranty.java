package sv.edu.catolica.factiasafe;

public class Warranty {
    public long id;
    public long invoiceId;
    public String productName;
    public String warrantyStart; // formato yyyy-MM-dd
    public String warrantyEnd;   // formato yyyy-MM-dd
    public Integer warrantyMonths;
    public String thumbnailPath;
    public String companyName;
    public String invoiceDate; // yyyy-MM-dd

    public Warranty() {}
}
