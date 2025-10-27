package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AjustesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ajustes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void abrirAcercaDe(View view) {
        Intent ventana = new Intent(AjustesActivity.this, AcercaDeActivity.class);
        startActivity(ventana);
    }

    public void VolverFacturas(View view) {
        finish();
    }

    public void abrirPreferenciasPdfExportar(View view) {
        Intent ventana = new Intent(AjustesActivity.this, PreferenciasPdfActivity.class);
        startActivity(ventana);
    }

    public void abrirPrivacidadSeguridad(View view) {
        Intent ventana = new Intent(AjustesActivity.this, PrivacidadSeguridadActivity.class);
        startActivity(ventana);
    }
}