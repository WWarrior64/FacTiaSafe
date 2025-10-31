package sv.edu.catolica.factiasafe;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoriasActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerViewCategorias;
    private MaterialButton buttonListo;
    private CategoriaAdapter categoriaAdapter;
    private List<Categoria> categoriasList;
    private View layoutAddCategoria;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categorias);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar_categorias);
        recyclerViewCategorias = findViewById(R.id.recycler_view_categorias);
        buttonListo = findViewById(R.id.button_listo);
        layoutAddCategoria = findViewById(R.id.layout_add_categoria);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        cargarCategoriasDummy();


        setupRecyclerView();


        setupListeners();
    }

    private void cargarCategoriasDummy() {
        categoriasList = new ArrayList<>();
        // Puedes reemplazar los R.drawable con 0 si no tienes los iconos dummy aún
        categoriasList.add(new Categoria("Servicios básicos", 0));
        categoriasList.add(new Categoria("Supermercado", 0));
        categoriasList.add(new Categoria("Transporte", 0));
        categoriasList.add(new Categoria("Salud", 0));
        categoriasList.add(new Categoria("Educación", 0));
        categoriasList.add(new Categoria("Compras personales", 0));
    }

    private void setupRecyclerView() {
        // Inicializar el Adaptador con los datos y el listener de interacción
        categoriaAdapter = new CategoriaAdapter(categoriasList, new CategoriaAdapter.OnCategoriaInteractionListener() {
            @Override
            public void onEditClick(int position) {
                // El clic en el icono de arrastrar (=) activa el diálogo de edición
                mostrarDialogoEdicion(position);
            }

            @Override
            public void onDeleteClick(int position) {
                // El clic en el icono de eliminar (⊖) elimina el elemento
                eliminarCategoria(position);
            }
        });

        recyclerViewCategorias.setAdapter(categoriaAdapter);

        // Configuración para Arrastrar y Soltar (Reordenar)
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewCategorias);
    }

    private void setupListeners() {
        // Listener para el botón LISTO (Guardar y Salir)
        buttonListo.setOnClickListener(v -> {
            // Lógica final para guardar en SQLite
            Toast.makeText(this, "Guardando " + categoriasList.size() + " categorías...", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Listener para "Añadir nueva categoría"
        layoutAddCategoria.setOnClickListener(v -> mostrarDialogoAdicion());
    }

    private void eliminarCategoria(final int position) {
        String nombre = categoriasList.get(position).getNombre();
        categoriasList.remove(position);
        categoriaAdapter.notifyItemRemoved(position);
        Toast.makeText(this, nombre + " eliminada.", Toast.LENGTH_SHORT).show();
        // NOTA: Implementar aquí la lógica para eliminar de SQLite.
    }

    private void mostrarDialogoAdicion() {
        // Crea la vista para el diálogo (un EditText simple)
        final EditText input = new EditText(this);
        input.setHint("Ej: Vestimenta");
        input.setPadding(50, 50, 50, 50); // Añade padding para mejor visualización

        new AlertDialog.Builder(this)
                .setTitle("Añadir Nueva Categoría")
                .setView(input)
                .setPositiveButton("Añadir", (dialog, which) -> {
                    String nuevoNombre = input.getText().toString().trim();
                    if (!nuevoNombre.isEmpty()) {
                        Categoria nuevaCategoria = new Categoria(nuevoNombre, 0); // 0 es el icono placeholder
                        categoriasList.add(nuevaCategoria);
                        categoriaAdapter.notifyItemInserted(categoriasList.size() - 1);
                        Toast.makeText(this, nuevoNombre + " añadida.", Toast.LENGTH_SHORT).show();
                        // NOTA: Implementar aquí la lógica para guardar en SQLite.
                    } else {
                        Toast.makeText(this, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEdicion(final int position) {
        final Categoria categoriaAEditar = categoriasList.get(position);
        final EditText input = new EditText(this);
        input.setText(categoriaAEditar.getNombre());
        input.setPadding(50, 50, 50, 50);

        new AlertDialog.Builder(this)
                .setTitle("Editar Categoría")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = input.getText().toString().trim();
                    if (!nuevoNombre.isEmpty()) {
                        categoriaAEditar.setNombre(nuevoNombre);
                        categoriaAdapter.notifyItemChanged(position);
                        Toast.makeText(this, "Categoría actualizada.", Toast.LENGTH_SHORT).show();
                        // NOTA: Implementar aquí la lógica para actualizar en SQLite.
                    } else {
                        Toast.makeText(this, "El nombre no puede estar vacío.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Configuración para Arrastrar y Soltar (Reordenar)
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(categoriasList, fromPosition, toPosition);
            categoriaAdapter.notifyItemMoved(fromPosition, toPosition);
            // NOTA: Aquí se debería implementar la lógica para actualizar el orden en SQLite/BD.
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // No hacemos nada al deslizar ya que usamos el icono de eliminar explícito
        }

        // Lógica de resaltado (opcional, para feedback visual)
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                // viewHolder.itemView.setBackgroundColor(Color.parseColor("#E0E0E0"));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            // viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    };
}