package sv.edu.catolica.factiasafe;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends Activity {

    private static final int REQ_POST_NOTIFICATIONS = 1001;
    private static final int REQ_STORAGE = 1002;
    private static final int REQ_CAMERA = 1003;
    private static final long SPLASH_DELAY_MS = 4000;

    private static final String PREF_ASKED_BATTERY_OPT = "asked_battery_optimization";
    private static final String PREF_ASKED_AUTOSTART = "asked_autostart";
    private static final String PREF_ASKED_STORAGE = "asked_storage";
    private static final String PREF_ASKED_CAMERA = "asked_camera";

    private SharedPreferences prefs;
    private Handler splashHandler = new Handler();

    // Nueva bandera para evitar que continueToAppWithDelay se ejecute más de una vez
    private boolean isAppFlowStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        PdfFolderUtils.ensureDefaultPdfFolder(this);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        if (isFirstLaunch) {
            PdfFolderUtils.ensureDefaultPdfFolder(this);
            prefs.edit().putBoolean("is_first_launch", false).apply();
        }
        ImageView imagen = findViewById(R.id.logoapp);
        Animation MiAnimacion = AnimationUtils.loadAnimation(this,R.anim.rotacion);
        imagen.startAnimation(MiAnimacion);

        // --- Setup de Notificaciones y Workers ---
        createNotificationChannelIfNeeded();
        requestNotificationPermissionIfNeeded();
        NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(getApplicationContext());

        WorkManager wm = WorkManager.getInstance(this);
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.HOURS).build();
        wm.enqueueUniquePeriodicWork("notification_check", ExistingPeriodicWorkPolicy.KEEP, req);

        // --- Manejo de Permisos de Fondo y Delay ---
        boolean askedBatteryOpt = prefs.getBoolean(PREF_ASKED_BATTERY_OPT, false);

        if (!askedBatteryOpt) {
            showBatteryOptimizationDialog();
        } else {
            // 2. Si ya preguntamos por Doze, verificar el permiso de Autoinicio/Fondo
            boolean askedAutostart = prefs.getBoolean(PREF_ASKED_AUTOSTART, false);
            if (!askedAutostart) {
                showAutostartDialog();
            } else {
                // 3. Si ambos fueron preguntados, pedir permisos nativos de Android
                requestPermissionsIfNeeded();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        // ESTE es el punto CLAVE para reanudar el flujo después de regresar de Settings.
        checkAndContinue();
    }

    private void checkAndContinue() {
        // Si el flujo ya comenzó o la actividad está terminando, no hacemos nada.
        if (isAppFlowStarted || isFinishing()) {
            return;
        }

        boolean askedBatteryOpt = prefs.getBoolean(PREF_ASKED_BATTERY_OPT, false);
        boolean askedAutostart = prefs.getBoolean(PREF_ASKED_AUTOSTART, false);

        // Si ya preguntamos por ambos permisos, AHORA podemos continuar.
        if (askedBatteryOpt && askedAutostart) {
            isAppFlowStarted = true; // Marcar para evitar reejecución
            requestPermissionsIfNeeded();
        }
        // Si solo se ha preguntado por uno, el onCreate/dialog ya lo está manejando.
    }

    private void requestPermissionsIfNeeded() {
        // Pedir almacenamiento primero
        boolean askedStorage = prefs.getBoolean(PREF_ASKED_STORAGE, false);
        if (!askedStorage) {
            requestStoragePermission();
            return;
        }

        // Luego pedir cámara
        boolean askedCamera = prefs.getBoolean(PREF_ASKED_CAMERA, false);
        if (!askedCamera) {
            requestCameraPermission();
            return;
        }

        // Si ambos fueron preguntados, continuar
        continueToAppWithDelay();
    }

    private void requestStoragePermission() {
        prefs.edit().putBoolean(PREF_ASKED_STORAGE, true).apply();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: usar READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO
                    },
                    REQ_STORAGE);
        } else {
            // Android 6-12: usar READ_EXTERNAL_STORAGE y WRITE_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_STORAGE);
        }
    }

    private void requestCameraPermission() {
        prefs.edit().putBoolean(PREF_ASKED_CAMERA, true).apply();

        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.CAMERA},
                REQ_CAMERA);
    }

    // --- DIÁLOGO 1: OPTIMIZACIÓN ESTÁNDAR (DOZE) ---
    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_dialogo_optimizacion)
                .setMessage(R.string.mensaje_dialogo_optimizacion)
                .setPositiveButton(R.string.permitir, (dialog, which) -> {
                    // Marcamos solo este permiso como preguntado
                    prefs.edit().putBoolean(PREF_ASKED_BATTERY_OPT, true).apply();
                    BatteryUtils.requestIgnoreBatteryOptimizations(this);
                    dialog.dismiss();

                    // Continuar al siguiente paso después del diálogo
                    showAutostartDialog();
                })
                .setNegativeButton(R.string.nopermitir, (dialog, which) -> {
                    // Si el usuario lo omite, pasamos directamente al siguiente permiso.
                    prefs.edit().putBoolean(PREF_ASKED_BATTERY_OPT, true).apply();
                    dialog.dismiss();
                    showAutostartDialog();
                })
                .setCancelable(false)
                .show();
    }

    private void showAutostartDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_dialogo_optimizacion2)
                .setMessage(R.string.mensaje_dialogo_optimizacion2)
                .setPositiveButton(R.string.abrirajustes, (d, w) -> {
                    // Marcamos solo este permiso como preguntado
                    prefs.edit().putBoolean(PREF_ASKED_AUTOSTART, true).apply();
                    boolean ok = BatteryUtils.tryOpenAutostartSettings(this);

                    if (!ok) {
                        // Fallback: abrir la pantalla de detalles de la App (Tu FIX)
                        BatteryUtils.openAppDetailsSettings(this);
                        Toast.makeText(this, R.string.busca_bateria, Toast.LENGTH_LONG).show();
                    }
                    d.dismiss();

                    // ¡IMPORTANTE! El usuario sale de la app a Settings.
                    // No llamamos a continueToAppWithDelay. Esperamos al próximo onCreate/onStart para continuar.
                })
                .setNegativeButton(R.string.nopermitir, (dialog, which) -> {
                    // Marcar como preguntado para no molestar más
                    prefs.edit().putBoolean(PREF_ASKED_AUTOSTART, true).apply();
                    dialog.dismiss();
                    // Pedir los permisos nativos de Android
                    requestPermissionsIfNeeded();
                })
                .setCancelable(false)
                .show();
    }

    // --- MÉTODO FINAL PARA TERMINAR EL SPLASH (Sin Cambios) ---
    private void continueToAppWithDelay() {
        splashHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Intent ventana = new Intent( SplashActivity.this, FacturaActivity.class );
                    startActivity(ventana);
                    finish();
                }
            }
        }, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar cualquier postDelayed pendiente para evitar fugas de memoria
        splashHandler.removeCallbacksAndMessages(null);
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String id = "warranty_channel";
            String name = "Recordatorios de garantía";
            String desc = "Notificaciones sobre vencimiento de garantías";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel chan = new NotificationChannel(id, name, importance);
            chan.setDescription(desc);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(chan);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                // pedir al usuario autorización: abre la pantalla del sistema para concederle exact alarms a tu app
                Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                startActivity(intent);
            }
        }

    }


    private void requestNotificationPermissionIfNeeded() {
        // Solo en Android 13+ se solicita en tiempo de ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Puedes mostrar una explicación primero si quieres:
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                    // aquí podrías mostrar un diálogo explicando por qué necesitas la notificación
                    // por simplicidad pedimos directamente
                }

                ActivityCompat.requestPermissions(this,
                        new String[]{ android.Manifest.permission.POST_NOTIFICATIONS },
                        REQ_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Manejar permiso de escritura para PdfFolderUtils (tu código original)
        if (requestCode == PdfFolderUtils.REQ_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PdfFolderUtils.ensureDefaultPdfFolder(this);
            } else {
                Toast.makeText(this, R.string.permiso_denegado_carpeta, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Manejar permiso de notificaciones (Android 13+)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido -> opcional: mostrar un Toast o registrar en prefs
                Toast.makeText(this, R.string.permiso_de_notificaciones_concedido, Toast.LENGTH_SHORT).show();
            } else {
                // Denegado -> el usuario siempre puede activarlo manualmente en settings
                Toast.makeText(this, R.string.permiso_notificaciones_noconcedido, Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Manejar permiso de almacenamiento
        if (requestCode == REQ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permiso_de_almacenamiento_concedido, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.permiso_de_almacenamiento_concedido, Toast.LENGTH_SHORT).show();
            }
            // Pedir siguiente permiso
            requestPermissionsIfNeeded();
            return;
        }

        // Manejar permiso de cámara
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permiso_de_c_mara_concedido, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.permiso_de_c_mara_concedido, Toast.LENGTH_SHORT).show();
            }
            // Continuar la app
            continueToAppWithDelay();
            return;
        }
    }

}
