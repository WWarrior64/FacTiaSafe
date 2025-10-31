package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.MenuItem;
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

public class NotificacionesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private MaterialButton buttonGuardar;
    private AutoCompleteTextView autoCompleteTiempo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notificaciones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar_notificaciones);
        buttonGuardar = findViewById(R.id.button_guardar_configuracion);
        autoCompleteTiempo = findViewById(R.id.auto_complete_tiempo);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupListeners();
        setupTiempoDropdown();
    }

    private void setupTiempoDropdown() {
        // Opciones basadas en tu diseño
        String[] tiempos = new String[] {
                "1 semana",
                "6 días",
                "3 días",
                "2 días",
                "1 día"
        };

        // Crear y configurar el ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // Usar 'this' porque estamos en la Activity
                android.R.layout.simple_dropdown_item_1line,
                tiempos
        );

        if (autoCompleteTiempo != null) {
            autoCompleteTiempo.setAdapter(adapter);

            // Mostrar el dropdown al hacer clic/enfocar
            autoCompleteTiempo.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    autoCompleteTiempo.showDropDown();
                }
            });
        }
    }

    private void setupListeners() {
        // Manejar el botón de Guardar
        buttonGuardar.setOnClickListener(v -> {
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
            // Implementa aquí la lógica para guardar los valores
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Manejar el clic en el ícono de retroceso de la Toolbar
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}