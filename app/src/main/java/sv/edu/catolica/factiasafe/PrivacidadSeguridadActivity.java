package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class PrivacidadSeguridadActivity extends AppCompatActivity {

    private Switch switchCifradoArchivos;
    private Switch switchAnonimizarDatos;
    private MaterialButton btnGuardarConfiguracion;

    // Keys para la tabla settings
    private static final String SETTING_CIFRADO_ARCHIVOS = "pdf_cifrado_archivos";
    private static final String SETTING_ANONIMIZAR_DATOS = "pdf_anonimizar_datos";
    private static final String SETTING_CIFRADO_PASSWORD = "pdf_cifrado_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacidad_seguridad);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchCifradoArchivos = findViewById(R.id.switch_cifrado_archivos);
        switchAnonimizarDatos = findViewById(R.id.switch_anonimizar_datos);
        btnGuardarConfiguracion = findViewById(R.id.btn_guardar_configuracion);

        // Cargar configuraciones guardadas
        cargarConfiguraciones();

        switchCifradoArchivos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mostrarDialogoCifrado();
            }
        });

        // Listener para guardar configuraciones
        btnGuardarConfiguracion.setOnClickListener(v -> guardarConfiguraciones());
    }

    /**
     * Carga las configuraciones guardadas en la tabla settings
     */
    private void cargarConfiguraciones() {
        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String cifradoValue = getSetting(db, SETTING_CIFRADO_ARCHIVOS);
            String anonimizarValue = getSetting(db, SETTING_ANONIMIZAR_DATOS);

            switchCifradoArchivos.setChecked(cifradoValue != null && (cifradoValue.equalsIgnoreCase("true") || cifradoValue.equals("1")));
            switchAnonimizarDatos.setChecked(anonimizarValue != null && (anonimizarValue.equalsIgnoreCase("true") || anonimizarValue.equals("1")));
        } finally {
            db.close();
            dbHelper.close();
        }
    }

    /**
     * Guarda las configuraciones en la tabla settings
     */
    private void guardarConfiguraciones() {
        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Guardar cifrado de archivos
            guardarSetting(db, SETTING_CIFRADO_ARCHIVOS, switchCifradoArchivos.isChecked() ? "true" : "false");

            // Guardar anonimizar datos
            guardarSetting(db, SETTING_ANONIMIZAR_DATOS, switchAnonimizarDatos.isChecked() ? "true" : "false");

            Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar la configuración", Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
            dbHelper.close();
        }
    }

    /**
     * Lee un valor de la tabla settings
     */
    private String getSetting(SQLiteDatabase db, String key) {
        if (db == null || key == null) return null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT value FROM settings WHERE [key] = ? LIMIT 1", new String[]{key});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.isNull(0) ? null : cursor.getString(0);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Guarda o actualiza un valor en la tabla settings
     */
    private void guardarSetting(SQLiteDatabase db, String key, String value) {
        if (db == null || key == null) return;

        // Verificar si ya existe
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.rawQuery("SELECT 1 FROM settings WHERE [key] = ? LIMIT 1", new String[]{key});
            exists = cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("key", key);
        contentValues.put("value", value);

        if (exists) {
            // Actualizar
            db.update("settings", contentValues, "[key] = ?", new String[]{key});
        } else {
            // Insertar
            db.insert("settings", null, contentValues);
        }
    }

    private void mostrarDialogoCifrado() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_configurar_cifrado, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnCancelar = dialogView.findViewById(R.id.btn_cancelar);
        Button btnActivar = dialogView.findViewById(R.id.btn_activar);
        com.google.android.material.textfield.TextInputEditText etPassword = dialogView.findViewById(R.id.et_password);
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        btnCancelar.setOnClickListener(v -> {
            switchCifradoArchivos.setChecked(false);
            dialog.dismiss();
        });

        btnActivar.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(PrivacidadSeguridadActivity.this, "Ingresa una contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(PrivacidadSeguridadActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            // Guardar contraseña en settings
            FaSafeDB dbHelper = new FaSafeDB(PrivacidadSeguridadActivity.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            guardarSetting(db, SETTING_CIFRADO_PASSWORD, password);
            db.close();
            dbHelper.close();

            Toast.makeText(PrivacidadSeguridadActivity.this, "Contraseña configurada", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }
}