package sv.edu.catolica.factiasafe;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;

import java.io.File;
import java.io.OutputStream;

public class PdfFolderUtils {

    private static final String SETTINGS_TABLE = "settings";
    private static final String KEY_PDF_PATH = "pdf_save_path";
    public static final int REQ_WRITE_STORAGE = 42;

    public static void ensureDefaultPdfFolder(Activity activity) {
        FaSafeDB dbHelper = new FaSafeDB(activity);
        String sRuta = getSetting(dbHelper, KEY_PDF_PATH);
        if (!TextUtils.isEmpty(sRuta)) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_WRITE_STORAGE);
                return;
            }

            try {
                File publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File targetDir = new File(publicDocs, "FactiaSafe/Facturas");
                if (!targetDir.exists()) {
                    boolean ok = targetDir.mkdirs();
                    if (!ok) {
                        Toast.makeText(activity, "Error al crear carpeta pública", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String pathStr = targetDir.getAbsolutePath();
                setSetting(dbHelper, KEY_PDF_PATH, pathStr);
                Toast.makeText(activity, "Carpeta pública creada automáticamente", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Error al crear carpeta pública: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            String relativePath = Environment.DIRECTORY_DOCUMENTS + "/FactiaSafe/Facturas";
            try {
                ContentResolver resolver = activity.getContentResolver();
                Uri collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY);

                ContentValues values = new ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia");
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");

                Uri dummyUri = resolver.insert(collection, values);
                if (dummyUri != null) {
                    try (OutputStream os = resolver.openOutputStream(dummyUri)) {
                        // Escribir vacío
                    } catch (Exception e) {
                        Toast.makeText(activity, "Error al escribir archivo dummy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    // Opcional: resolver.delete(dummyUri, null, null);
                } else {
                    Toast.makeText(activity, "Error al insertar en MediaStore", Toast.LENGTH_SHORT).show();
                    return;
                }

                setSetting(dbHelper, KEY_PDF_PATH, relativePath);
                Toast.makeText(activity, "Carpeta pública creada automáticamente", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Error al crear carpeta con MediaStore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static String getSetting(FaSafeDB dbHelper, String key) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            c = db.rawQuery("SELECT value FROM " + SETTINGS_TABLE + " WHERE [key] = ?", new String[]{ key });
            if (c != null && c.moveToFirst()) {
                return c.isNull(0) ? null : c.getString(0);
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }

    private static void setSetting(FaSafeDB dbHelper, String key, String value) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("key", key);
            cv.put("value", value);
            long row = db.insertWithOnConflict(SETTINGS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            if (row == -1L) {
                db.execSQL("UPDATE " + SETTINGS_TABLE + " SET value = ?, updated_at = CURRENT_TIMESTAMP WHERE [key] = ?", new Object[]{ value, key });
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (db != null) db.close();
        }
    }
}