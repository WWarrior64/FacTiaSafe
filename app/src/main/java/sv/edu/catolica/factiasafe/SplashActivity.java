package sv.edu.catolica.factiasafe;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        PdfFolderUtils.ensureDefaultPdfFolder(this);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        if (isFirstLaunch) {
            PdfFolderUtils.ensureDefaultPdfFolder(this);
            prefs.edit().putBoolean("is_first_launch", false).apply();
        }

        ImageView imagen = findViewById(R.id.logoapp);
        Animation MiAnimacion = AnimationUtils.loadAnimation(this,R.anim.rotacion);

        imagen.startAnimation(MiAnimacion);
        Handler manejador = new Handler();
        manejador.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent ventana = new Intent( SplashActivity.this, FacturaActivity.class );
                startActivity(ventana);
                finish();
            }
        }, 4000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PdfFolderUtils.REQ_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PdfFolderUtils.ensureDefaultPdfFolder(this);
            } else {
                Toast.makeText(this, "Permiso denegado: no se puede crear carpeta pública sin permiso.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}