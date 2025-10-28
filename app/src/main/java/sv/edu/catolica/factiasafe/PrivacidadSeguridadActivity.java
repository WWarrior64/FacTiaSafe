package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class PrivacidadSeguridadActivity extends AppCompatActivity {

    private Switch switchCifradoArchivos;
    private Switch switchAnonimizarDatos;
    private MaterialButton btnGuardarConfiguracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacidad_seguridad);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());


        switchCifradoArchivos = findViewById(R.id.switch_cifrado_archivos);
        switchAnonimizarDatos = findViewById(R.id.switch_anonimizar_datos);
        btnGuardarConfiguracion = findViewById(R.id.btn_guardar_configuracion);


        switchCifradoArchivos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mostrarDialogoCifrado();
            }
        });
    }

    private void mostrarDialogoCifrado() {

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_configurar_cifrado, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnCancelar = dialogView.findViewById(R.id.btn_cancelar);
        Button btnActivar = dialogView.findViewById(R.id.btn_activar);
        btnCancelar.setOnClickListener(v -> {
            switchCifradoArchivos.setChecked(false);
            dialog.dismiss();
        });


        btnActivar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}