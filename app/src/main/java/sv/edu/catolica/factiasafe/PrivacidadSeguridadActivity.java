package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class PrivacidadSeguridadActivity extends AppCompatActivity {

    private Switch switchCifradoArchivos;
    private Switch switchAnonimizarDatos;
    private MaterialButton btnGuardarConfiguracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacidad_seguridad);

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