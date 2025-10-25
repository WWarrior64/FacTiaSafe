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


public class FacturaActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private Chip selectedChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityLayout(R.layout.activity_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Configuración de la Toolbar (métodos heredados)
        setToolbarTitle("Facturas");
        showUpButton(false);

        // Inicializa vistas del contenido (que ahora están en activity_factura_content.xml)
        ImageButton searchButton = toolbar.findViewById(R.id.search_button);

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> handleSearchClick());
        }

        // chips y recycler
        setupFilterChips();
        recyclerView = findViewById(R.id.invoices_recycler_view);
        setupRecyclerView();

        // seleccionar A/Z por defecto
        Chip chipAz = findViewById(R.id.chip_az);
        if (chipAz != null) handleChipSelection(chipAz);
    }

    // >> CLAVE: Implementar el ID de navegación <<
    @Override
    protected int getBottomNavItemId() {
        return R.id.navigation_facturas;
    }

    // >> CLAVE: Implementar el comportamiento del FAB heredado <<

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

    private void setupRecyclerView() {
        // Implementa aquí la configuración de tu RecyclerView, LayoutManager y Adapter
    }

    private void handleSearchClick() {
        // Lógica para iniciar la búsqueda
        Toast.makeText(this, "Abriendo búsqueda...", Toast.LENGTH_SHORT).show();
    }

}