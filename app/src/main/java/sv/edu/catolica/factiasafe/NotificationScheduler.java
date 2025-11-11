package sv.edu.catolica.factiasafe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Helper para programar/cancelar alarmas que disparan NotificationReceiver.
 * - Usa setExactAndAllowWhileIdle cuando es posible.
 * - Si la App no puede programar exact alarms en S+, hace fallback a setAndAllowWhileIdle.
 * - Si ocurre SecurityException intenta abrir la pantalla de ajustes para que el usuario conceda
 *   REQUEST_SCHEDULE_EXACT_ALARM (con fallback a literal si la constante no existe).
 */
public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";

    /**
     * Programa una alarma exacta (o fallback) que lanzará NotificationReceiver en whenMillis.
     * No bloquea; lanza Intent de settings para pedir permiso exact alarms si es necesario.
     *
     * @param ctx           Context (puede ser ApplicationContext)
     * @param whenMillis    epoch millis en UTC (RTC_WAKEUP semantics)
     * @param notificationId id numérico único para el PendingIntent/notification
     * @param title         título para la notification (se pasa por extras)
     * @param text          texto para la notification (se pasa por extras)
     */
    public static void scheduleExactNotification(Context ctx, long whenMillis, int notificationId, String title, String text) {
        if (ctx == null) return;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            Log.w(TAG, "AlarmManager null");
            return;
        }

        boolean canExact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                canExact = am.canScheduleExactAlarms();
            } catch (Exception ex) {
                // Por si alguna plataforma arroja excepción; asumimos true y aplicamos fallback si no funciona.
                canExact = true;
            }
            if (!canExact) {
                Log.w(TAG, "App cannot schedule exact alarms (canScheduleExactAlarms=false). Will fallback to approximate alarm.");
            }
        }

        PendingIntent pi = getPendingIntent(ctx, notificationId, title, text, true);

        try {
            Log.i(TAG, "Scheduling notifId=" + notificationId + " at " + whenMillis + " -> " + new java.util.Date(whenMillis));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canExact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                } else {
                    // fallback: approximate but still wake device when possible
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // setExact exists since KITKAT
                am.setExact(AlarmManager.RTC_WAKEUP, whenMillis, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, whenMillis, pi);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling alarm: " + se.getMessage(), se);
            // En S+ puede faltar permiso SCHEDULE_EXACT_ALARM -> abrir pantalla del sistema para pedirlo.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    Intent intent;
                    try {
                        // Intent oficial cuando la SDK lo incluye
                        intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    } catch (Throwable t) {
                        // Fallback a String literal si la constante no existe en la compilación
                        intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                } catch (Exception ex) {
                    Log.w(TAG, "Failed to launch exact alarm request intent: " + ex.getMessage(), ex);
                }
            }

            // Intentamos fallback aproximado si es posible
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, whenMillis, pi);
                }
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            Log.e(TAG, "General error scheduling alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela la alarma/programación para el notificationId dado (si existe).
     */
    public static void cancelScheduledNotification(Context ctx, int notificationId) {
        if (ctx == null) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = getPendingIntent(ctx, notificationId, null, null, false);
        if (pi == null) {
            Log.i(TAG, "No existing PendingIntent to cancel for notifId=" + notificationId);
            return;
        }

        try {
            am.cancel(pi);
            // cancelar también localmente
            try { pi.cancel(); } catch (Exception ignored) {}
            Log.i(TAG, "Cancelled alarm for notifId=" + notificationId);
        } catch (Exception e) {
            Log.w(TAG, "Error cancelling alarm for notifId=" + notificationId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un PendingIntent para NotificationReceiver con los extras adecuados.
     *
     * @param ctx            context
     * @param notificationId id de la notificación (requestCode)
     * @param title          titulo extra (nullable)
     * @param text           text extra (nullable)
     * @param createIfMissing si false, devuelve null si no existía (FLAG_NO_CREATE)
     * @return PendingIntent o null según createIfMissing
     */
    private static PendingIntent getPendingIntent(Context ctx, int notificationId, String title, String text, boolean createIfMissing) {
        Intent i = new Intent(ctx, NotificationReceiver.class);
        if (title != null) i.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        if (text != null) i.putExtra(NotificationReceiver.EXTRA_TEXT, text);
        i.putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        int flags;
        if (createIfMissing) {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            // don't create a new one if it doesn't exist
            flags = PendingIntent.FLAG_NO_CREATE;
        }

        // Añadir FLAG_IMMUTABLE en API >= S (31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        try {
            return PendingIntent.getBroadcast(ctx, notificationId, i, flags);
        } catch (Exception ex) {
            Log.w(TAG, "getPendingIntent failed: " + ex.getMessage(), ex);
            return null;
        }
    }
}
