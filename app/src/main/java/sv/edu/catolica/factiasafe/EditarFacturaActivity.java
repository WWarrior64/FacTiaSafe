package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class EditarFacturaActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private MaterialButton buttonGuardar, buttonCancelar;
    private ViewPager2 viewPager;
    private final String[] tabTitles = new String[]{"Datos Principales", "Datos Extras"};
    private int invoiceId;
    private EditarPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_factura); // Update to the new layout similar to activity_entrada_manual.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get invoice ID from intent
        invoiceId = getIntent().getIntExtra("invoice_id", -1);
        if (invoiceId == -1) {
            Toast.makeText(this, "ID de factura no proporcionado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        buttonGuardar = findViewById(R.id.button_guardar);
        buttonCancelar = findViewById(R.id.button_cancelar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Editar Factura");
        }

        setupViewPager();
        setupListeners();
    }

    private void setupViewPager() {
        pagerAdapter = new EditarPagerAdapter(this, invoiceId);
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabTitles[position])).attach();
    }

    private void setupListeners() {
        // Manejar el botón de Cancelar
        buttonCancelar.setOnClickListener(v -> finish());

        // Manejar el botón de Guardar
        buttonGuardar.setOnClickListener(v -> saveChanges());
        // Activar el botón de volver atrás en la toolbar
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void saveChanges() {
    // obtener fragments desde el adapter guardado
        if (pagerAdapter == null) {
            Toast.makeText(this, "Error interno: adapter no inicializado", Toast.LENGTH_SHORT).show();
            return;
        }
        Fragment f0 = pagerAdapter.getFragment(0);
        Fragment f1 = pagerAdapter.getFragment(1);
        if (!(f0 instanceof DatosPrincipalesFragment)) {
            // fallback: intentar buscar en FragmentManager
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
        // Recolectar datos que se guardan en tabla invoices:
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
            // Actualiza invoices
            db.execSQL(
                    "UPDATE invoices SET company_name = ?, external_id = ?, date = ?, subtotal = ?, tax_percentage = ?, tax_amount = ?, discount_percentage = ?, discount_amount = ?, total = ?, currency = ?, notes = ? WHERE id = ?",
                    new Object[]{companyName, externalId, date, subtotal, taxPct, taxAmount, discountPct, discountAmount, total, currency, notas, invoiceId}
            );

            principalesFragment.saveIntoDatabase(db);
            if (extraFragment != null) extraFragment.saveIntoDatabase(db);

            db.setTransactionSuccessful();
            Toast.makeText(this, "Factura actualizada", Toast.LENGTH_SHORT).show();

            // señalamos que hay que re-crear notificaciones cuando la transacción termine
            schedulingNeeded = true;

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            try { db.endTransaction(); } catch (Exception ignored) {}
            try { db.close(); } catch (Exception ignored) {}
            try { dbHelper.close(); } catch (Exception ignored) {}
        }

        // --- AHORA, fuera de la transacción y con la DB cerrada, lanzamos el re-scheduler ---
        if (schedulingNeeded) {
            new Thread(() -> {
                try {
                    NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(getApplicationContext());
                } catch (Exception ex) {
                    android.util.Log.e("EditarFacturaActivity", "Error scheduling after save: " + ex.getMessage(), ex);
                }
            }).start();
        }

        finish();
    }
}