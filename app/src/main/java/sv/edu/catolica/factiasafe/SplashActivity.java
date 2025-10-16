package sv.edu.catolica.factiasafe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

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
}