package sv.edu.catolica.factiasafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "BOOT_COMPLETED — attempting to recreate and schedule notifications");
            new Thread(() -> {
                try {
                    NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(context);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to reschedule on boot: " + e.getMessage(), e);
                    // Opcional: Envia notif o log a file para debug
                }
            }).start();
        }
    }
}
