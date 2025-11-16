package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;

public class DetallesActivity extends AppCompatActivity {
    private TextInputEditText editTienda, editDatosExtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalles);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Encontrar views para llenar con OCR
        editTienda = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_tienda_comercio)).getEditText();
        editDatosExtras = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_datos_extras)).getEditText();

        // Recibir datos de OCR desde Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            HashMap<String, Object> datosExtraidos = (HashMap<String, Object>) extras.getSerializable("datos_extraidos");
            if (datosExtraidos != null) {
                // Llenar tienda/comercio con "empresa"
                if (datosExtraidos.containsKey("empresa")) {
                    editTienda.setText((String) datosExtraidos.get("empresa"));
                }
                // Llenar notas extras con texto legal/residual
                if (datosExtraidos.containsKey("notas_extras")) {
                    editDatosExtras.setText((String) datosExtraidos.get("notas_extras"));
                }
            }
        }
    }

    public void VolverEscanear(View view) { finish(); }
    public void Cancelar(View view) { finish(); }
}