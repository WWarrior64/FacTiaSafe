package sv.edu.catolica.factiasafe;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // Ejemplo de lo que debería hacer tu Worker:
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        // Vuelve a programar TODAS las notificaciones (usando el código corregido)
        NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(context);
        return Result.success();
    }
}