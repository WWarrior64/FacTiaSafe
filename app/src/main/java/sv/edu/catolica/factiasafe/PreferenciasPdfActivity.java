package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import java.io.File;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class PreferenciasPdfActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwitchMaterial switchProductos;
    private SwitchMaterial switchTiendas;
    private View layoutCambiarRuta;
    private View layoutGestionarCategorias;
    private MaterialButton btnGuardarConfiguracion;
    private TextView labelRuta;

    // Constante para el manejo de preferencias (usar tabla settings)
    private static final String SETTINGS_TABLE = "settings";
    private static final String KEY_INCLUIR_PRODUCTOS = "incluir_productos";
    private static final String KEY_INCLUIR_TIENDAS = "incluir_tiendas";
    private static final String KEY_PDF_PATH = "pdf_save_path";

    private FaSafeDB dbHelper;

    // Launcher para seleccionar carpeta (Storage Access Framework)
    private ActivityResultLauncher<Uri> dirPickerLauncher;

    // URI inicial sugerida para Documents
    private static final Uri INITIAL_DOCUMENTS_URI = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments");


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferencias_pdf);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new FaSafeDB(this);

        // Registrar launcher para OpenDocumentTree
        dirPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        // Tomar permisos persistentes
                        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        try {
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception ignored) {}

                        // Crear subcarpetas FactiaSafe/Facturas
                        DocumentFile rootDir = DocumentFile.fromTreeUri(this, uri);
                        if (rootDir == null) {
                            Toast.makeText(this, "Error al acceder a la carpeta seleccionada", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentFile factiaDir = findOrCreateDir(rootDir, "FactiaSafe");
                        if (factiaDir == null) {
                            Toast.makeText(this, "Error al crear carpeta FactiaSafe", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentFile facturasDir = findOrCreateDir(factiaDir, "Facturas");
                        if (facturasDir == null) {
                            Toast.makeText(this, "Error al crear carpeta Facturas", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uriStr = facturasDir.getUri().toString();
                        setSetting(KEY_PDF_PATH, uriStr);
                        labelRuta.setText(displayPathFromString(uriStr));
                        Toast.makeText(this, "Ruta pública seleccionada y guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        // Usuario canceló
                        Toast.makeText(this, "Selección cancelada: no se cambió la ruta", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        inicializarVistas();
        setupToolbar();
        cargarPreferencias();
        setupListeners();
        // Removed ensureDefaultPdfFolderExists() from here to run on app launch instead
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        switchProductos = findViewById(R.id.switch_incluir_productos);
        layoutCambiarRuta = findViewById(R.id.layout_cambiar_ruta);
        layoutGestionarCategorias = findViewById(R.id.layout_gestionar_categorias);
        btnGuardarConfiguracion = findViewById(R.id.btn_guardar_configuracion);
        labelRuta = findViewById(R.id.label_ruta);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void cargarPreferencias() {
        // Lee las preferencias de la tabla settings (si existen), si no -> valores por defecto desde XML ya aplicados.
        String sProductos = getSetting(KEY_INCLUIR_PRODUCTOS);
        String sTiendas = getSetting(KEY_INCLUIR_TIENDAS);
        String sRuta = getSetting(KEY_PDF_PATH);

        if (sProductos != null) {
            switchProductos.setChecked(Boolean.parseBoolean(sProductos));
        }
        if (sTiendas != null) {
            switchTiendas.setChecked(Boolean.parseBoolean(sTiendas));
        }
        if (sRuta != null && !sRuta.isEmpty()) {
            // Mostrar ruta de forma amigable
            labelRuta.setText(displayPathFromString(sRuta));
        } else {
            // valor por defecto
            labelRuta.setText("/Documents/FactiaSafe/Facturas");
        }
    }

    private void setupListeners() {
        // Guardar al pulsar botón
        btnGuardarConfiguracion.setOnClickListener(v -> guardarPreferencias());

        // Abrir selector de ruta al pulsar en el layout (para cambiar a custom)
        layoutCambiarRuta.setOnClickListener(v -> abrirSelectorRuta());

        // Abrir actividad de categorías (ya la tienes)
        layoutGestionarCategorias.setOnClickListener(v -> abrirCategorias(null));
    }

    private void guardarPreferencias() {
        boolean incluirProductos = switchProductos.isChecked();
        boolean incluirTiendas = switchTiendas.isChecked();

        setSetting(KEY_INCLUIR_PRODUCTOS, String.valueOf(incluirProductos));
        setSetting(KEY_INCLUIR_TIENDAS, String.valueOf(incluirTiendas));

        Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void abrirSelectorRuta() {
        dirPickerLauncher.launch(INITIAL_DOCUMENTS_URI);
    }

    public void abrirCategorias(View view) {
        Intent ventana = new Intent(PreferenciasPdfActivity.this, CategoriasActivity.class);
        startActivity(ventana);
    }

    // ---------- Helpers DB (settings) ----------

    private String getSetting(String key) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            c = db.rawQuery("SELECT value FROM " + SETTINGS_TABLE + " WHERE [key] = ?", new String[]{ key });
            if (c != null && c.moveToFirst()) {
                return c.isNull(0) ? null : c.getString(0);
            }
        } catch (Exception e) {
            // ignore por robustez
        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
            try { if (db != null) db.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    private void setSetting(String key, String value) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("key", key);
            cv.put("value", value);
            // updated_at se actualiza con CURRENT_TIMESTAMP si usas INSERT OR REPLACE + columna default
            // Usamos INSERT OR REPLACE para sobrescribir fácilmente:
            long row = db.insertWithOnConflict(SETTINGS_TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            if (row == -1L) {
                // como fallback, hacer UPDATE
                db.execSQL("UPDATE " + SETTINGS_TABLE + " SET value = ?, updated_at = CURRENT_TIMESTAMP WHERE [key] = ?", new Object[]{ value, key });
            }
        } catch (Exception e) {
            // por robustez no romper
        } finally {
            try { if (db != null) db.close(); } catch (Exception ignored) {}
        }
    }

    // ---------- Helper para crear o encontrar directorio en SAF ----------
    private DocumentFile findOrCreateDir(DocumentFile parent, String name) {
        // Buscar si existe
        for (DocumentFile file : parent.listFiles()) {
            if (file.isDirectory() && name.equals(file.getName())) {
                return file;
            }
        }
        // Crear nuevo
        return parent.createDirectory(name);
    }

    // ---------- Utilities para mostrar ruta de forma legible ----------
    private String displayPathFromString(String pathStr) {
        if (pathStr.startsWith("content://")) {
            // Para URIs de SAF
            try {
                Uri u = Uri.parse(pathStr);
                String segment = u.getLastPathSegment();
                if (segment != null) {
                    int colonIndex = segment.indexOf(':');
                    if (colonIndex != -1) {
                        String volume = segment.substring(0, colonIndex);
                        String docPath = segment.substring(colonIndex + 1);
                        if ("primary".equals(volume)) {
                            return "/" + docPath;
                        } else {
                            return "/" + volume + "/" + docPath;
                        }
                    }
                    return "/" + segment;
                }
            } catch (Exception e) {}
            return pathStr;
        } else {
            // Para rutas relativas o absolutas
            if (pathStr.startsWith("/")) {
                int index = pathStr.indexOf("/Documents/");
                if (index != -1) {
                    return pathStr.substring(index);
                }
                return pathStr;
            } else {
                // Relativa, agregar /
                return "/" + pathStr;
            }
        }
    }
}