package sv.edu.catolica.factiasafe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WarrantyAdapter corregido:
 * - muestra "Inicio: dd/MM/yyyy | Vence: dd/MM/yyyy | Restan: X meses" (si hay datos)
 * - calcula porcentaje correctamente (0..100) y lo clampa
 * - parsea varios formatos de fecha
 */
public class WarrantyAdapter extends RecyclerView.Adapter<WarrantyAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(Warranty warranty);
    }

    private final Context ctx;
    private List<Warranty> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public WarrantyAdapter(Context ctx, List<Warranty> initial, OnItemClickListener listener) {
        this.ctx = ctx;
        this.items = initial != null ? initial : new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<Warranty> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_garantia, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Warranty w = items.get(position);
        holder.title.setText(!TextUtils.isEmpty(w.productName) ? w.productName : (w.companyName != null ? w.companyName : ctx.getString(R.string.garant_a)));

        // Construir la cadena de fecha a mostrar
        StringBuilder fechaSb = new StringBuilder();

        // Preferimos mostrar warrantyStart como "Inicio: dd/MM/yyyy"
        Date startDate = parseDateSafe(w.warrantyStart);
        Date invoiceDate = parseDateSafe(w.invoiceDate);

        if (startDate != null) {
            fechaSb.append(ctx.getString(R.string.inicio)).append(formatDate(startDate));
        } else if (invoiceDate != null) {
            fechaSb.append(ctx.getString(R.string.fecha)).append(formatDate(invoiceDate));
        }

        Date endDate = parseDateSafe(w.warrantyEnd);
        if (endDate != null) {
            if (fechaSb.length() > 0) fechaSb.append(" | ");
            fechaSb.append(ctx.getString(R.string.vence)).append(formatDate(endDate));

            // calcular meses restantes o vencida
            Date now = new Date();
            if (endDate.before(now)) {
                fechaSb.append(ctx.getString(R.string.vencida));
            } else {
                int monthsRem = monthsUntil(now, endDate);
                if (monthsRem <= 0) {
                    // si queda menos de un mes, mostramos días restantes
                    long daysRem = daysBetween(now, endDate);
                    fechaSb.append(ctx.getString(R.string.restan)).append(daysRem).append(ctx.getString(R.string.d_a)).append(daysRem == 1 ? "" : "s");
                } else {
                    fechaSb.append(ctx.getString(R.string.restan)).append(monthsRem).append(ctx.getString(R.string.mes)).append(monthsRem == 1 ? "" : "es");
                }
            }
        }

        holder.fecha.setText(fechaSb.toString());

        // Cargar thumbnail si existe (mantengo decodeFile; cambia a Glide si prefieres)
        if (!TextUtils.isEmpty(w.thumbnailPath)) {
            try {
                File f = new File(w.thumbnailPath);
                if (f.exists()) {
                    Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                    holder.image.setImageBitmap(bm);
                } else {
                    holder.image.setImageResource(R.drawable.logo_factia_safe);
                }
            } catch (Exception e) {
                holder.image.setImageResource(R.drawable.logo_factia_safe);
            }
        } else {
            holder.image.setImageResource(R.drawable.logo_factia_safe);
        }

        int percent = calculateProgressPercent(w);
        holder.progress.setProgress(percent);
        holder.percentText.setText(percent + "%");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(w);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView fecha;
        ProgressBar progress;
        TextView percentText;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_factura_preview);
            title = itemView.findViewById(R.id.text_factura_title);
            fecha = itemView.findViewById(R.id.text_fecha_restante);
            progress = itemView.findViewById(R.id.progress_garantia);
            percentText = itemView.findViewById(R.id.text_progress_percent);
        }
    }

    /**
     * Calcula el porcentaje de vencimiento:
     * - si hay start y end: 0% antes del start, 100% después del end, proporcional entre medias.
     * - si hay warrantyMonths y start: porcentaje según meses pasados / warrantyMonths.
     * - si falta info devuelve 0.
     */
    private int calculateProgressPercent(Warranty w) {
        try {
            Date start = parseDateSafe(w.warrantyStart);
            Date end = parseDateSafe(w.warrantyEnd);
            Date now = new Date();

            if (start != null && end != null) {
                if (now.before(start)) return 0;
                if (now.after(end) || now.equals(end)) return 100;
                long total = end.getTime() - start.getTime();
                long elapsed = now.getTime() - start.getTime();
                if (total <= 0) return 100;
                int percent = (int) Math.max(0, Math.min(100, (elapsed * 100) / total));
                return percent;
            } else if (w.warrantyMonths != null && start != null) {
                long monthsPassed = diffInMonths(start, now);
                long monthsTotal = w.warrantyMonths;
                if (monthsTotal <= 0) return 100;
                int percent = (int) Math.max(0, Math.min(100, (monthsPassed * 100) / monthsTotal));
                return percent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long diffInMonths(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return Math.max(0, days / 30);
    }

    private int monthsUntil(Date from, Date to) {
        if (from == null || to == null) return 0;
        Calendar cFrom = Calendar.getInstance();
        cFrom.setTime(from);
        Calendar cTo = Calendar.getInstance();
        cTo.setTime(to);

        int yearDiff = cTo.get(Calendar.YEAR) - cFrom.get(Calendar.YEAR);
        int monthDiff = cTo.get(Calendar.MONTH) - cFrom.get(Calendar.MONTH);
        int months = yearDiff * 12 + monthDiff;

        if (cTo.get(Calendar.DAY_OF_MONTH) < cFrom.get(Calendar.DAY_OF_MONTH)) months -= 1;
        if (months < 0) months = 0;
        return months;
    }

    private long daysBetween(Date from, Date to) {
        long diff = to.getTime() - from.getTime();
        return Math.max(0, TimeUnit.MILLISECONDS.toDays(diff));
    }

    // ---------- ayuda con parseo/format de fechas (soporta varios formatos comunes) ----------
    private Date parseDateSafe(String src) {
        if (src == null) return null;
        src = src.trim();
        if (src.isEmpty()) return null;

        // Intentamos varios formatos comunes
        String[] patterns = new String[] {
                "yyyy-MM-dd",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy/MM/dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(p, Locale.getDefault());
                fmt.setLenient(false);
                return fmt.parse(src);
            } catch (ParseException ignored) {}
        }

        // Si ninguno funciona, intentar parsear solo la parte fecha si contiene espacio
        if (src.length() >= 10) {
            String possible = src.substring(0, 10);
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                fmt.setLenient(false);
                return fmt.parse(possible);
            } catch (ParseException ignored) {}
            try {
                SimpleDateFormat fmt2 = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                fmt2.setLenient(false);
                return fmt2.parse(possible);
            } catch (ParseException ignored) {}
        }

        return null;
    }

    private String formatDate(Date d) {
        if (d == null) return "";
        SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return out.format(d);
    }
}
