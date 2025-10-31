package sv.edu.catolica.factiasafe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import sv.edu.catolica.factiasafe.R;
import sv.edu.catolica.factiasafe.Categoria;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaViewHolder> {

    private final List<Categoria> categoriasList;
    private final OnCategoriaInteractionListener listener;

    // Interfaz para manejar clics en la Activity
    public interface OnCategoriaInteractionListener {
        void onEditClick(int position);
        void onDeleteClick(int position);
    }

    public CategoriaAdapter(List<Categoria> categoriasList, OnCategoriaInteractionListener listener) {
        this.categoriasList = categoriasList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_categoria_editable, parent, false);
        return new CategoriaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position) {
        Categoria categoria = categoriasList.get(position);

        // 1. Mostrar nombre
        holder.textCategoriaNombre.setText(categoria.getNombre());

        // 2. Manejar clic en ELIMINAR (⊖)
        holder.iconDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(holder.getAdapterPosition());
            }
        });

        // 3. Manejar clic en ARRASTRAR (=) - Usado para editar/interactuar
        holder.iconDragHandle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(holder.getAdapterPosition());
            }
        });

        // NOTA: La lógica de arrastrar (ItemTouchHelper) se maneja en la Activity.
    }

    @Override
    public int getItemCount() {
        return categoriasList.size();
    }
}