package sv.edu.catolica.factiasafe;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FaSafeDB extends SQLiteOpenHelper {
    private static final String DB_NAME = "factia_safe.sqlite";
    private static final int DB_VERSION = 10; // Incrementar para upgrades
    private final Context context;
    private final String dbPath;
    private boolean isDatabaseCopied = false; // Flag para evitar copias múltiples

    public FaSafeDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        this.dbPath = context.getDatabasePath(DB_NAME).getPath();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Habilita FTS5 explícitamente si es necesario (Android lo hace por default, pero safeguard)
        db.execSQL("PRAGMA compile_options;");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Copia la DB pre-construida ANTES de que OpenHelper cree tablas vacías
        if (!isDatabaseCopied) {
            copyPrebuiltDatabase();
            isDatabaseCopied = true;
        }
        // No ejecutes CREATE TABLE; la DB ya tiene el esquema
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Para upgrades: elimina y recopia (preserva FTS5)
        try {
            db.close();
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            copyPrebuiltDatabase();
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.errorenupgrade) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPrebuiltDatabase() {
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            try (InputStream input = context.getResources().openRawResource(R.raw.factia_safe);
                 OutputStream output = new FileOutputStream(dbFile)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                output.flush();
            } catch (IOException e) {
                Toast.makeText(context, context.getString(R.string.errorcopiarbd) + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    // Usa ESTOS métodos en lugar de openDatabase()
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        if (!isDatabaseCopied) {
            copyPrebuiltDatabase();
            isDatabaseCopied = true;
        }
        return super.getReadableDatabase(); // Esto carga FTS5
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (!isDatabaseCopied) {
            copyPrebuiltDatabase();
            isDatabaseCopied = true;
        }
        return super.getWritableDatabase(); // Esto carga FTS5
    }

    // Elimina openDatabase() o hazlo deprecated
    @Deprecated
    public SQLiteDatabase openDatabase() {
        return getWritableDatabase(); // Redirige a getWritableDatabase para compatibilidad
    }
}