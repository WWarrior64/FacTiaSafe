package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import org.json.JSONObject;
import org.json.JSONException;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.LinkedHashMap;
import java.util.Map;

public class NotificacionesActivity extends AppCompatActivity {

    private static final String SETTINGS_KEY_LABEL = "notifications_lead_time_label";
    private static final String SETTINGS_KEY_DAYS = "notifications_lead_time_days";

    private Toolbar toolbar;
    private MaterialButton buttonGuardar;
    private AutoCompleteTextView autoCompleteTiempo;

    // Etiquetas y su representación en días (mantenemos orden lógico)
    private LinkedHashMap<String, Integer> opcionesTiempo = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notificaciones);

        // Ajuste de inset (Edge-to-edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        opcionesTiempo.put(getString(R.string.op_1_semana), 7);
        opcionesTiempo.put(getString(R.string.op_6_dias), 6);
        opcionesTiempo.put(getString(R.string.op_5_dias), 5);
        opcionesTiempo.put(getString(R.string.op_4_dias), 4);
        opcionesTiempo.put(getString(R.string.op_3_dias), 3);
        opcionesTiempo.put(getString(R.string.op_2_dias), 2);
        opcionesTiempo.put(getString(R.string.op_1_dia), 1);

        toolbar = findViewById(R.id.toolbar_notificaciones);
        buttonGuardar = findViewById(R.id.button_guardar_configuracion);
        autoCompleteTiempo = findViewById(R.id.auto_complete_tiempo);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Mostrar icono de back en toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupTiempoDropdown();
        loadSavedSetting();
        setupListeners();
    }

    private void setupTiempoDropdown() {
        String[] labels = opcionesTiempo.keySet().toArray(new String[0]);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        autoCompleteTiempo.setAdapter(adapter);

        // Mostrar dropdown en foco/click
        autoCompleteTiempo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) autoCompleteTiempo.showDropDown();
        });
        autoCompleteTiempo.setOnClickListener(v -> autoCompleteTiempo.showDropDown());
    }

    private void setupListeners() {
        // Guardar configuración
        buttonGuardar.setOnClickListener(v -> {
            String label = autoCompleteTiempo.getText() != null ? autoCompleteTiempo.getText().toString().trim() : "";
            if (label.isEmpty() || !opcionesTiempo.containsKey(label)) {
                Toast.makeText(this, R.string.selecc_tiempo_valido, Toast.LENGTH_SHORT).show();
                autoCompleteTiempo.showDropDown();
                return;
            }

            int days = opcionesTiempo.get(label);
            boolean ok = saveLeadTimeSetting(label, days);
            if (ok) {
                Toast.makeText(this, getString(R.string.conf_guardada) + label + ")", Toast.LENGTH_SHORT).show();
                // TODO: aquí podrías reprogramar alarms/recordatorios según 'days'
                // scheduleReminders(days);
                finish();
            } else {
                Toast.makeText(this, R.string.error_guardar_conf, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // NUEVO método
    private void updateAllWarrantiesReminderDays(int newDays) {
        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("reminder_days_before", newDays);
        db.update("warranties", cv, "status = ?", new String[]{"active"});
        db.close();
        dbHelper.close();
    }

    /**
     * Lee la configuración previa desde la tabla settings y la aplica a la UI.
     * Si no hay configuración, deja el valor por defecto "3 días" (si existe en las opciones).
     */
    private void loadSavedSetting() {
        FaSafeDB dbHelper = null;
        SQLiteDatabase db = null;
        Cursor cur = null;
        try {
            dbHelper = new FaSafeDB(this);
            db = dbHelper.getReadableDatabase();

            cur = db.rawQuery("SELECT value FROM settings WHERE [key] = ? LIMIT 1", new String[]{SETTINGS_KEY_LABEL});
            if (cur != null && cur.moveToFirst()) {
                String savedLabel = cur.getString(0);
                if (savedLabel != null && !savedLabel.trim().isEmpty() && opcionesTiempo.containsKey(savedLabel)) {
                    autoCompleteTiempo.setText(savedLabel, false);
                    return;
                }
            }

            // Fallback: si no hay setting, intentar cargar days y mapear a label
            if (cur != null) { cur.close(); cur = null; }
            cur = db.rawQuery("SELECT value FROM settings WHERE [key] = ? LIMIT 1", new String[]{SETTINGS_KEY_DAYS});
            if (cur != null && cur.moveToFirst()) {
                String daysStr = cur.getString(0);
                try {
                    int days = Integer.parseInt(daysStr);
                    String label = findLabelByDays(days);
                    if (label != null) {
                        autoCompleteTiempo.setText(label, false);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Por último, valor por defecto "3 días" si existe
            if (opcionesTiempo.containsKey("3 días")) {
                autoCompleteTiempo.setText("3 días", false);
            } else {
                // si no existe esa etiqueta, tomar la primera de la lista
                String first = opcionesTiempo.keySet().iterator().next();
                autoCompleteTiempo.setText(first, false);
            }

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_leyendo_conf) + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cur != null) try { cur.close(); } catch (Exception ignored) {}
            if (db != null) try { db.close(); } catch (Exception ignored) {}
            if (dbHelper != null) try { dbHelper.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Guarda dos claves en settings (label y days) usando INSERT OR REPLACE semantics.
     */
    private boolean saveLeadTimeSetting(String label, int days) {
        FaSafeDB dbHelper = null;
        SQLiteDatabase db = null;
        try {
            dbHelper = new FaSafeDB(this);
            db = dbHelper.getWritableDatabase();

            // Guardar label
            ContentValues v1 = new ContentValues();
            v1.put("key", SETTINGS_KEY_LABEL);
            v1.put("value", label);
            v1.put("updated_at", System.currentTimeMillis());
            db.insertWithOnConflict("settings", null, v1, SQLiteDatabase.CONFLICT_REPLACE);

            // Guardar days (valor canónico)
            ContentValues v2 = new ContentValues();
            v2.put("key", SETTINGS_KEY_DAYS);
            v2.put("value", Integer.toString(days));
            v2.put("updated_at", System.currentTimeMillis());
            db.insertWithOnConflict("settings", null, v2, SQLiteDatabase.CONFLICT_REPLACE);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (db != null) try { db.close(); } catch (Exception ignored) {}
            if (dbHelper != null) try { dbHelper.close(); } catch (Exception ignored) {}
        }

        // --- Después de guardar la preferencia, actuamos en background ---
        // Opción A (Simple): Thread background (rápido de integrar)
        new Thread(() -> {
            try {
                // 1) Actualizar todas las warranties (opcional) — coméntalo si no quieres esto
                updateAllWarrantiesReminderDays(days);

                // 2) Reconstruir notificaciones y programarlas usando el rescheduler central
                NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(getApplicationContext());
            } catch (Exception ex) {
                android.util.Log.e("NotificacionesActivity", "Error reprogramando notifs después de guardar setting: " + ex.getMessage(), ex);
            }
        }).start();

        return true;
    }

    /**
     * Busca la etiqueta que corresponde a n días (si existe).
     */
    private String findLabelByDays(int days) {
        for (Map.Entry<String, Integer> e : opcionesTiempo.entrySet()) {
            if (e.getValue() == days) return e.getKey();
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Manejar clic en el ícono de retroceso de la Toolbar
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
