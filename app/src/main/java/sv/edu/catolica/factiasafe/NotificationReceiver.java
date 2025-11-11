package sv.edu.catolica.factiasafe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    private static final String CHANNEL_ID = "factiasafe_notifications";
    private static final String CHANNEL_NAME = "FactiaSafe Recordatorios";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String title = intent.getStringExtra(EXTRA_TITLE);
            String text = intent.getStringExtra(EXTRA_TEXT);
            int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, (int)(System.currentTimeMillis() & 0xffff));
            Log.i(TAG, "Received alarm for notifId=" + notifId + ": " + title + " - " + text);


            // Mostrar notificación como antes...
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(
                        NotifConstants.WARRANTY_CHANNEL_ID,
                        NotifConstants.WARRANTY_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                ch.setDescription("Recordatorios y notificaciones de FactiaSafe");
                if (nm != null) nm.createNotificationChannel(ch);
            }

            Bitmap largeIconBitmap = drawableToBitmap(context, R.drawable.ic_icon_factura_large);

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NotifConstants.WARRANTY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.logo_factia_safe)
                    .setLargeIcon(largeIconBitmap)
                    .setContentTitle(title != null ? title : "Recordatorio FactiaSafe")
                    .setContentText(text != null ? text : "")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            if (nm != null) nm.notify(notifId, nb.build());

            // --- MARCAR EN DB como 'sent' para evitar reprograme/duplicados ---
            // --- MARCAR EN DB como 'sent' para evitar reprograme/duplicados ---
            try {
                FaSafeDB dbHelper = new FaSafeDB(context);
                android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

                // Derivamos los IDs desde el notifId (según la lógica de NotificacionRescheduler)
                int d_days = notifId % 100; // Ej: 12305 -> 5
                int w_id = notifId / 100;   // Ej: 12305 -> 123

                ContentValues cv = new ContentValues();
                cv.put("status", "sent");

                // Actualizamos la fila exacta usando source_id (w_id) y
                // un 'd' (days_before) que coincida en el payload.
                String where = "source_table = ? AND source_id = ? AND payload LIKE ? AND status = ?";
                String payloadLike = "%\"days_before\":" + d_days + "}%"; // Busca "days_before":5

                int updatedRows = db.update("notifications", cv, where, new String[]{
                        "warranties",
                        String.valueOf(w_id),
                        payloadLike,
                        "scheduled"
                });

                if (updatedRows > 0) {
                    Log.i(TAG, "Marcada como 'sent' la notifId=" + notifId + " (w:" + w_id + ", d:" + d_days + ")");
                } else {
                    Log.w(TAG, "No se encontró fila para marcar 'sent': notifId=" + notifId);
                }

                try { db.close(); } catch (Exception ignored) {}
                try { dbHelper.close(); } catch (Exception ignored) {}
            } catch (Exception e) {
                Log.w(TAG, "Fallo al marcar notificación como 'sent' en DB: " + e.getMessage());
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error en NotificationReceiver: " + ex.getMessage(), ex);
        }
    }

    // Función para convertir un Drawable de Recurso a Bitmap
    public static Bitmap drawableToBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // Manejar casos donde el drawable no es BitmapDrawable directamente (ej. VectorDrawable)
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        // Fallback si no tiene dimensiones (ej. algunos drawables XML)
        if (width <= 0 || height <= 0) {
            width = 1;
            height = 1;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
