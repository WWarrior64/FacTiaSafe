package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Re-scheduler seguro:
 *  - No usa execSQL(...) para SELECTs.
 *  - Rellena una lista 'toSchedule' dentro de la transacción,
 *    cierra la DB y luego programa alarmas fuera de la transacción.
 */
public class NotificacionRescheduler {
    private static final String TAG = "NotificacionRescheduler";

    private static class ScheduleItem {
        final int warrantyId;
        final int notifId;
        final long whenMillis;
        final String title;
        final String text;
        ScheduleItem(int warrantyId, int notifId, long whenMillis, String title, String text) {
            this.warrantyId = warrantyId;
            this.notifId = notifId;
            this.whenMillis = whenMillis;
            this.title = title;
            this.text = text;
        }
    }

    /**
     * Reconstruye filas en tabla notifications (para warranties) y programa alarmas.
     * Seguro contra locks: escribe en DB en transacción, luego schedules fuera.
     */
    public static void recreateAndScheduleAllWarrantyNotifications(Context ctx) {
        FaSafeDB dbHelper = null;
        SQLiteDatabase db = null;
        List<ScheduleItem> toSchedule = new ArrayList<>();

        int maxRetries = 3;
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                dbHelper = new FaSafeDB(ctx);
                db = dbHelper.getWritableDatabase();

                // poner busy timeout razonable
                try {
                    db.execSQL("PRAGMA busy_timeout = 5000;"); // ok aquí: pragma no-SELECT
                } catch (Exception ignored) {}

                db.beginTransaction();

                // BORRAR notificaciones antiguas de warranties (lo hacemos en transacción)
                db.delete("notifications", "source_table = ?", new String[]{"warranties"});

                // --- NUEVO ---
                // Obtener el 'cero' de hoy (medianoche) para filtrar notificaciones de días ANTERIORES.
                // No omitiremos notificaciones de hoy que ya pasaron (ej. 9am si son las 11am).
                java.util.Calendar calToday = java.util.Calendar.getInstance();
                calToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calToday.set(java.util.Calendar.MINUTE, 0);
                calToday.set(java.util.Calendar.SECOND, 0);
                calToday.set(java.util.Calendar.MILLISECOND, 0);
                long startOfTodayMillis = calToday.getTimeInMillis();

                // LEER warranties activas con rawQuery (NO execSQL)
                Cursor cur = db.rawQuery(
                        "SELECT id, product_name, warranty_end, reminder_days_before FROM warranties WHERE status = 'active'",
                        null
                );

                while (cur.moveToNext()) {
                    int warrantyId = cur.getInt(0);
                    String productName = cur.isNull(1) ? "" : cur.getString(1);
                    String warrantyEnd = cur.isNull(2) ? null : cur.getString(2);
                    int reminderDays = cur.isNull(3) ? 7 : cur.getInt(3);

                    if (warrantyEnd == null || warrantyEnd.trim().isEmpty()) continue;

                    // insertar filas en tabla notifications para cada día pedido (d = reminderDays .. 1)
                    for (int d = reminderDays; d >= 1; d--) {
                        long whenMillis = computeNotifyMillis(warrantyEnd, d);

                        // --- ¡FIX CORREGIDO! ---
                        // Omitir solo si la fecha de notificación es de un DÍA ANTERIOR a hoy.
                        // Si es de hoy (ej. 9AM) pero ya son las 11AM, SÍ queremos programarla
                        // para que se dispare de inmediato.
                        if (whenMillis < startOfTodayMillis) {
                            Log.d(TAG, "Omitiendo notificación de día anterior: warrantyId=" + warrantyId + " days=" + d);
                            continue; // Es de un día anterior, no programar.
                        }
                        // --- FIN FIX CORREGIDO ---

                        String notifyIso = toIsoDateTime(whenMillis);


                        ContentValues ncv = new ContentValues();
                        ncv.put("source_table", "warranties");
                        ncv.put("source_id", warrantyId);
                        ncv.put("channel", "local");
                        ncv.put("notify_at", notifyIso);
                        // payload: JSON simple (evitar comillas no escapadas)
                        String payload = "{\"product_name\":\"" + escapeJson(productName) + "\",\"warranty_id\":" + warrantyId + ",\"days_before\":" + d + "}";
                        ncv.put("payload", payload);
                        ncv.put("status", "scheduled");
                        long row = db.insert("notifications", null, ncv);
                        // preparar item para scheduling (usar esquema notifId = warrantyId*100 + d)
                        int notifId = warrantyId * 100 + d;

                        NotificationScheduler.cancelScheduledNotification(ctx, notifId);
                        String title = ctx.getString(R.string.garantia_proxima);
                        // Cambio: Se usó strings.xml con plurales
                        String name = productName.isEmpty() ?
                                ctx.getString(R.string.unagarantia) :
                                productName;
                        String text = ctx.getResources().getQuantityString(
                                R.plurals.garantiaexpiraen,
                                d,
                                name,
                                d
                        );

                        toSchedule.add(new ScheduleItem(warrantyId, notifId, whenMillis, title, text));
                    }

                    // opcional: día 0 (vence hoy)
                    long whenMillis0 = computeNotifyMillis(warrantyEnd, 0);

                    // --- ¡FIX CORREGIDO! ---
                    // Omitir solo si la fecha de notificación es de un DÍA ANTERIOR a hoy.
                    if (whenMillis0 < startOfTodayMillis) {
                        Log.d(TAG, "Omitiendo notificación de día anterior: warrantyId=" + warrantyId + " days=0");
                    } else {
                        // Programar. Si whenMillis0 es 9AM y ya son 11AM, se disparará de inmediato.
                        String notifyIso0 = toIsoDateTime(whenMillis0);
                        ContentValues ncv0 = new ContentValues();
                        ncv0.put("source_table", "warranties");
                        ncv0.put("source_id", warrantyId);
                        ncv0.put("channel", "local");
                        ncv0.put("notify_at", notifyIso0);
                        String payload0 = "{\"product_name\":\"" + escapeJson(productName) + "\",\"warranty_id\":" + warrantyId + ",\"days_before\":0}";
                        ncv0.put("payload", payload0);
                        ncv0.put("status", "scheduled");
                        long row0 = db.insert("notifications", null, ncv0);
                        int notifId0 = warrantyId * 100 + 0;
                        String title0 = "Garantía vence hoy";
                        String text0 = (productName.isEmpty() ? "Una garantía" : productName) + " vence hoy.";
                        toSchedule.add(new ScheduleItem(warrantyId, notifId0, whenMillis0, title0, text0));
                    }
                    // --- FIN FIX CORREGIDO ---
                }
                cur.close();

                db.setTransactionSuccessful();
                // todo ok -> salir del loop y programar
                break;
            } catch (SQLiteDatabaseLockedException lockEx) {
                Log.w(TAG, "DB locked while recreating notifications, attempt " + attempt + ": " + lockEx.getMessage());
                attempt++;
                try { Thread.sleep(500 * attempt); } catch (InterruptedException ignored) {}
                // retry
            } catch (SQLiteException se) {
                Log.e(TAG, "SQLiteException recreating notifications: " + se.getMessage(), se);
                // no retry for other sqlite exceptions
                break;
            } catch (Exception ex) {
                Log.e(TAG, "Error recreating notifications: " + ex.getMessage(), ex);
                break;
            } finally {
                if (db != null) {
                    try {
                        if (db.inTransaction()) db.endTransaction();
                    } catch (Exception ignored) {}
                    try { db.close(); } catch (Exception ignored) {}
                }
                if (dbHelper != null) {
                    try { dbHelper.close(); } catch (Exception ignored) {}
                }
            }
        } // end retry loop

        // AHORA programar alarmas fuera de la transacción / BD cerrada
        if (!toSchedule.isEmpty()) {
            for (ScheduleItem it : toSchedule) {
                try {
                    NotificationScheduler.scheduleExactNotification(ctx, it.whenMillis, it.notifId, it.title, it.text);
                    Log.i(TAG, "Scheduled notifId=" + it.notifId + " for warranty=" + it.warrantyId + " at " + it.whenMillis);
                } catch (SecurityException se) {
                    Log.w(TAG, "No permission/ability to schedule exact alarm; falling back or skipping notifId=" + it.notifId + ": " + se.getMessage());
                    // si NotificationScheduler implementa fallback (setAndAllowWhileIdle), se intentará ahí
                    try { NotificationScheduler.scheduleExactNotification(ctx, it.whenMillis, it.notifId, it.title, it.text); } catch (Exception ignored) {}
                } catch (Exception e) {
                    Log.e(TAG, "Failed to schedule notifId=" + it.notifId + ": " + e.getMessage(), e);
                }
            }
        }
    }

    // helpers
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int deriveNotifIdFromPayload(String payload, int fallback) {
        // si guardaste notifId en payload puedes extraerlo aquí; por simplicidad:
        return fallback & 0xffff;
    }

    public static long computeNotifyMillis(String warrantyStartDateYYYYMMDD, int reminderDaysBefore) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date start = sdf.parse(warrantyStartDateYYYYMMDD);
            if (start == null) return 0;
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(start);
            cal.add(java.util.Calendar.DATE, -reminderDaysBefore);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 9);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception ex) {
            return 0;
        }
    }

    public static long parseIsoToMillis(String iso) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getDefault());
            java.util.Date d = sdf.parse(iso);
            return d != null ? d.getTime() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Bad ISO: " + iso);
            return 0;
        }
    }

    public static String toIsoDateTime(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new java.util.Date(millis));
    }
}
