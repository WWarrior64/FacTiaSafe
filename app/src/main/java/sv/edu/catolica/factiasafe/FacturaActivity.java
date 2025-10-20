package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.android.material.navigation.NavigationBarView;


public class FacturaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton mainFab;
    private LinearLayout fabMenuContainer;
    private Chip selectedChip;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> handleSearchClick());

        // 2. Inicialización del FAB y menú
        mainFab = findViewById(R.id.main_fab);
        fabMenuContainer = findViewById(R.id.fab_menu_container);
        fabMenuContainer.setVisibility(View.GONE); // Asegura que esté oculto al inicio

        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMenu();
            }
        });

        // 3. Configuración de Chips de Filtro
        setupFilterChips();

        // 4. Configuración del RecyclerView
        recyclerView = findViewById(R.id.invoices_recycler_view);
        setupRecyclerView();

        // 5. Configuración de la Navegación Inferior
        bottomNavigationView = findViewById(R.id.bottom_nav_view);
        setupBottomNavigation();

        // Inicializar el filtro "A/Z" como seleccionado
        Chip chipAz = findViewById(R.id.chip_az);
        handleChipSelection(chipAz);
    }

    private void toggleFabMenu() {
        if (isMenuOpen) {
            // Si el menú está abierto: CERRAR
            fabMenuContainer.setVisibility(View.GONE);
            // Cambiar el ícono a '+' (ic_add)
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add));
            isMenuOpen = false;
        } else {
            // Si el menú está cerrado: ABRIR
            fabMenuContainer.setVisibility(View.VISIBLE);
            // Cambiar el ícono a 'X' (ic_close)
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close));
            isMenuOpen = true;
        }
    }

    // Métodos onClick definidos en el XML para los botones del menú FAB (ADAPTADO)
    public void EntradaManual(View view) {
        Intent ventana = new Intent(this, EntradaManualActivity.class);
        startActivity(ventana);
        toggleFabMenu(); // Cierra el menú después de la acción
    }

    public void ImportarPDF(View view) {
        Intent ventana = new Intent(FacturaActivity.this, ImportPdfActivity.class);
        startActivity(ventana);
        toggleFabMenu(); // Cierra el menú después de la acción
    }

    public void abrirEscanear(View view) {
        Intent ventana = new Intent(FacturaActivity.this, EscanearActivity.class);
        startActivity(ventana);
        toggleFabMenu(); // Cierra el menú después de la acción
    }

    private void abrirAjustes() {
        Intent intent = new Intent(this, AjustesActivity.class);
        startActivity(intent);
    }

    // ------------------------------------
    // --- Manejo de Chips de Filtro (Nuevo) ---
    // ------------------------------------

    private void setupFilterChips() {
        Chip chipAz = findViewById(R.id.chip_az);
        Chip chipRecientes = findViewById(R.id.chip_recientes);
        Chip chipAntiguos = findViewById(R.id.chip_antiguos);
        Chip chipEsteMes = findViewById(R.id.chip_este_mes);

        chipAz.setOnClickListener(v -> handleChipClick((Chip) v, "A/Z"));
        chipRecientes.setOnClickListener(v -> handleChipClick((Chip) v, "Recientes"));
        chipAntiguos.setOnClickListener(v -> handleChipClick((Chip) v, "Antiguos"));
        chipEsteMes.setOnClickListener(v -> handleChipClick((Chip) v, "Este mes"));
    }

    private void handleChipClick(Chip chip, String filterType) {
        handleChipSelection(chip);
        Toast.makeText(this, "Filtrando por: " + filterType, Toast.LENGTH_SHORT).show();
        // Lógica para actualizar el RecyclerView (ej. adapter.applyFilter(filterType);)
    }

    private void handleChipSelection(Chip newSelectedChip) {
        // Lógica para cambiar la apariencia de los chips (requiere los IDs de color en colors.xml)
        if (selectedChip != null) {
            selectedChip.setChipBackgroundColorResource(android.R.color.transparent);
            // Si no tienes R.color.colorOnSecondaryFixed, usa android.R.color.darker_gray temporalmente
            selectedChip.setChipStrokeColorResource(R.color.gris_inactivo);
            selectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        }

        newSelectedChip.setChipBackgroundColorResource(R.color.azul_principal);
        newSelectedChip.setChipStrokeWidth(0f);
        newSelectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));

        selectedChip = newSelectedChip;
    }

    // ------------------------------------
    // --- Otros métodos ---
    // ------------------------------------

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_facturas) {
                return true; // Ya estamos aquí
            } else if (id == R.id.navigation_garantias) {
                Intent intent = new Intent(this, GarantiasActivity.class);
                // >> CLAVE: Reordenar a la parte frontal si ya existe <<
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_ajustes) {
                abrirAjustes();
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.navigation_facturas);
    }

    private void setupRecyclerView() {
        // Implementa aquí la configuración de tu RecyclerView, LayoutManager y Adapter
    }

    private void handleSearchClick() {
        // Lógica para iniciar la búsqueda
        Toast.makeText(this, "Abriendo búsqueda...", Toast.LENGTH_SHORT).show();
    }
}