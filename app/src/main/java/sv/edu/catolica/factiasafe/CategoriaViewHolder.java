package sv.edu.catolica.factiasafe;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import sv.edu.catolica.factiasafe.R;

public class CategoriaViewHolder extends RecyclerView.ViewHolder {

    TextView textCategoriaNombre;
    ImageView iconDelete;
    ImageView iconDragHandle;

    public CategoriaViewHolder(@NonNull View itemView) {
        super(itemView);
        // Asegúrate de que los IDs coincidan con tu item_categoria_editable.xml
        textCategoriaNombre = itemView.findViewById(R.id.text_categoria_nombre);
        iconDelete = itemView.findViewById(R.id.icon_delete);
        iconDragHandle = itemView.findViewById(R.id.icon_drag_handle);
    }
}