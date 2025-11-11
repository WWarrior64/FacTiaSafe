package sv.edu.catolica.factiasafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TimeChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "TimeChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // La función recreateAndScheduleAllWarrantyNotifications ya maneja el borrado y la reprogramación completa,
        // y se encarga de cambiar el ID si es necesario por el cambio de zona horaria o tiempo.

        if (Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            Log.i(TAG, "Time or timezone changed — RECREATING all warranty notifications");

            // Ya no necesitamos un Thread si lo hacemos de esta manera,
            // ya que el receptor de cambio de tiempo/zona horaria es de corta duración y
            // la función de recreación debería ser suficientemente rápida.

            // Llama SOLAMENTE a la función que borra y reprocesa correctamente.
            NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(context);
        }

        // Eliminamos el Thread redundante, ya que la lógica solo debe ejecutarse
        // cuando el tiempo/zona horaria realmente cambia (dentro del if).

        // Si realmente quieres asegurar que se ejecute siempre, hazlo sin el chequeo redundante:
        /*
        NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(context);
        */

        // Manteniendo la estructura original (aunque simplificando el body):
        // La verificación 'if' es suficiente, ya que este Receiver solo se activa
        // con TIME_CHANGED o TIMEZONE_CHANGED (según tu manifest).
    }
}