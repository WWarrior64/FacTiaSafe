package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

public class EntradaManualActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private MaterialButton buttonCancelar;
    private NestedScrollView nestedScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrada_manual);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        buttonCancelar = findViewById(R.id.button_cancelar);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        setSupportActionBar(toolbar);
        // Habilitar el botón de retroceso (usa el ícono definido en el XML)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupListeners();

        // Inicializar la vista: mostrar Datos Principales (índice 0)
        showTabContent(0);
    }

    private void setupListeners() {
        // Manejar el botón de Cancelar
        buttonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Opción simple: Volver a la actividad anterior (equivalente a onBackPressed)
                onBackPressed();
                // Opción más informativa:
                // Toast.makeText(EntradaManualActivity.this, "Operación cancelada", Toast.LENGTH_SHORT).show();
                // finish();
            }
        });

        // Manejar el cambio de Pestañas (TabLayout)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTabContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No es necesario hacer nada especial al deseleccionar
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Opcional: Desplazarse hacia arriba si se toca la pestaña actual
                if (tab.getPosition() == 0) {
                    nestedScrollView.smoothScrollTo(0, 0);
                }
            }
        });

        // Manejar el click en el campo de Fecha (para abrir un DatePickerDialog)
        // TextInputEditText editFecha = findViewById(R.id.edit_fecha);
        // editFecha.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showTabContent(int position) {
        if (position == 0) {
            // Pestaña "Datos Principales"
            nestedScrollView.setVisibility(View.VISIBLE);
            // if (datosExtrasContainer != null) datosExtrasContainer.setVisibility(View.GONE);
        } else if (position == 1) {
            // Pestaña "Datos Extras"
            nestedScrollView.setVisibility(View.GONE);
            // Cuando crees la vista de Datos Extras:
            // if (datosExtrasContainer != null) datosExtrasContainer.setVisibility(View.VISIBLE);

            // Mensaje temporal, ya que la vista "Datos Extras" aún no existe
            Toast.makeText(this, "Cargando vista de Datos Extras...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Simula el botón de retroceso del sistema
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}