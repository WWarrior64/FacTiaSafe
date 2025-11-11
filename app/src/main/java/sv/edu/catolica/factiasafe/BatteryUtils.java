package sv.edu.catolica.factiasafe;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
// android.widget.Toast; // Toast no es necesario aquí, se usa en la Activity

public class BatteryUtils {

    public static boolean isIgnoringBatteryOptimizations(Context ctx) {
        if (ctx == null) return false;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
        } else {
            return true; // versiones antiguas no tienen Doze
        }
    }

    /**
     * Lanza el Intent para pedir al usuario que ignore optimizaciones (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).
     * Devuelve true si el intent pudo lanzarse.
     */
    public static boolean requestIgnoreBatteryOptimizations(Context ctx) {
        if (ctx == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Abre la pantalla de settings de optimización de batería (general).
     */
    public static boolean openBatteryOptimizationSettings(Context ctx) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Abre la página de Ajustes/Detalles de la Aplicación. 
     * Esta es la mejor manera de guiar al usuario a las opciones de 'Uso de Batería' o 'Fondo' (el FIX).
     */
    public static boolean openAppDetailsSettings(Context ctx) {
        if (ctx == null) return false;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Intent para pantallas OEM de autostart (MIUI, etc). Devuelve true si alguno se lanzó.
     * Mantenemos esto como fallback para dispositivos muy específicos.
     */
    public static boolean tryOpenAutostartSettings(Context ctx) {
        if (ctx == null) return false;

        // Lista de componentes conocidos (pueden variar por versión)
        String[][] candidates = new String[][]{
                // MIUI (Security center)
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                // Huawei
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
                // Samsung (general)
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
        };

        for (String[] comp : candidates) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(comp[0], comp[1]));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                return true;
            } catch (Exception ignored) { }
        }

        // Fallback: abrir la pantalla de detalles de la app (ya cubierto por openAppDetailsSettings)
        return false;
    }

}