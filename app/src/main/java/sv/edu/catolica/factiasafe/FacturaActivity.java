package sv.edu.catolica.factiasafe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class FacturaActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private Chip selectedChip;
    private List<Invoice> invoiceList = new ArrayList<>();
    private InvoiceAdapter adapter;
    private String currentFilter = "A/Z";

    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityLayout(R.layout.activity_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configuración de la Toolbar (métodos heredados)
        setToolbarTitle(getString(R.string.encabezado_editarfactura));
        showUpButton(false);

        // Inicializa vistas del contenido (que ahora están en activity_factura_content.xml)
        ImageButton searchButton = toolbar.findViewById(R.id.search_button);
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> handleSearchClick());
        }

        // chips y recycler
        setupFilterChips();
        recyclerView = findViewById(R.id.invoices_recycler_view);
        setupRecyclerView();

// Restaurar estado si existe
        if (savedInstanceState != null) {
            currentFilter = savedInstanceState.getString("current_filter", "A/Z");
        }

        // Seleccionar el chip correspondiente
        selectChipByFilter(currentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvoices(currentFilter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_filter", currentFilter);
    }

    // >> CLAVE: Implementar el ID de navegación <<
    @Override
    protected int getBottomNavItemId() {
        return R.id.navigation_facturas;
    }

    // ------------------------------------
    // --- Manejo de Chips de Filtro (Nuevo) ---
    // ------------------------------------
    private void setupFilterChips() {
        Chip chipAz = findViewById(R.id.chip_az);
        Chip chipRecientes = findViewById(R.id.chip_recientes);
        Chip chipAntiguos = findViewById(R.id.chip_antiguos);
        Chip chipEsteMes = findViewById(R.id.chip_este_mes);

        chipAz.setOnClickListener(v -> handleChipClick((Chip) v, "A/Z"));
        chipRecientes.setOnClickListener(v -> handleChipClick((Chip) v, "Recientes"));
        chipAntiguos.setOnClickListener(v -> handleChipClick((Chip) v, "Antiguos"));
        chipEsteMes.setOnClickListener(v -> handleChipClick((Chip) v, "Este mes"));
    }

    private void handleChipClick(Chip chip, String filterType) {
        handleChipSelection(chip);
        Toast.makeText(this, "Filtrando por: " + filterType, Toast.LENGTH_SHORT).show();
        loadInvoices(filterType); // Actualiza la lista con el filtro
    }

    private void handleChipSelection(Chip newSelectedChip) {
        int fallbackStroke = ContextCompat.getColor(this, R.color.gris_inactivo);
        int fallbackOnPrimary = ContextCompat.getColor(this, R.color.white);
        int transparent = Color.TRANSPARENT;

        // Resuelve colores desde el tema usando MaterialColors (usa el chip como view)
        int colorPrimary = MaterialColors.getColor(newSelectedChip, androidx.appcompat.R.attr.colorPrimary);

        // intenta resolver colorOnPrimary, si no existe usa fallback
        int colorOnPrimary;
        try {
            colorOnPrimary = MaterialColors.getColor(newSelectedChip, com.google.android.material.R.attr.colorOnPrimary);
        } catch (Exception e) {
            colorOnPrimary = fallbackOnPrimary;
        }

        int strokeColor;
        try {
            strokeColor = MaterialColors.getColor(newSelectedChip, com.google.android.material.R.attr.colorOnSecondary);
        } catch (Exception e) {
            strokeColor = fallbackStroke;
        }

        // Deseleccionar chip anterior
        if (selectedChip != null) {
            selectedChip.setChipBackgroundColor(ColorStateList.valueOf(transparent));
            selectedChip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
            selectedChip.setTextColor(ColorStateList.valueOf(strokeColor));
        }

        // Seleccionar nuevo chip
        newSelectedChip.setChipBackgroundColor(ColorStateList.valueOf(colorPrimary));
        newSelectedChip.setChipStrokeWidth(0f);
        newSelectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        selectedChip = newSelectedChip;
    }

    private void selectChipByFilter(String filter) {
        Chip chipToSelect = null;
        switch (filter) {
            case "A/Z":
                chipToSelect = findViewById(R.id.chip_az);
                break;
            case "Recientes":
                chipToSelect = findViewById(R.id.chip_recientes);
                break;
            case "Antiguos":
                chipToSelect = findViewById(R.id.chip_antiguos);
                break;
            case "Este mes":
                chipToSelect = findViewById(R.id.chip_este_mes);
                break;
        }

        if (chipToSelect != null) {
            handleChipSelection(chipToSelect);
        } else {
            // Default a A/Z
            chipToSelect = findViewById(R.id.chip_az);
            if (chipToSelect != null) {
                handleChipSelection(chipToSelect);
            }
            currentFilter = "A/Z";
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InvoiceAdapter(this, invoiceList);
        recyclerView.setAdapter(adapter);
    }

    private void loadInvoices(String filter) {
        invoiceList.clear();
        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Seleccionamos notas y un resumen de items usando GROUP_CONCAT
        String base =
                "SELECT i.id, i.company_name, i.date, i.total, i.currency, i.thumbnail_path, " +
                        "       COALESCE(i.notes, '') AS notes, " +
                        "       COALESCE(GROUP_CONCAT(ii_trim, ' | '), '') AS items_preview " +
                        "FROM invoices i " +
                        "LEFT JOIN (" +
                        "    SELECT invoice_id, TRIM(description) || ' x' || IFNULL(quantity, 0) AS ii_trim " +
                        "    FROM invoice_items " +
                        ") ii ON ii.invoice_id = i.id ";

        String whereClause = "";
        String orderBy = "";

        if (filter.equals("A/Z")) {
            orderBy = "company_name ASC";
        } else if (filter.equals("Recientes")) {
            orderBy = "date DESC";
        } else if (filter.equals("Antiguos")) {
            orderBy = "date ASC";
        } else if (filter.equals("Este mes")) {
            whereClause = " WHERE strftime('%Y-%m', i.date) = strftime('%Y-%m', 'now')";
            orderBy = "date DESC";
        }

        String groupBy = " GROUP BY i.id ";
        String query = base + (whereClause.isEmpty() ? "" : whereClause) + groupBy + (orderBy.isEmpty() ? "" : " ORDER BY " + orderBy);

        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Invoice invoice = new Invoice();
                invoice.setId(cursor.getInt(0));
                invoice.setCompanyName(cursor.isNull(1) ? "Sin nombre" : cursor.getString(1));
                invoice.setDate(cursor.isNull(2) ? "" : cursor.getString(2));
                invoice.setTotal(cursor.isNull(3) ? 0.0 : cursor.getDouble(3));
                invoice.setCurrency(cursor.isNull(4) ? "$" : cursor.getString(4));
                invoice.setThumbnailPath(cursor.isNull(5) ? null : cursor.getString(5));
                invoice.setNotes(cursor.isNull(6) ? "" : cursor.getString(6));

                // items_preview viene separado por " | " (puedes cambiar separador)
                Cursor itemsCursor = db.rawQuery(
                        "SELECT description FROM invoice_items WHERE invoice_id = ? LIMIT 4",
                        new String[]{String.valueOf(invoice.getId())}
                );

                StringBuilder itemsPreview = new StringBuilder();
                int count = 0;
                while (itemsCursor.moveToNext()) {
                    if (count > 0) itemsPreview.append(", ");
                    itemsPreview.append(itemsCursor.getString(0));
                    count++;
                }

                itemsCursor.close();

                // Si hay más de 4 productos, agregamos "..."
                Cursor countCursor = db.rawQuery(
                        "SELECT COUNT(*) FROM invoice_items WHERE invoice_id = ?",
                        new String[]{String.valueOf(invoice.getId())}
                );
                int totalItems = 0;
                if (countCursor.moveToFirst()) totalItems = countCursor.getInt(0);
                countCursor.close();

                if (totalItems > 4) {
                    itemsPreview.append(" ...");
                }

                invoice.setItemsPreview(itemsPreview.toString());

                invoiceList.add(invoice);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        adapter.notifyDataSetChanged();
    }


    @SuppressWarnings("All")
    private void handleSearchClick() {
        // Inflar el layout (usa tu mismo bottom_sheet_search.xml)
        final View content = getLayoutInflater().inflate(R.layout.bottom_sheet_search, null);
        final androidx.recyclerview.widget.RecyclerView resultsRv = content.findViewById(R.id.bs_search_results);
        final android.widget.EditText searchInput = content.findViewById(R.id.bs_search_input);
        final android.widget.TextView emptyTv = content.findViewById(R.id.bs_empty);
        final ImageButton closeBtn = content.findViewById(R.id.bs_close);

        // Preparar RecyclerView y adapter temporal
        final List<Invoice> results = new ArrayList<>();
        final InvoiceAdapter searchAdapter = new InvoiceAdapter(this, results);
        resultsRv.setLayoutManager(new LinearLayoutManager(this));
        resultsRv.setAdapter(searchAdapter);

        // Create PopupWindow
        final PopupWindow popupWindow = new PopupWindow(this);
        popupWindow.setContentView(content);
        popupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setFocusable(true);

        // Sin fondo (transparente), sin sombreado
        popupWindow.setBackgroundDrawable(null);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(false); // permite overlap de statusbar/toolbar en algunos casos

        // Listener para clicks en items (usa RecyclerItemClickListener que ya tienes)
        final RecyclerItemClickListener rvListener = new RecyclerItemClickListener(this, resultsRv, (view, position) -> {
            try {
                Invoice inv = results.get(position);
                popupWindow.dismiss();
                openInvoice(inv.getId());
            } catch (Exception ignored) {}
        });

        // Agregar touch listener al recycler (usamos tu clase)
        resultsRv.addOnItemTouchListener(rvListener);

        // Dismiss handler: limpiar listener y ocultar teclado
        popupWindow.setOnDismissListener(() -> {
            try { resultsRv.removeOnItemTouchListener(rvListener); } catch (Exception ignored) {}
            try {
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            } catch (Exception ignored) {}
        });

        // Close button dentro del layout
        closeBtn.setOnClickListener(v -> {
            try { popupWindow.dismiss(); } catch (Exception ignored) {}
        });

        // Mostrar el popup ANCLADO a la toolbar si existe (aparece encima)
        final View anchor = findViewById(R.id.toolbar); // asegúrate que este id exista (tu toolbar)
        // si no hay toolbar, anclar a la raíz de la activity (aparecerá en la parte superior)
        final View root = findViewById(android.R.id.content);

        // Antes de mostrar: medir contenido para obtener alto
        content.measure(
                View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );

        final int popupHeight = content.getMeasuredHeight();

        // Position: si hay toolbar, mostramos justo encima/pegado a su bottom,
        // si no, pegamos a la parte superior de la ventana.
        if (anchor != null) {
            // obtener coords del anchor en pantalla
            int[] loc = new int[2];
            anchor.getLocationOnScreen(loc);
            int anchorTop = loc[1];
            // showAtLocation con Gravity.TOP y offset Y igual al top del anchor para que cubra toolbar
            popupWindow.showAtLocation(root, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, anchorTop);
        } else {
            // sin toolbar: mostrar en top (y opcional shift si tienes statusbar)
            popupWindow.showAtLocation(root, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        }

        // Forzar que el popup no recorte contenido detrás (opcional)
        popupWindow.setAnimationStyle(0); // sin animación por defecto; cámbialo si quieres

        // Mostrar teclado tras pequeño delay para que el popup esté visible
        searchInput.postDelayed(() -> {
            searchInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }, 120);

        // Debounce: limpiar runnable anterior
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

        // TextWatcher con debounce 300ms
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                final String q = s == null ? "" : s.toString().trim();
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> runSearchQuery(q, results, searchAdapter, emptyTv);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        // Inicializa búsqueda vacía para cargar recientes
        searchInput.setText("");
    }



    /** Abre la actividad de edición (EditarFacturaActivity) pasando invoice_id */
    private void openInvoice(int invoiceId) {
        Intent i = new Intent(this, EditarFacturaActivity.class);
        i.putExtra("invoice_id", invoiceId);
        startActivity(i);
    }

    private void runSearchQuery(String q, List<Invoice> resultsList, InvoiceAdapter adapter, android.widget.TextView emptyView) {
        // Ejecuta en hilo de background
        new Thread(() -> {
            resultsList.clear();
            FaSafeDB dbHelper = new FaSafeDB(FacturaActivity.this);
            SQLiteDatabase db = null;
            android.database.Cursor cursor = null;
            try {
                db = dbHelper.getReadableDatabase();
                String like = "%" + q + "%";

                // Query base: traemos columnas principales + notes (notes está en invoices)
                String sql;
                String[] args;
                if (q == null || q.isEmpty()) {
                    sql = "SELECT id, company_name, external_id, date, total, currency, thumbnail_path, COALESCE(notes,'') " +
                            "FROM invoices " +
                            "ORDER BY date DESC LIMIT 50";
                    args = new String[]{};
                } else {
                    String idArg = "-1";
                    try { Integer.parseInt(q); idArg = q; } catch (Exception ignored) {}
                    sql = "SELECT id, company_name, external_id, date, total, currency, thumbnail_path, COALESCE(notes,'') " +
                            "FROM invoices " +
                            "WHERE ( ? != '-1' AND id = ? ) OR external_id LIKE ? OR company_name LIKE ? OR notes LIKE ? " +
                            "ORDER BY date DESC LIMIT 200";
                    args = new String[]{ idArg, idArg, like, like, like };
                }

                cursor = db.rawQuery(sql, args);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Invoice inv = new Invoice();
                        inv.setId(cursor.getInt(0));
                        inv.setCompanyName(cursor.isNull(1) ? "Sin nombre" : cursor.getString(1));
                        inv.setExternalId(cursor.isNull(2) ? "" : cursor.getString(2));
                        inv.setDate(cursor.isNull(3) ? "" : cursor.getString(3));
                        inv.setTotal(cursor.isNull(4) ? 0.0 : cursor.getDouble(4));
                        inv.setCurrency(cursor.isNull(5) ? "" : cursor.getString(5));
                        inv.setThumbnailPath(cursor.isNull(6) ? null : cursor.getString(6));
                        inv.setNotes(cursor.isNull(7) ? "" : cursor.getString(7));

                        // Traer hasta 5 items (si devuelve 5 -> hay más; mostraremos 4 + " ...")
                        android.database.Cursor itemsCursor = null;
                        try {
                            itemsCursor = db.rawQuery(
                                    "SELECT description FROM invoice_items WHERE invoice_id = ? ORDER BY id ASC LIMIT 5",
                                    new String[]{ String.valueOf(inv.getId()) }
                            );
                            StringBuilder itemsPreview = new StringBuilder();
                            int cnt = 0;
                            while (itemsCursor != null && itemsCursor.moveToNext()) {
                                if (cnt > 0) itemsPreview.append(", ");
                                String desc = itemsCursor.isNull(0) ? "" : itemsCursor.getString(0);
                                itemsPreview.append(desc);
                                cnt++;
                            }
                            // Si devolvió exactamente 5 filas, quitamos la última y añadimos " ..."
                            if (cnt == 5) {
                                // eliminar la última entrada añadida (todo después de la última ", ")
                                int lastComma = itemsPreview.lastIndexOf(", ");
                                if (lastComma != -1) itemsPreview.delete(lastComma, itemsPreview.length());
                                // añadir indicación de más
                                if (itemsPreview.length() > 0) itemsPreview.append(" ...");
                            }
                            inv.setItemsPreview(itemsPreview.toString());
                        } finally {
                            try { if (itemsCursor != null) itemsCursor.close(); } catch (Exception ignored) {}
                        }

                        resultsList.add(inv);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                // opcional: manejar/logear si lo consideras (aquí lo silenciamos para no romper UX)
            } finally {
                try { if (cursor != null) cursor.close(); } catch (Exception ignored) {}
                try { if (db != null) db.close(); } catch (Exception ignored) {}
            }

            // actualizar UI en el hilo principal
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(resultsList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        }).start();
    }



    // dentro de FacturaActivity (como inner static class) o en su propio archivo
    public static class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
        public interface OnItemClickListener { void onItemClick(View view, int position); }

        private final GestureDetector gestureDetector;
        private final OnItemClickListener clickListener;
        private final RecyclerView recyclerView;

        public RecyclerItemClickListener(Context context, RecyclerView recyclerView, OnItemClickListener listener) {
            this.recyclerView = recyclerView;
            this.clickListener = listener;
            this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onSingleTapUp(MotionEvent e) { return true; }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && gestureDetector.onTouchEvent(e)) {
                int position = rv.getChildAdapterPosition(child);
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(child, position);
                    return true; // consume para evitar doble click
                }
            }
            return false;
        }

        @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { /* no-op */ }
        @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { /* no-op */ }
    }

}