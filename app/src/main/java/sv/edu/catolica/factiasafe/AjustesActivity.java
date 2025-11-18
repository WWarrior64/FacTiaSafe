package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

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

    public void abrirNotificaciones(View view) {
        Intent ventana = new Intent(AjustesActivity.this, NotificacionesActivity.class);
        startActivity(ventana);
    }

    public void borrarCacheApp(View view) {
        limpiarCacheDeAplicacion();
        Toast.makeText(this, R.string.cache_exito, Toast.LENGTH_SHORT).show();
    }

    private void limpiarCacheDeAplicacion() {
        try {
            // directorio de caché interno
            File cacheDir = getApplicationContext().getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                borrarDirectorio(cacheDir);
            }

            // directorio de caché externo (si existe)
            File externalCacheDir = getApplicationContext().getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                borrarDirectorio(externalCacheDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_cache, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean borrarDirectorio(File dir) {
        if (dir != null && dir.isDirectory()) {

            String[] elementos = dir.list();
            for (String elemento : elementos) {

                boolean exito = borrarDirectorio(new File(dir, elemento));
                if (!exito) {

                    return false;
                }
            }
        }
        return dir.delete();
    }

}