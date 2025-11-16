package sv.edu.catolica.factiasafe;

public class Categoria {
    private int id;
    private String nombre;
    private String descripcion;

    // Constructor con ID (para BD)
    public Categoria(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Constructor sin ID (para nuevas categorías)
    public Categoria(String nombre) {
        this(-1, nombre, "");
    }

    // Constructor con descripción
    public Categoria(String nombre, String descripcion) {
        this(-1, nombre, descripcion);
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
