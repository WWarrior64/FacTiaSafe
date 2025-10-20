package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GarantiasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;
    private Chip selectedChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_garantias);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Configuración de la Toolbar y navegación
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ImageButton searchButton = findViewById(R.id.button_search);
        searchButton.setOnClickListener(v -> handleSearchClick());

        // 2. Configuración de Chips de Filtro
        setupFilterChips();

        // 3. Configuración del RecyclerView
        recyclerView = findViewById(R.id.recycler_garantias);
        setupRecyclerView();

        // 4. Configuración del FAB (asumiendo que en Garantias solo abre una acción principal)
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> handleFabClick());

        // 5. Configuración de la Navegación Inferior
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation();

        // Inicializar el filtro "A/Z" como seleccionado
        Chip chipAz = findViewById(R.id.chip_az);
        // Si el chip Az existe, lo seleccionamos al inicio
        if (chipAz != null) {
            handleChipSelection(chipAz);
        }
    }

    private void setupFilterChips() {
        Chip chipAz = findViewById(R.id.chip_az);
        Chip chipRecientes = findViewById(R.id.chip_recientes);
        Chip chipAntiguos = findViewById(R.id.chip_antiguos);
        Chip chipProximoVencer = findViewById(R.id.chip_proximo_vencer);

        chipAz.setOnClickListener(v -> handleChipClick((Chip) v, "A/Z"));
        chipRecientes.setOnClickListener(v -> handleChipClick((Chip) v, "Recientes"));
        chipAntiguos.setOnClickListener(v -> handleChipClick((Chip) v, "Antiguos"));
        chipProximoVencer.setOnClickListener(v -> handleChipClick((Chip) v, "Próximo a vencer"));
    }

    private void handleChipClick(Chip chip, String filterType) {
        handleChipSelection(chip);
        Toast.makeText(this, "Filtrando garantías por: " + filterType, Toast.LENGTH_SHORT).show();
        // Implementa aquí la lógica de filtrado del RecyclerView
    }

    private void handleChipSelection(Chip newSelectedChip) {
        if (selectedChip != null) {
            // Deseleccionar el chip anterior
            selectedChip.setChipBackgroundColorResource(android.R.color.transparent);
            selectedChip.setChipStrokeColorResource(R.color.gris_inactivo);
            selectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

        newSelectedChip.setChipBackgroundColorResource(R.color.azul_principal);
        newSelectedChip.setChipStrokeWidth(0f);

        newSelectedChip.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        selectedChip = newSelectedChip;
    }

    // ------------------------------------
    // --- Navegación y Acciones ---
    // ------------------------------------

    private void setupRecyclerView() {
        // Lógica para configurar el RecyclerView: LayoutManager, Adapter y carga inicial.
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_facturas) {
                Intent intent = new Intent(this, FacturaActivity.class);
                // >> CLAVE: Reordenar a la parte frontal si ya existe <<
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_garantias) {
                return true; // Ya estamos aquí
            } else if (id == R.id.navigation_ajustes) {
                startActivity(new Intent(this, AjustesActivity.class));
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_garantias);
    }

    private void handleSearchClick() {
        // Lógica para iniciar una nueva actividad de búsqueda o mostrar un campo de búsqueda
        Toast.makeText(this, "Abriendo búsqueda de garantías...", Toast.LENGTH_SHORT).show();
    }

    private void handleFabClick() {
        // Acción para agregar una nueva garantía (podría ser un simple Intent a Entrada Manual)
        Toast.makeText(this, "Abriendo formulario para nueva garantía...", Toast.LENGTH_SHORT).show();
        // Ejemplo: startActivity(new Intent(this, EntradaManualActivity.class));
    }

    // Manejar el botón de retroceso/Up de la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}