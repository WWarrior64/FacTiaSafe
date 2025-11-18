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
    private CategoriaDAO categoriaDAO;

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

        categoriaDAO = new CategoriaDAO(this);
        cargarCategoriasDesdeDB();
        setupRecyclerView();
        setupListeners();
    }

    /**
     * Carga las categorías desde la base de datos
     */
    private void cargarCategoriasDesdeDB() {
        categoriasList = new ArrayList<>(categoriaDAO.getAllCategorias());
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
        // Listener para el botón LISTO (Cerrar Activity)
        buttonListo.setOnClickListener(v -> finish());

        // Listener para "Añadir nueva categoría"
        layoutAddCategoria.setOnClickListener(v -> mostrarDialogoAdicion());
    }

    /**
     * Elimina una categoría de la BD y la lista
     * @param position Posición en la lista
     */
    private void eliminarCategoria(final int position) {
        Categoria categoria = categoriasList.get(position);
        String nombre = categoria.getNombre();

        // Eliminar de BD
        if (categoriaDAO.deleteCategoria(categoria.getId())) {
            // Eliminar de la lista y notificar
            categoriasList.remove(position);
            categoriaAdapter.notifyItemRemoved(position);
            Toast.makeText(this, nombre + getString(R.string.eliminada), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.error_categoria, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Muestra diálogo para crear una nueva categoría
     */
    private void mostrarDialogoAdicion() {
        final EditText inputNombre = new EditText(this);
        inputNombre.setHint(R.string.ej_vestimenta);
        inputNombre.setPadding(50, 50, 50, 50);

        final EditText inputDescripcion = new EditText(this);
        inputDescripcion.setHint(R.string.descripcion_op);
        inputDescripcion.setPadding(50, 50, 50, 50);

        // Crear un contenedor para ambos EditTexts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputNombre);
        layout.addView(inputDescripcion);

        new AlertDialog.Builder(this)
                .setTitle(R.string.nueva_categoria)
                .setView(layout)
                .setPositiveButton(R.string.anadir, (dialog, which) -> {
                    String nuevoNombre = inputNombre.getText().toString().trim();
                    String nuevaDescripcion = inputDescripcion.getText().toString().trim();

                    if (nuevoNombre.isEmpty()) {
                        Toast.makeText(this, R.string.nombre_no_vacio, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verificar que no exista otra categoría con ese nombre
                    if (categoriaDAO.existsByName(nuevoNombre)) {
                        Toast.makeText(this, R.string.categoria_existente, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Insertar en BD
                    long id = categoriaDAO.insertCategoria(nuevoNombre, nuevaDescripcion);
                    if (id != -1) {
                        Categoria nuevaCategoria = new Categoria((int) id, nuevoNombre, nuevaDescripcion);
                        categoriasList.add(nuevaCategoria);
                        categoriaAdapter.notifyItemInserted(categoriasList.size() - 1);
                        Toast.makeText(this, nuevoNombre + getString(R.string.anadida), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.error_guardar_cat, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar_3, null)
                .show();
    }

    /**
     * Muestra diálogo para editar una categoría existente
     * @param position Posición en la lista
     */
    private void mostrarDialogoEdicion(final int position) {
        final Categoria categoriaAEditar = categoriasList.get(position);

        final EditText inputNombre = new EditText(this);
        inputNombre.setText(categoriaAEditar.getNombre());
        inputNombre.setPadding(50, 50, 50, 50);

        final EditText inputDescripcion = new EditText(this);
        inputDescripcion.setText(categoriaAEditar.getDescripcion());
        inputDescripcion.setHint(R.string.descripcion_op);
        inputDescripcion.setPadding(50, 50, 50, 50);

        // Crear un contenedor para ambos EditTexts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(inputNombre);
        layout.addView(inputDescripcion);

        new AlertDialog.Builder(this)
                .setTitle(R.string.editar_categoria)
                .setView(layout)
                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    String nuevoNombre = inputNombre.getText().toString().trim();
                    String nuevaDescripcion = inputDescripcion.getText().toString().trim();

                    if (nuevoNombre.isEmpty()) {
                        Toast.makeText(this, R.string.nombre_no_vacio, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verificar que no exista otra categoría con ese nombre (excepto la actual)
                    if (categoriaDAO.existsByNameExcluding(nuevoNombre, categoriaAEditar.getId())) {
                        Toast.makeText(this, R.string.categoria_existente, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Actualizar en BD
                    if (categoriaDAO.updateCategoria(categoriaAEditar.getId(), nuevoNombre, nuevaDescripcion)) {
                        categoriaAEditar.setNombre(nuevoNombre);
                        categoriaAEditar.setDescripcion(nuevaDescripcion);
                        categoriaAdapter.notifyItemChanged(position);
                        Toast.makeText(this, R.string.categoria_act, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.error_act_categoria, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancelar_3, null)
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