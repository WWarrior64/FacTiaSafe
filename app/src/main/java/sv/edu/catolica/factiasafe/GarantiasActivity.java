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
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageButton;
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

    private void handleSearchClick() {
        Toast.makeText(this, "Abriendo búsqueda de garantías...", Toast.LENGTH_SHORT).show();
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
}