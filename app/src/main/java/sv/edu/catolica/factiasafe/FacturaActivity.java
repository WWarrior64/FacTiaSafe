package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;


public class FacturaActivity extends AppCompatActivity {

    private FloatingActionButton mainFab;
    private LinearLayout fabMenuContainer;
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

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_nav_view);
        bottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_facturas) {
                    return true;
                } else if (itemId == R.id.navigation_garantias) {
                    return true;
                } else if (itemId == R.id.navigation_ajustes) {
                    abrirAjustes();
                    return true;
                }

                return false;
            }
        }

        );

        mainFab = findViewById(R.id.main_fab);
        fabMenuContainer = findViewById(R.id.fab_menu_container);
        fabMenuContainer.setVisibility(View.GONE);

        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFabMenu();
            }
        });
    }

    private void toggleFabMenu() {
        if (isMenuOpen) {
            // Si el menú está abierto: OCULTAR

            // Oculta el contenedor del menú
            fabMenuContainer.setVisibility(View.GONE);

            // Cambiar el ícono a '+' (ic_add)
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add));

            isMenuOpen = false;
        } else {
            // Si el menú está cerrado: MOSTRAR

            // Muestra el contenedor del menú
            fabMenuContainer.setVisibility(View.VISIBLE);

            // Cambiar el ícono a 'X' (ic_close)
            mainFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close));

            isMenuOpen = true;
        }
    }

    public void ImportarPDF(View view) {
        Intent ventana = new Intent(FacturaActivity.this, ImportPdfActivity.class);
        startActivity(ventana);
    }

    public void abrirEscanear(View view) {
        Intent ventana = new Intent(FacturaActivity.this, EscanearActivity.class);
        startActivity(ventana);
    }

    private void abrirAjustes() {
        Intent intent = new Intent(this, AjustesActivity.class);
        startActivity(intent);
    }


}