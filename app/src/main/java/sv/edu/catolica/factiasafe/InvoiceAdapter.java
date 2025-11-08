package sv.edu.catolica.factiasafe;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder> {
    private List<Invoice> invoiceList;
    private Context context;

    public InvoiceAdapter(Context context, List<Invoice> invoiceList) {
        this.context = context;
        this.invoiceList = invoiceList;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_factura, parent, false);
        return new InvoiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice invoice = invoiceList.get(position);
        holder.title.setText(invoice.getCompanyName());

        // Formatear info: fecha - moneda total
        String formattedDate = formatDate(invoice.getDate());
        String infoText = formattedDate + " - " + invoice.getCurrency() + " " + String.format("%.2f", invoice.getTotal());
        holder.info.setText(infoText);

        String itemsPreview = invoice.getItemsPreview();
        if (itemsPreview != null && !itemsPreview.isEmpty()) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(itemsPreview);
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        String notes = invoice.getNotes();
        if (notes != null && !notes.isEmpty()) {
            holder.notes.setVisibility(View.VISIBLE);
            holder.notes.setText(notes.length() > 100 ? notes.substring(0, 97) + "..." : notes);
        } else {
            holder.notes.setVisibility(View.GONE);
        }

        // Cargar thumbnail si existe
        if (invoice.getThumbnailPath() != null && !invoice.getThumbnailPath().isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(invoice.getThumbnailPath());
            if (bitmap != null) {
                holder.thumbnail.setImageBitmap(bitmap);
            } else {
                holder.thumbnail.setImageResource(android.R.color.darker_gray); // Fallback
            }
        } else {
            holder.thumbnail.setImageResource(android.R.color.darker_gray); // Fallback
        }
    }

    @Override
    public int getItemCount() {
        return invoiceList.size();
    }

    // Método auxiliar para formatear fecha (YYYY-MM-DD a dd/MMM/yyyy)
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateStr);
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MMM/yyyy", new java.util.Locale("es", "SV")); // Español para meses como "oct"
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr; // Fallback si falla el parse
        }
    }

    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView info;
        ImageView thumbnail;
        TextView subtitle;
        TextView notes;

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.invoice_title);
            info = itemView.findViewById(R.id.invoice_info);
            thumbnail = itemView.findViewById(R.id.invoice_thumbnail);
            subtitle = itemView.findViewById(R.id.invoice_subtitle);
            notes = itemView.findViewById(R.id.invoice_notes);

            // Add click listener to the whole item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Invoice invoice = invoiceList.get(position);
                    Context context = itemView.getContext();
                    Intent intent = new Intent(context, DetalleFacturaActivity.class);
                    intent.putExtra("invoice_id", invoice.getId());
                    context.startActivity(intent);
                }
            });
        }
    }
}