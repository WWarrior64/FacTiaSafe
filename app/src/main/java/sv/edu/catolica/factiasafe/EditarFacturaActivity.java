package sv.edu.catolica.factiasafe;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso; // Add Picasso dependency if not present:'

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditarFacturaActivity extends AppCompatActivity {
    private EditText campoNombreEmpresa, campoNumeroFactura, campoFecha, campoTotal, campoItems;
    private EditText campoFechaInicioGarantia, campoFechaFinGarantia;
    private ImageView imagenFactura;
    private int invoiceId;
    private FaSafeDB dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize DB
        dbHelper = new FaSafeDB(this);

        // Get invoice ID from intent
        invoiceId = getIntent().getIntExtra("INVOICE_ID", -1);
        if (invoiceId == -1) {
            Toast.makeText(this, "Error: No se encontró ID de factura", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Find views
        ImageButton botonVolver = findViewById(R.id.boton_volver);
        campoNombreEmpresa = findViewById(R.id.campo_nombre_empresa);
        campoNumeroFactura = findViewById(R.id.campo_numero_factura);
        campoFecha = findViewById(R.id.campo_fecha);
        campoTotal = findViewById(R.id.campo_total);
        campoItems = findViewById(R.id.campo_items);
        campoFechaInicioGarantia = findViewById(R.id.campo_fecha_inicio_garantia);
        campoFechaFinGarantia = findViewById(R.id.campo_fecha_fin_garantia);
        imagenFactura = findViewById(R.id.imagen_factura);
        LinearLayout actionGuardar = findViewById(R.id.action_guardar);
        LinearLayout actionCancelar = findViewById(R.id.action_cancelar);

        // Load data
        loadInvoiceData();

        // Set date pickers
        setupDatePicker(campoFecha);
        setupDatePicker(campoFechaInicioGarantia);
        setupDatePicker(campoFechaFinGarantia);

        // Button listeners
        botonVolver.setOnClickListener(v -> finish());
        actionCancelar.setOnClickListener(v -> finish());
        actionGuardar.setOnClickListener(v -> saveChanges());
    }

    private void loadInvoiceData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT company_name, external_id, date, total, thumbnail_path, ocr_text FROM invoices WHERE id = ?", new String[]{String.valueOf(invoiceId)});
        if (cursor.moveToFirst()) {
            campoNombreEmpresa.setText(cursor.getString(0));
            campoNumeroFactura.setText(cursor.getString(1));
            campoFecha.setText(cursor.getString(2));
            campoTotal.setText(String.valueOf(cursor.getDouble(3)));

            // Load image if path exists (using Picasso for simplicity; fallback to BitmapFactory if preferred)
            String thumbnailPath = cursor.getString(4);
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                Picasso.get().load("file://" + thumbnailPath).into(imagenFactura);
            }

            // For items, use ocr_text or notes as placeholder; if you have invoice_items, query and concatenate
            campoItems.setText(cursor.getString(5)); // Assuming ocr_text as items description
        }
        cursor.close();

        // Load warranty if exists
        Cursor warrantyCursor = db.rawQuery("SELECT warranty_start, warranty_end FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        if (warrantyCursor.moveToFirst()) {
            campoFechaInicioGarantia.setText(warrantyCursor.getString(0));
            campoFechaFinGarantia.setText(warrantyCursor.getString(1));
        }
        warrantyCursor.close();

        db.close();
    }

    private void setupDatePicker(EditText editText) {
        editText.setFocusable(false); // Prevent keyboard
        editText.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            String currentDate = editText.getText().toString();
            if (!currentDate.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date date = sdf.parse(currentDate);
                    calendar.setTime(date);
                } catch (ParseException e) {
                    // Ignore
                }
            }
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                editText.setText(formattedDate);
            }, year, month, day);
            datePicker.show();
        });
    }

    private void saveChanges() {
        String companyName = campoNombreEmpresa.getText().toString();
        String externalId = campoNumeroFactura.getText().toString();
        String date = campoFecha.getText().toString();
        double total;
        try {
            total = Double.parseDouble(campoTotal.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Total inválido", Toast.LENGTH_SHORT).show();
            return;
        }
        String items = campoItems.getText().toString(); // Save as ocr_text or notes
        String warrantyStart = campoFechaInicioGarantia.getText().toString();
        String warrantyEnd = campoFechaFinGarantia.getText().toString();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Update invoices
            db.execSQL("UPDATE invoices SET company_name = ?, external_id = ?, date = ?, total = ?, ocr_text = ? WHERE id = ?",
                    new Object[]{companyName, externalId, date, total, items, invoiceId});

            // Update or insert warranty (assuming one per invoice; adjust if multiple)
            Cursor checkWarranty = db.rawQuery("SELECT id FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            if (checkWarranty.moveToFirst()) {
                db.execSQL("UPDATE warranties SET warranty_start = ?, warranty_end = ? WHERE invoice_id = ?",
                        new Object[]{warrantyStart, warrantyEnd, invoiceId});
            } else if (!warrantyStart.isEmpty() && !warrantyEnd.isEmpty()) {
                db.execSQL("INSERT INTO warranties (invoice_id, warranty_start, warranty_end, status) VALUES (?, ?, ?, 'active')",
                        new Object[]{invoiceId, warrantyStart, warrantyEnd});
            }
            checkWarranty.close();

            db.setTransactionSuccessful();
            Toast.makeText(this, "Factura actualizada", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
}