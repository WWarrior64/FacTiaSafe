package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class PreferenciasPdfActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwitchMaterial switchProductos;
    private SwitchMaterial switchTiendas;
    private View layoutCambiarRuta;
    private View layoutGestionarCategorias;
    private MaterialButton btnGuardarConfiguracion;

    // Constante para el manejo de preferencias (usar SharedPreferences)
    private static final String PREFS_NAME = "PdfPreferences";
    private static final String KEY_INCLUIR_PRODUCTOS = "incluir_productos";
    private static final String KEY_INCLUIR_TIENDAS = "incluir_tiendas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preferencias_pdf);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 1. Inicializar vistas
        inicializarVistas();

        // 2. Configurar la Toolbar
        setupToolbar();

        // 3. Cargar las preferencias guardadas
        cargarPreferencias();

        // 4. Configurar listeners de eventos
        setupListeners();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        switchProductos = findViewById(R.id.switch_incluir_productos);
        switchTiendas = findViewById(R.id.switch_incluir_tiendas);
        layoutCambiarRuta = findViewById(R.id.layout_cambiar_ruta);
        layoutGestionarCategorias = findViewById(R.id.layout_gestionar_categorias);
        btnGuardarConfiguracion = findViewById(R.id.btn_guardar_configuracion);
    }

    // --- Configuración de la Toolbar y Navegación ---
    private void setupToolbar() {
        // Establecer la Toolbar como la ActionBar de la actividad
        setSupportActionBar(toolbar);

        // Habilitar el botón de retroceso (si se usa NoActionBar en el tema)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Listener para el botón de retroceso
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // --- Lógica de Preferencias ---
    private void cargarPreferencias() {
        // Nota: En un proyecto real, usarías SharedPreferences o Room para persistencia.
        // Aquí simulamos la carga.

        // Por ahora, solo usamos los valores predeterminados del XML:
        // switchProductos.setChecked(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_INCLUIR_PRODUCTOS, true));
        // switchTiendas.setChecked(getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean(KEY_INCLUIR_TIENDAS, false));
    }

    private void guardarPreferencias() {
        // En un proyecto real, guardarías los valores:
        /*
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INCLUIR_PRODUCTOS, switchProductos.isChecked())
            .putBoolean(KEY_INCLUIR_TIENDAS, switchTiendas.isChecked())
            .apply();
        */
        Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show();
        finish();
    }

    // --- Configuración de Listeners ---
    private void setupListeners() {

        // 1. Listener para el botón principal de guardar
        btnGuardarConfiguracion.setOnClickListener(v -> guardarPreferencias());

        // 2. Listener para la opción de "Cambiar Ruta" (Clic en el ConstraintLayout completo)
        layoutCambiarRuta.setOnClickListener(v -> abrirSelectorRuta());

        // 4. Listeners para los Switch (Opcional, si quieres guardar cambios inmediatamente)
        /*
        switchProductos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lógica para guardar o preparar el cambio
        });

        switchTiendas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lógica para guardar o preparar el cambio
        });
        */
    }

    // --- Métodos de Navegación ---

    private void abrirSelectorRuta() {
        // Aquí iría el Intent para abrir la pantalla/selector que maneja la ruta de guardado
        Toast.makeText(this, "Abriendo selector de ruta...", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, SelectorRutaActivity.class);
        // startActivity(intent);
    }

    public void abrirCategorias(View view) {
        Intent ventana = new Intent(PreferenciasPdfActivity.this, CategoriasActivity.class);
        startActivity(ventana);
    }
}