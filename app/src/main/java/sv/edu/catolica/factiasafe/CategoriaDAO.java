package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {
    private static final String TAG = "CategoriaDAO";
    private FaSafeDB dbHelper;
    private Context context;

    public CategoriaDAO(Context context) {
        this.context = context;
        this.dbHelper = new FaSafeDB(context);
    }

    /**
     * Obtiene todas las categorías de la base de datos
     * @return Lista de categorías
     */
    public List<Categoria> getAllCategorias() {
        List<Categoria> categorias = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "categories",
                new String[]{"id", "name", "description"},
                null, null, null, null,
                "name ASC")) {

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String description = cursor.getString(2);

                Categoria categoria = new Categoria(id, name, description);
                categorias.add(categoria);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return categorias;
    }

    /**
     * Obtiene una categoría por ID
     * @param id ID de la categoría
     * @return Categoría encontrada o null
     */
    public Categoria getCategoriaById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "categories",
                new String[]{"id", "name", "description"},
                "id = ?",
                new String[]{String.valueOf(id)},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                String name = cursor.getString(1);
                String description = cursor.getString(2);
                return new Categoria(id, name, description);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return null;
    }

    /**
     * Inserta una nueva categoría
     * @param nombre Nombre de la categoría
     * @param description Descripción de la categoría
     * @return ID de la categoría insertada, o -1 si falla
     */
    public long insertCategoria(String nombre, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put("name", nombre);
            if (description != null && !description.isEmpty()) {
                values.put("description", description);
            }

            id = db.insert("categories", null, values);

        } catch (Exception e) {
            Log.e(TAG,e.getMessage(), e);
        }

        return id;
    }

    /**
     * Actualiza una categoría existente
     * @param id ID de la categoría
     * @param nombre Nuevo nombre
     * @param description Nueva descripción
     * @return true si se actualizó correctamente, false si falla
     */
    public boolean updateCategoria(int id, String nombre, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("name", nombre);
            if (description != null && !description.isEmpty()) {
                values.put("description", description);
            }

            int rowsAffected = db.update("categories", values, "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return false;
    }

    /**
     * Elimina una categoría
     * @param id ID de la categoría
     * @return true si se eliminó correctamente, false si falla
     */
    public boolean deleteCategoria(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int rowsAffected = db.delete("categories", "id = ?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG,  e.getMessage(), e);
        }

        return false;
    }

    /**
     * Verifica si una categoría con ese nombre ya existe
     * @param nombre Nombre de la categoría
     * @return true si existe, false si no
     */
    public boolean existsByName(String nombre) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "categories",
                new String[]{"id"},
                "name = ?",
                new String[]{nombre},
                null, null, null)) {

            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return false;
    }

    /**
     * Verifica si una categoría con ese nombre ya existe (excepto la especificada)
     * @param nombre Nombre de la categoría
     * @param excludeId ID a excluir de la búsqueda
     * @return true si existe, false si no
     */
    public boolean existsByNameExcluding(String nombre, int excludeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                "categories",
                new String[]{"id"},
                "name = ? AND id != ?",
                new String[]{nombre, String.valueOf(excludeId)},
                null, null, null)) {

            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return false;
    }
}
