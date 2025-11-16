package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class EntradaManualActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private MaterialButton buttonGuardar, buttonCancelar;
    private ViewPager2 viewPager;
    private final String[] tabTitles = new String[]{"Datos Principales", "Datos Extras"};
    private EntradaPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrada_manual);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        buttonGuardar = findViewById(R.id.button_guardar);
        buttonCancelar = findViewById(R.id.button_cancelar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Nueva Factura");
        }

        setupViewPager();
        setupListeners();
    }

    private void setupViewPager() {
        pagerAdapter = new EntradaPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Sincroniza el TabLayout con el ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();
    }

    private void setupListeners() {
        // Manejar el botón de Cancelar
        buttonCancelar.setOnClickListener(v -> finish());

        // Manejar el botón de Guardar
        buttonGuardar.setOnClickListener(v -> saveNewInvoice());

        // Activar el botón de volver atrás en la toolbar
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void saveNewInvoice() {
        if (pagerAdapter == null) {
            Toast.makeText(this, "Error interno: adapter no inicializado", Toast.LENGTH_SHORT).show();
            return;
        }

        Fragment f0 = pagerAdapter.getFragment(0);
        Fragment f1 = pagerAdapter.getFragment(1);

        if (!(f0 instanceof DatosPrincipalesFragment)) {
            for (Fragment fr : getSupportFragmentManager().getFragments()) {
                if (fr instanceof DatosPrincipalesFragment) {
                    f0 = fr;
                    break;
                }
            }
        }

        if (!(f1 instanceof DatosExtraFragment)) {
            for (Fragment fr : getSupportFragmentManager().getFragments()) {
                if (fr instanceof DatosExtraFragment) {
                    f1 = fr;
                    break;
                }
            }
        }

        if (!(f0 instanceof DatosPrincipalesFragment)) {
            Toast.makeText(this, "No se encontró DatosPrincipalesFragment", Toast.LENGTH_SHORT).show();
            return;
        }

        DatosPrincipalesFragment principalesFragment = (DatosPrincipalesFragment) f0;
        DatosExtraFragment extraFragment = (f1 instanceof DatosExtraFragment) ? (DatosExtraFragment) f1 : null;

        // Recolectar datos
        String companyName = principalesFragment.getCompanyName();
        String externalId = principalesFragment.getExternalId();
        String date = principalesFragment.getDate();
        double subtotal = principalesFragment.getSubtotal();
        double taxPct = principalesFragment.getTaxPercentage();
        double taxAmount = principalesFragment.getTaxAmount();
        double discountPct = principalesFragment.getDiscountPercentage();
        double discountAmount = principalesFragment.getDiscountAmount();
        double total = principalesFragment.getTotal();
        String currency = principalesFragment.getCurrency();
        String tienda = "";
        String categoria = "";
        String notas = "";
        String thumbnailPath = principalesFragment.getThumbnailPath();
        String warrantyStart = principalesFragment.getWarrantyStart();
        String warrantyEnd = principalesFragment.getWarrantyEnd();

        if (extraFragment != null) {
            tienda = extraFragment.getTienda();
            categoria = extraFragment.getCategoria();
            notas = extraFragment.getNotas();
        }

        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean schedulingNeeded = false;

        db.beginTransaction();
        try {
            // Insertar nueva factura
            ContentValues invoiceValues = new ContentValues();
            invoiceValues.put("company_name", companyName);
            invoiceValues.put("external_id", externalId);
            invoiceValues.put("date", date);
            invoiceValues.put("subtotal", subtotal);
            invoiceValues.put("tax_percentage", taxPct);
            invoiceValues.put("tax_amount", taxAmount);
            invoiceValues.put("discount_percentage", discountPct);
            invoiceValues.put("discount_amount", discountAmount);
            invoiceValues.put("total", total);
            invoiceValues.put("currency", currency);
            invoiceValues.put("notes", notas);
            if (!thumbnailPath.isEmpty()) {
                invoiceValues.put("thumbnail_path", thumbnailPath);
            }

            // Obtener o crear store_id y obtener category_id
            int storeId = obtenerOCrearStore(db, tienda);
            int categoryId = getCategoryIdByName(db, categoria);

            if (storeId > 0) {
                invoiceValues.put("store_id", storeId);
            }
            if (categoryId > 0) {
                invoiceValues.put("category_id", categoryId);
            }

            long invoiceId = db.insert("invoices", null, invoiceValues);
            if (invoiceId == -1) {
                throw new Exception("Error al insertar factura");
            }

            // Guardar items
            principalesFragment.saveItemsToDatabase(db, (int) invoiceId);

            // Guardar datos extra (tienda, categoría, notas, imagen)
            if (extraFragment != null) {
                extraFragment.saveIntoDatabaseWithId(db, (int) invoiceId);
            }

            // Guardar garantía si aplica
            if (!warrantyStart.isEmpty() && !warrantyEnd.isEmpty()) {
                saveWarranty(db, (int) invoiceId, companyName, warrantyStart, warrantyEnd);
                schedulingNeeded = true;
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, "Factura creada exitosamente", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("EntradaManualActivity", "Error saving invoice", e);
        } finally {
            try { db.endTransaction(); } catch (Exception ignored) {}
            try { db.close(); } catch (Exception ignored) {}
            try { dbHelper.close(); } catch (Exception ignored) {}
        }

        // Re-scheduler en background
        if (schedulingNeeded) {
            new Thread(() -> {
                try {
                    NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(getApplicationContext());
                } catch (Exception ex) {
                    Log.e("EntradaManualActivity", "Error scheduling after save: " + ex.getMessage(), ex);
                }
            }).start();
        }

        finish();
    }

    private int obtenerOCrearStore(SQLiteDatabase db, String storeName) {
        if (storeName == null || storeName.isEmpty()) return -1;

        // Buscar store existente
        android.database.Cursor cursor = db.rawQuery("SELECT id FROM stores WHERE name = ? LIMIT 1", new String[]{storeName});
        if (cursor.moveToFirst()) {
            int storeId = cursor.getInt(0);
            cursor.close();
            return storeId;
        }
        cursor.close();

        // Si no existe, crear uno nuevo
        ContentValues storeValues = new ContentValues();
        storeValues.put("name", storeName);
        long newStoreId = db.insert("stores", null, storeValues);
        return (int) newStoreId;
    }

    private int getCategoryIdByName(SQLiteDatabase db, String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) return -1;
        android.database.Cursor cursor = db.rawQuery("SELECT id FROM categories WHERE name = ? LIMIT 1", new String[]{categoryName});
        if (cursor.moveToFirst()) {
            int categoryId = cursor.getInt(0);
            cursor.close();
            return categoryId;
        }
        cursor.close();
        return -1;
    }

    private void saveWarranty(SQLiteDatabase db, int invoiceId, String productName, String warrantyStart, String warrantyEnd) {
        try {
            ContentValues warrantyValues = new ContentValues();
            warrantyValues.put("invoice_id", invoiceId);
            warrantyValues.putNull("invoice_item_id");
            warrantyValues.put("product_name", productName != null ? productName : "");
            warrantyValues.put("warranty_start", warrantyStart);
            warrantyValues.put("warranty_end", warrantyEnd);
            warrantyValues.put("warranty_months", calculateMonthsBetween(warrantyStart, warrantyEnd));
            warrantyValues.put("reminder_days_before", 7);
            warrantyValues.put("status", "active");
            warrantyValues.put("notes", "");

            long result = db.insert("warranties", null, warrantyValues);
            if (result == -1) {
                Log.w("EntradaManualActivity", "Error al insertar warranty");
            }
        } catch (Exception e) {
            Log.e("EntradaManualActivity", "Error saving warranty: " + e.getMessage(), e);
        }
    }

    private int calculateMonthsBetween(String startYYYYMMDD, String endYYYYMMDD) {
        if (startYYYYMMDD == null || endYYYYMMDD == null || startYYYYMMDD.isEmpty() || endYYYYMMDD.isEmpty()) {
            return 0;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date s = sdf.parse(startYYYYMMDD);
            java.util.Date e = sdf.parse(endYYYYMMDD);
            if (s == null || e == null) return 0;

            java.util.Calendar cs = java.util.Calendar.getInstance();
            cs.setTime(s);
            java.util.Calendar ce = java.util.Calendar.getInstance();
            ce.setTime(e);

            int years = ce.get(java.util.Calendar.YEAR) - cs.get(java.util.Calendar.YEAR);
            int months = ce.get(java.util.Calendar.MONTH) - cs.get(java.util.Calendar.MONTH);
            int total = years * 12 + months;
            if (ce.get(java.util.Calendar.DAY_OF_MONTH) < cs.get(java.util.Calendar.DAY_OF_MONTH)) {
                total--;
            }
            return Math.max(0, total);
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}