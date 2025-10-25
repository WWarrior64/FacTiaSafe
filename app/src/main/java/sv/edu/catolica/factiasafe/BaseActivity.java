package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;


import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar;
    protected BottomNavigationView bottomNavigationView;
    protected FloatingActionButton mainFab;
    protected LinearLayout fabMenuContainer; // Contenedor del menú compartido
    protected boolean isFabMenuOpen = false; // Estado del menú
    private FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_base);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        contentFrame = findViewById(R.id.content_frame);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        mainFab = findViewById(R.id.main_fab);
        fabMenuContainer = findViewById(R.id.fab_menu_container);
        setupBottomNavigation();

        if (mainFab != null) {
            mainFab.setOnClickListener(v -> onMainFabClicked());
        }

        // Asegurar que el menú esté cerrado al iniciar
        if (fabMenuContainer != null) {
            fabMenuContainer.setVisibility(View.GONE);
        }
        bottomNavigationView.setSelectedItemId(getBottomNavItemId());
    }

    protected void setActivityLayout(@LayoutRes int layoutResId) {
        // Asegura que el contentFrame NO sea nulo antes de inflar.
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResId, contentFrame, true);
        } else {
            // Esto indica un problema de inicialización en activity_base.xml o en onCreate().
            Log.e("BaseActivity", "contentFrame es nulo. No se puede inflar el layout.");
        }
    }

    protected void onMainFabClicked() {
        toggleFabMenu(); // El comportamiento por defecto del FAB es abrir/cerrar el menú
    }

    // Lógica central para alternar el menú flotante
    protected void toggleFabMenu() {
        if (fabMenuContainer == null || mainFab == null) {
            return;
        }

        if (isFabMenuOpen) {
            // CERRAR
            fabMenuContainer.setVisibility(View.GONE);
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add));
            isFabMenuOpen = false;
        } else {
            // ABRIR
            fabMenuContainer.setVisibility(View.VISIBLE);
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close));
            isFabMenuOpen = true;
        }
    }

    public void EntradaManual(View view) {
        Intent intent = new Intent(this, EntradaManualActivity.class);
        startActivity(intent);
        toggleFabMenu();
    }

    public void ImportarPDF(View view) {
        Intent intent = new Intent(this, ImportPdfActivity.class);
        startActivity(intent);
        toggleFabMenu();
    }

    public void abrirEscanear(View view) {
        Intent intent = new Intent(this, EscanearActivity.class);
        startActivity(intent);
        toggleFabMenu();
    }

    // ----------------
    // Bottom navigation: (con FLAG_ACTIVITY_NO_ANIMATION para evitar parpadeo)
    // ----------------

    @Override
    protected void onPostResume() {
        super.onPostResume();

        // 🔑 CLAVE: Mover la sincronización aquí. Esto garantiza que se ejecute cuando
        // la Activity se crea por primera vez (vía onCreate -> onResume -> onPostResume)
        // Y cuando es traída al frente (vía onNewIntent -> onResume -> onPostResume).
        int selectedId = getBottomNavItemId();
        if (selectedId != 0 && bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(selectedId);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == getBottomNavItemId()) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.navigation_facturas) {
                intent = new Intent(this, FacturaActivity.class);
            } else if (id == R.id.navigation_garantias) {
                intent = new Intent(this, GarantiasActivity.class);
            } else if (id == R.id.navigation_ajustes) {
                intent = new Intent(this, AjustesActivity.class);
            }

            if (intent != null) {
                // Usar solo REORDER_TO_FRONT para Facturas y Garantías.
                if (id == R.id.navigation_facturas || id == R.id.navigation_garantias) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                } else {
                    // Para Ajustes, usa CLEAR_TOP para que no se duplique en el stack.
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                }

                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

    }

    // Cada Activity hija debe devolver el id que corresponde en el bottom nav
    protected abstract int getBottomNavItemId();

    // ... (setToolbarTitle, showUpButton, etc. van aquí) ...
    protected void setToolbarTitle(CharSequence title) {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
    }

    protected void showUpButton(boolean show) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(show);
            getSupportActionBar().setDisplayShowHomeEnabled(show);
        }
    }
}