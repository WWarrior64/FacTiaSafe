package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GarantiasActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private Chip selectedChip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityLayout(R.layout.activity_garantias);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configuración de la Toolbar (métodos heredados)
        setToolbarTitle("Garantías");
        showUpButton(false);

        ImageButton searchButton = toolbar.findViewById(R.id.search_button);

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> handleSearchClick());
        }

        setupFilterChips();
        recyclerView = findViewById(R.id.recycler_garantias);
        setupRecyclerView();


        Chip chipAz = findViewById(R.id.chip_az);
        if (chipAz != null) handleChipSelection(chipAz);
    }

    // >> CLAVE 1: Implementar el ID de navegación <<
    @Override
    protected int getBottomNavItemId() {
        return R.id.navigation_garantias;
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
        int fallbackStroke = ContextCompat.getColor(this, R.color.gris_inactivo);
        int fallbackOnPrimary = ContextCompat.getColor(this, R.color.white);
        int transparent = Color.TRANSPARENT;

        // Resuelve colores desde el tema usando MaterialColors (usa el chip como view)
        int colorPrimary = MaterialColors.getColor(newSelectedChip, androidx.appcompat.R.attr.colorPrimary);
        // intenta resolver colorOnPrimary, si no existe usa fallback
        int colorOnPrimary;
        try {
            colorOnPrimary = MaterialColors.getColor(newSelectedChip, com.google.android.material.R.attr.colorOnPrimary);
        } catch (Exception e) {
            colorOnPrimary = fallbackOnPrimary;
        }

        int strokeColor;
        try {
            strokeColor = MaterialColors.getColor(newSelectedChip, com.google.android.material.R.attr.colorOnSecondary);
        } catch (Exception e) {
            strokeColor = fallbackStroke;
        }

        // Deseleccionar chip anterior
        if (selectedChip != null) {
            selectedChip.setChipBackgroundColor(ColorStateList.valueOf(transparent));
            selectedChip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
            selectedChip.setTextColor(ColorStateList.valueOf(strokeColor));
        }

        // Seleccionar nuevo chip
        newSelectedChip.setChipBackgroundColor(ColorStateList.valueOf(colorPrimary));
        newSelectedChip.setChipStrokeWidth(0f);
        newSelectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));

        selectedChip = newSelectedChip;
    }

    // ------------------------------------
    // --- Navegación y Acciones ---
    // ------------------------------------

    private void setupRecyclerView() {
        // Lógica para configurar el RecyclerView: LayoutManager, Adapter y carga inicial.
    }

    private void handleSearchClick() {
        // Lógica para iniciar una nueva actividad de búsqueda o mostrar un campo de búsqueda
        Toast.makeText(this, "Abriendo búsqueda de garantías...", Toast.LENGTH_SHORT).show();
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