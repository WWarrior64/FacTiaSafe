package sv.edu.catolica.factiasafe; // Crea una carpeta 'models' para esto

public class Categoria {
    private String nombre;
    private int iconoResourceId; // Opcional, si quieres iconos diferentes por categoría

    public Categoria(String nombre, int iconoResourceId) {
        this.nombre = nombre;
        this.iconoResourceId = iconoResourceId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getIconoResourceId() {
        return iconoResourceId;
    }

    // (Puedes añadir setters si lo necesitas)
}
