package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    // usar Uri contract: lanzaremos con launch(null)
    private ActivityResultLauncher<Uri> dirPickerLauncher;



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

        // Registrar launcher (OpenDocumentTree) para escoger carpeta
        // registrar launcher para OpenDocumentTree
        dirPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        try {
                            final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            // pide permisos persistentes a la URI seleccionada
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception ignored) {}

                        String uriStr = uri.toString();
                        setSetting(KEY_PDF_PATH, uriStr);
                        labelRuta.setText(displayPathFromString(uriStr));
                        Toast.makeText(this, "Ruta seleccionada y guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        // usuario canceló -> fallback a carpeta privada
                        createPrivateAppFolderAndShow();
                    }
                }
        );


        inicializarVistas();
        setupToolbar();
        cargarPreferencias();
        setupListeners();
        ensureDefaultPdfFolderExists();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        switchProductos = findViewById(R.id.switch_incluir_productos);
        switchTiendas = findViewById(R.id.switch_incluir_tiendas);
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
            // Mostrar ruta de forma amigable (intenta mostrar último segmento)
            try {
                labelRuta.setText(displayPathFromString(sRuta));
            } catch (Exception e) {
                labelRuta.setText(sRuta);
            }
        } else {
            // valor por defecto
            labelRuta.setText("/Documents/FactiaSafe/Facturas");
        }
    }

    // -----------------------------------------------------------------
// Constantes
// -----------------------------------------------------------------
    private static final int REQ_WRITE_STORAGE = 42;
    private static final int REQ_PICK_FOLDER = 99; // para SAF (Android 10+)

    // -----------------------------------------------------------------
// Método adaptado: crear carpeta por defecto y mostrarla (SAF para Q+)
// -----------------------------------------------------------------
    private void ensureDefaultPdfFolderExists() {
        // 1) Si ya hay ruta configurada, mostrarla y salir.
        String sRuta = getSetting(KEY_PDF_PATH);
        if (!TextUtils.isEmpty(sRuta)) {
            // sRuta puede ser una ruta absoluta (Android <=28) o una URI (content://...) para SAF
            labelRuta.setText(displayPathFromString(sRuta));
            return;
        }

        // 2) Distinción por versión Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 9 y menores: pedimos WRITE_EXTERNAL_STORAGE si no lo tenemos
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_WRITE_STORAGE
                );
                return; // esperamos respuesta en onRequestPermissionsResult
            }

            // Tenemos permiso -> crear carpeta pública Documents/FactiaSafe/Facturas
            try {
                File publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File targetDir = new File(publicDocs, "FactiaSafe/Facturas");
                if (!targetDir.exists()) {
                    boolean ok = targetDir.mkdirs();
                    // Si falla, seguimos pero mostramos la ruta absoluta de fallback
                    if (!ok) {
                        labelRuta.setText(targetDir.getAbsolutePath());
                        // opcional: guardar también en settings (texto)
                        setSetting(KEY_PDF_PATH, targetDir.getAbsolutePath());
                        return;
                    }
                }
                // Todo ok: mostramos y guardamos la ruta absoluta
                labelRuta.setText(targetDir.getAbsolutePath());
                setSetting(KEY_PDF_PATH, targetDir.getAbsolutePath());
            } catch (Exception e) {
                // Fallback: mostrar ruta por defecto
                labelRuta.setText("/Documents/FactiaSafe/Facturas");
            }
        } else {
            // Android 10+ -> usar SAF (ACTION_OPEN_DOCUMENT_TREE). Pedimos al usuario seleccionar carpeta.
            // Abrimos el selector SOLO si no hay ruta configurada (ya comprobado arriba).
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                // Lanza con el launcher en lugar de startActivityForResult
                dirPickerLauncher.launch(null);
            } catch (Exception e) {
                createPrivateAppFolderAndShow();
            }
        }
    }

    private void createPrivateAppFolderAndShow() {
        try {
            File docs = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File targetDir = new File(docs, "FactiaSafe/Facturas");
            if (!targetDir.exists()) targetDir.mkdirs();
            labelRuta.setText(targetDir.getAbsolutePath());
            setSetting(KEY_PDF_PATH, targetDir.getAbsolutePath());
        } catch (Exception ignored) {
            labelRuta.setText("/Documents/FactiaSafe/Facturas");
        }
    }


    // -----------------------------------------------------------------
// Resultado de permisos (ya lo tienes, lo complementamos para SAF)
// -----------------------------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Reintentar crear la carpeta pública ahora que tenemos permiso
                ensureDefaultPdfFolderExists();
            } else {
                // El usuario negó permiso -> usar carpeta privada
                try {
                    File docs = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    File targetDir = new File(docs, "FactiaSafe/Facturas");
                    if (!targetDir.exists()) targetDir.mkdirs();
                    labelRuta.setText(targetDir.getAbsolutePath());
                    setSetting(KEY_PDF_PATH, targetDir.getAbsolutePath());
                } catch (Exception ignored) {
                    labelRuta.setText("/Documents/FactiaSafe/Facturas");
                }
                Toast.makeText(this, "Permiso denegado: se usará carpeta privada de la app.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // -----------------------------------------------------------------
// Manejar resultado de SAF (cuando el usuario escoge carpeta en Android 10+)
// -----------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FOLDER) {
            if (resultCode == RESULT_OK && data != null) {
                Uri treeUri = data.getData();
                if (treeUri != null) {
                    try {
                        // Pedir permisos persistentes sobre la URI elegida
                        final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                    } catch (Exception ignored) {}

                    // Guardar la URI como string en settings y mostrar friendly label
                    String uriStr = treeUri.toString();
                    setSetting(KEY_PDF_PATH, uriStr);
                    labelRuta.setText(displayPathFromString(uriStr));
                    Toast.makeText(this, "Ruta seleccionada y guardada", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Usuario canceló -> fallback a carpeta privada de la app
                try {
                    File docs = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    File targetDir = new File(docs, "FactiaSafe/Facturas");
                    if (!targetDir.exists()) targetDir.mkdirs();
                    labelRuta.setText(targetDir.getAbsolutePath());
                    setSetting(KEY_PDF_PATH, targetDir.getAbsolutePath());
                } catch (Exception ignored) {
                    labelRuta.setText("/Documents/FactiaSafe/Facturas");
                }
            }
        }
    }


    private void setupListeners() {
        // Guardar al pulsar botón
        btnGuardarConfiguracion.setOnClickListener(v -> guardarPreferencias());

        // Abrir selector de ruta al pulsar en el layout
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
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            // opcional: abrir en Documents (no siempre funciona)
            // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            dirPickerLauncher.launch(null);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el selector de carpetas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            createPrivateAppFolderAndShow();
        }
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

    // ---------- Utilities para mostrar ruta de forma legible ----------
    private String displayPathFromUri(Uri uri) {
        if (uri == null) return "";
        return displayPathFromString(uri.toString());
    }

    private String displayPathFromString(String uriStr) {
        // Intentamos humanizar la URI: si es content://... muestra último path segment o el URI simple.
        try {
            Uri u = Uri.parse(uriStr);
            String last = u.getLastPathSegment();
            if (last != null && !last.isEmpty()) return last;
            return uriStr;
        } catch (Exception e) {
            return uriStr;
        }
    }
}
