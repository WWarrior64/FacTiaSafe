package sv.edu.catolica.factiasafe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * GarantiasActivity — muestra lista de garantías, chips de filtrado y abre detalle de factura.
 * Todo en el mismo paquete: FaSafeDB debe existir (tú ya lo tienes).
 */
public class GarantiasActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private Chip selectedChip;
    private WarrantyAdapter adapter;
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();

    private List<Warranty> warrantyList = new ArrayList<>();
    private String currentFilter = "RECENTES"; // filtro por defecto

    private android.os.Handler searchHandler = new android.os.Handler();
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setActivityLayout(R.layout.activity_garantias);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
        setToolbarTitle("Garantías");
        showUpButton(false);

        ImageButton searchButton = toolbar.findViewById(R.id.search_button);
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> handleSearchClick());
        }

        setupFilterChips();
        recyclerView = findViewById(R.id.recycler_garantias);
        setupRecyclerView();

        Chip chipAz = findViewById(R.id.chip_az);
        if (chipAz != null) handleChipSelection(chipAz);

        // Carga inicial
        loadWarranties(currentFilter);
    }

    @Override
    protected int getBottomNavItemId() {
        return R.id.navigation_garantias;
    }

    private void setupFilterChips() {
        Chip chipAz = findViewById(R.id.chip_az);
        Chip chipRecientes = findViewById(R.id.chip_recientes);
        Chip chipAntiguos = findViewById(R.id.chip_antiguos);
        Chip chipProximoVencer = findViewById(R.id.chip_proximo_vencer);

        if (chipAz != null) chipAz.setOnClickListener(v -> handleChipClick((Chip) v, "AZ"));
        if (chipRecientes != null) chipRecientes.setOnClickListener(v -> handleChipClick((Chip) v, "RECENTES"));
        if (chipAntiguos != null) chipAntiguos.setOnClickListener(v -> handleChipClick((Chip) v, "ANTIGUOS"));
        if (chipProximoVencer != null) chipProximoVencer.setOnClickListener(v -> handleChipClick((Chip) v, "PROXIMO_VENCER"));
    }

    private void handleChipClick(Chip chip, String filterType) {
        handleChipSelection(chip);
        Toast.makeText(this, "Filtrando garantías por: " + chip.getText(), Toast.LENGTH_SHORT).show();
        loadWarranties(filterType);
    }

    private void handleChipSelection(Chip newSelectedChip) {
        int fallbackStroke = ContextCompat.getColor(this, R.color.gris_inactivo);
        int fallbackOnPrimary = ContextCompat.getColor(this, R.color.white);
        int transparent = Color.TRANSPARENT;

        int colorPrimary = MaterialColors.getColor(newSelectedChip, androidx.appcompat.R.attr.colorPrimary);
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

        if (selectedChip != null) {
            selectedChip.setChipBackgroundColor(ColorStateList.valueOf(transparent));
            selectedChip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
            selectedChip.setTextColor(ColorStateList.valueOf(strokeColor));
        }

        newSelectedChip.setChipBackgroundColor(ColorStateList.valueOf(colorPrimary));
        newSelectedChip.setChipStrokeWidth(0f);
        newSelectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));

        selectedChip = newSelectedChip;
    }

    private void setupRecyclerView() {
        adapter = new WarrantyAdapter(this, new ArrayList<>(), warranty -> {
            if (warranty.invoiceId > 0) {
                try {
                    Intent i = new Intent(this, DetalleFacturaActivity.class); // ajusta si tu activity tiene otro nombre
                    i.putExtra("invoice_id", (int) warranty.invoiceId);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "Abrir factura: id=" + warranty.invoiceId, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Garantía seleccionada: id=" + warranty.id, Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadWarranties(String filter) {
        if (filter != null) currentFilter = filter;

        // limpiar lista y notificar (evita flicker)
        warrantyList.clear();
        adapter.setItems(new ArrayList<>());

        // Ejecutar en bgExecutor para no bloquear UI
        bgExecutor.submit(() -> {
            FaSafeDB dbHelper = new FaSafeDB(GarantiasActivity.this);
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = dbHelper.getReadableDatabase();

                // Construir ORDER BY
                String orderBy;
                if ("AZ".equalsIgnoreCase(filter)) {
                    orderBy = "w.product_name COLLATE NOCASE ASC";
                } else if ("RECENTES".equalsIgnoreCase(filter)) {
                    orderBy = "i.created_at DESC";
                } else if ("ANTIGUOS".equalsIgnoreCase(filter)) {
                    orderBy = "i.created_at ASC";
                } else if ("PROXIMO_VENCER".equalsIgnoreCase(filter)) {
                    orderBy = "w.warranty_end IS NULL, w.warranty_end ASC";
                } else {
                    orderBy = "w.created_at DESC";
                }

                String sql = "SELECT w.id AS w_id, w.invoice_id AS w_invoice_id, w.product_name, " +
                        "w.warranty_start, w.warranty_end, w.warranty_months, " +
                        "i.thumbnail_path, i.company_name, i.date AS invoice_date " +
                        "FROM warranties w " +
                        "LEFT JOIN invoices i ON w.invoice_id = i.id " +
                        " ORDER BY " + orderBy + " LIMIT 500";

                cursor = db.rawQuery(sql, null);
                List<Warranty> temp = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Warranty w = new Warranty();

                        int idx = cursor.getColumnIndex("w_id");
                        if (idx != -1 && !cursor.isNull(idx)) w.id = cursor.getLong(idx);

                        idx = cursor.getColumnIndex("w_invoice_id");
                        if (idx != -1 && !cursor.isNull(idx)) w.invoiceId = cursor.getLong(idx);

                        idx = cursor.getColumnIndex("product_name");
                        if (idx != -1) w.productName = cursor.isNull(idx) ? null : cursor.getString(idx);

                        idx = cursor.getColumnIndex("warranty_start");
                        if (idx != -1) w.warrantyStart = cursor.isNull(idx) ? null : cursor.getString(idx);

                        idx = cursor.getColumnIndex("warranty_end");
                        if (idx != -1) w.warrantyEnd = cursor.isNull(idx) ? null : cursor.getString(idx);

                        idx = cursor.getColumnIndex("warranty_months");
                        if (idx != -1 && !cursor.isNull(idx)) w.warrantyMonths = cursor.getInt(idx);

                        idx = cursor.getColumnIndex("thumbnail_path");
                        if (idx != -1) w.thumbnailPath = cursor.isNull(idx) ? null : cursor.getString(idx);

                        idx = cursor.getColumnIndex("company_name");
                        if (idx != -1) w.companyName = cursor.isNull(idx) ? null : cursor.getString(idx);

                        idx = cursor.getColumnIndex("invoice_date");
                        if (idx != -1) w.invoiceDate = cursor.isNull(idx) ? null : cursor.getString(idx);

                        temp.add(w);
                    } while (cursor.moveToNext());
                }

                // actualizar lista principal
                warrantyList.clear();
                warrantyList.addAll(temp);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if (cursor != null) cursor.close(); } catch (Exception ignored) {}
                try { if (db != null) db.close(); } catch (Exception ignored) {}
                try { dbHelper.close(); } catch (Exception ignored) {}
            }

            // actualizar UI
            runOnUiThread(() -> {
                adapter.setItems(warrantyList);
                if (warrantyList.isEmpty()) {
                    Toast.makeText(GarantiasActivity.this, "No hay garantías para mostrar", Toast.LENGTH_SHORT).show();
                }
            });
        });
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
        final List<Warranty> results = new ArrayList<>();
        final WarrantyAdapter searchAdapter = new WarrantyAdapter(this, results, warranty -> {
            if (warranty.invoiceId > 0) {
                try {
                    Intent i = new Intent(this, DetalleFacturaActivity.class);
                    i.putExtra("invoice_id", (int) warranty.invoiceId);
                    startActivity(i);
                } catch (Exception e) {
                    Toast.makeText(this, "Abrir factura: id=" + warranty.invoiceId, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Garantía seleccionada: id=" + warranty.id, Toast.LENGTH_SHORT).show();
            }
        });
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
        popupWindow.setClippingEnabled(false);

        // Listener para clicks en items (usa RecyclerItemClickListener que ya tienes)
        final RecyclerItemClickListener rvListener = new RecyclerItemClickListener(this, resultsRv, (view, position) -> {
            try {
                Warranty warranty = results.get(position);
                popupWindow.dismiss();
                if (warranty.invoiceId > 0) {
                    Intent i = new Intent(this, DetalleFacturaActivity.class);
                    i.putExtra("invoice_id", (int) warranty.invoiceId);
                    startActivity(i);
                }
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
        final View anchor = findViewById(R.id.toolbar);
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

    private void runSearchQuery(String q, List<Warranty> resultsList, WarrantyAdapter adapter, android.widget.TextView emptyView) {
        // Ejecuta en hilo de background
        new Thread(() -> {
            resultsList.clear();
            FaSafeDB dbHelper = new FaSafeDB(GarantiasActivity.this);
            SQLiteDatabase db = null;
            android.database.Cursor cursor = null;
            try {
                db = dbHelper.getReadableDatabase();
                String like = "%" + q + "%";

                // Query base: traemos columnas principales + notas de la factura asociada
                String sql;
                String[] args;
                if (q == null || q.isEmpty()) {
                    sql = "SELECT w.id, w.invoice_id, w.product_name, w.warranty_start, w.warranty_end, w.warranty_months, " +
                            "i.thumbnail_path, i.company_name, i.date " +
                            "FROM warranties w " +
                            "LEFT JOIN invoices i ON w.invoice_id = i.id " +
                            "ORDER BY w.created_at DESC LIMIT 50";
                    args = new String[]{};
                } else {
                    String idArg = "-1";
                    try { Integer.parseInt(q); idArg = q; } catch (Exception ignored) {}
                    sql = "SELECT w.id, w.invoice_id, w.product_name, w.warranty_start, w.warranty_end, w.warranty_months, " +
                            "i.thumbnail_path, i.company_name, i.date " +
                            "FROM warranties w " +
                            "LEFT JOIN invoices i ON w.invoice_id = i.id " +
                            "WHERE ( ? != '-1' AND w.id = ? ) OR w.product_name LIKE ? OR i.company_name LIKE ? " +
                            "ORDER BY w.created_at DESC LIMIT 200";
                    args = new String[]{ idArg, idArg, like, like };
                }

                cursor = db.rawQuery(sql, args);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        Warranty warranty = new Warranty();
                        warranty.id = cursor.getInt(0);
                        warranty.invoiceId = cursor.isNull(1) ? 0 : cursor.getInt(1);
                        warranty.productName = cursor.isNull(2) ? "" : cursor.getString(2);
                        warranty.warrantyStart = cursor.isNull(3) ? "" : cursor.getString(3);
                        warranty.warrantyEnd = cursor.isNull(4) ? "" : cursor.getString(4);
                        warranty.warrantyMonths = cursor.isNull(5) ? 0 : cursor.getInt(5);
                        warranty.thumbnailPath = cursor.isNull(6) ? null : cursor.getString(6);
                        warranty.companyName = cursor.isNull(7) ? "Sin nombre" : cursor.getString(7);
                        warranty.invoiceDate = cursor.isNull(8) ? "" : cursor.getString(8);

                        resultsList.add(warranty);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                // opcional: manejar/logear si lo consideras
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // recarga al volver
        loadWarranties(currentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bgExecutor.shutdownNow();
    }

    // dentro de GarantiasActivity (como inner static class) o en su propio archivo
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