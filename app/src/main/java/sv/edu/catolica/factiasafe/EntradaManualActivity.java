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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class EntradaManualActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private MaterialButton buttonCancelar;
    private ViewPager2 viewPager;
    private final String[] tabTitles = new String[]{"Datos Principales", "Datos Extras"};

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
        viewPager = findViewById(R.id.view_pager); // <- ViewPager2
        buttonCancelar = findViewById(R.id.button_cancelar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        setupViewPager();

        setupListeners();

    }

    private void setupViewPager() {
        EntradaPagerAdapter pagerAdapter = new EntradaPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Sincroniza el TabLayout con el ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
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