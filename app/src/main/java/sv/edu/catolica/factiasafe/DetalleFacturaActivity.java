package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DetalleFacturaActivity extends AppCompatActivity {

    private TextView textoNombreEmpresa, textoNumeroFactura, textoFecha, textSubtotalValue,
            textoImpuestoAplicado, textoPorcentajeImpuesto, textoCantidadImpuesto,
            textoPorcentajeDescuento, textoCantidadDescuento, textTotalValue,
            textoFechaInicioGarantia, textoFechaFinGarantia, textoGarantia,
            textoTiendaComercio, textoCategoria, textoItems, textoDatosExtras;

    private ImageView imagenFactura;

    private ImageButton botonVolver;

    private LinearLayout actionEditar, actionBorrar, actionCancelar;

    private int invoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_factura);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener el ID de la factura desde el Intent
        invoiceId = getIntent().getIntExtra("invoice_id", -1);
        if (invoiceId == -1) {
            Toast.makeText(this, "ID de factura no proporcionado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar vistas
        initializeViews();

        // Cargar datos
        loadInvoiceData();

        // Configurar listeners para botones
        setButtonListeners();
    }

    private void initializeViews() {
        botonVolver = findViewById(R.id.boton_volver);
        textoNombreEmpresa = findViewById(R.id.texto_nombre_empresa);
        textoNumeroFactura = findViewById(R.id.texto_numero_factura);
        textoFecha = findViewById(R.id.texto_fecha);
        textSubtotalValue = findViewById(R.id.text_subtotal_value);
        textoImpuestoAplicado = findViewById(R.id.texto_impuesto_aplicado);
        textoPorcentajeImpuesto = findViewById(R.id.texto_porcentaje_impuesto);
        textoCantidadImpuesto = findViewById(R.id.texto_cantidad_impuesto);
        textoPorcentajeDescuento = findViewById(R.id.texto_porcentaje_descuento);
        textoCantidadDescuento = findViewById(R.id.texto_cantidad_descuento);
        textTotalValue = findViewById(R.id.text_total_value);
        textoFechaInicioGarantia = findViewById(R.id.texto_fecha_inicio_garantia);
        textoFechaFinGarantia = findViewById(R.id.texto_fecha_fin_garantia);
        textoGarantia = findViewById(R.id.texto_garantia);
        textoTiendaComercio = findViewById(R.id.texto_tienda_comercio);
        textoCategoria = findViewById(R.id.texto_categoria);
        textoItems = findViewById(R.id.texto_items);
        textoDatosExtras = findViewById(R.id.texto_datos_extras);
        imagenFactura = findViewById(R.id.imagen_factura);
        actionEditar = findViewById(R.id.action_editar);
        actionBorrar = findViewById(R.id.action_borrar);
        actionCancelar = findViewById(R.id.action_cancelar);
    }

    private void loadInvoiceData() {
        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Cargar datos principales de la factura
        Cursor invoiceCursor = db.rawQuery("SELECT * FROM invoices WHERE id = ?", new String[]{String.valueOf(invoiceId)});
        if (!invoiceCursor.moveToFirst()) {
            Toast.makeText(this, "Factura no encontrada", Toast.LENGTH_SHORT).show();
            invoiceCursor.close();
            db.close();
            finish();
            return;
        }

        String companyName = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("company_name"));
        String externalId = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("external_id"));
        String date = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("date"));
        double subtotal = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("subtotal"));
        double taxPercentage = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("tax_percentage"));
        double taxAmount = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("tax_amount"));
        double discountPercentage = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("discount_percentage"));
        double discountAmount = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("discount_amount"));
        double total = invoiceCursor.getDouble(invoiceCursor.getColumnIndexOrThrow("total"));
        String currency = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("currency"));
        String notes = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("notes"));
        String thumbnailPath = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("thumbnail_path"));
        int storeId = invoiceCursor.getInt(invoiceCursor.getColumnIndexOrThrow("store_id"));
        int categoryId = invoiceCursor.getInt(invoiceCursor.getColumnIndexOrThrow("category_id"));

        textoNombreEmpresa.setText(companyName != null ? companyName : "N/A");
        textoNumeroFactura.setText(externalId != null ? externalId : "N/A");
        textoFecha.setText(date != null ? date : "N/A");
        textSubtotalValue.setText(currency != null ? currency + " " + subtotal : "N/A");
        textoImpuestoAplicado.setText("IVA"); // Asumiendo que es IVA; ajusta si hay un campo para el nombre del impuesto
        textoPorcentajeImpuesto.setText(String.valueOf(taxPercentage));
        textoCantidadImpuesto.setText(currency != null ? currency + " " + taxAmount : "N/A");
        textoPorcentajeDescuento.setText(String.valueOf(discountPercentage));
        textoCantidadDescuento.setText(currency != null ? currency + " " + discountAmount : "N/A");
        textTotalValue.setText(currency != null ? currency + " " + total : "N/A");
        textoDatosExtras.setText(notes != null ? notes : "N/A");

        // Cargar imagen de miniatura si existe
        if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath);
            if (bitmap != null) {
                imagenFactura.setImageBitmap(bitmap);
            } else {
                // Fallback si bitmap es null
                imagenFactura.setImageResource(R.drawable.factura_placeholder4); // Asume un placeholder drawable
            }
        } else {
            imagenFactura.setImageResource(R.drawable.factura_placeholder4); // Placeholder si path null o vacío
        }

        // Cargar nombre de la tienda
        textoTiendaComercio.setText(getStoreName(db, storeId));

        // Cargar nombre de la categoría
        textoCategoria.setText(getCategoryName(db, categoryId));

        invoiceCursor.close();

        // Cargar items
        loadItems(db);

        // Cargar garantía
        loadWarranty(db);

        db.close();
    }

    private String getStoreName(SQLiteDatabase db, int storeId) {
        Cursor cursor = db.rawQuery("SELECT name FROM stores WHERE id = ?", new String[]{String.valueOf(storeId)});
        String name = "N/A";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0) != null ? cursor.getString(0) : "N/A";
        }
        cursor.close();
        return name;
    }

    private String getCategoryName(SQLiteDatabase db, int categoryId) {
        Cursor cursor = db.rawQuery("SELECT name FROM categories WHERE id = ?", new String[]{String.valueOf(categoryId)});
        String name = "N/A";
        if (cursor.moveToFirst()) {
            name = cursor.getString(0) != null ? cursor.getString(0) : "N/A";
        }
        cursor.close();
        return name;
    }

    private void loadItems(SQLiteDatabase db) {
        StringBuilder itemsBuilder = new StringBuilder();
        Cursor itemsCursor = db.rawQuery("SELECT description, quantity, unit_price FROM invoice_items WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        while (itemsCursor.moveToNext()) {
            String description = itemsCursor.getString(0) != null ? itemsCursor.getString(0) : "N/A";
            double quantity = itemsCursor.getDouble(1);
            double unitPrice = itemsCursor.getDouble(2);
            itemsBuilder.append(description).append(" ; ").append(quantity).append(" ; ").append(unitPrice).append("\n");
        }
        itemsCursor.close();
        textoItems.setText(itemsBuilder.length() > 0 ? itemsBuilder.toString() : "N/A");
    }

    private void loadWarranty(SQLiteDatabase db) {
        Cursor warrantyCursor = db.rawQuery("SELECT warranty_start, warranty_end FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        if (warrantyCursor.moveToFirst()) {
            String startDate = warrantyCursor.getString(0);
            String endDate = warrantyCursor.getString(1);
            textoFechaInicioGarantia.setText(startDate != null ? startDate : "N/A");
            textoFechaFinGarantia.setText(endDate != null ? endDate : "N/A");

            // Calcular duración en meses si ambas fechas están disponibles
            if (startDate != null && endDate != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date start = sdf.parse(startDate);
                    Date end = sdf.parse(endDate);

                    Calendar calStart = Calendar.getInstance();
                    calStart.setTime(start);
                    Calendar calEnd = Calendar.getInstance();
                    calEnd.setTime(end);

                    int years = calEnd.get(Calendar.YEAR) - calStart.get(Calendar.YEAR);
                    int months = calEnd.get(Calendar.MONTH) - calStart.get(Calendar.MONTH);
                    int totalMonths = years * 12 + months;

                    // Ajustar si el día del mes es menor
                    if (calEnd.get(Calendar.DAY_OF_MONTH) < calStart.get(Calendar.DAY_OF_MONTH)) {
                        totalMonths--;
                    }

                    textoGarantia.setText(totalMonths + " Meses");
                } catch (ParseException e) {
                    textoGarantia.setText("N/A");
                }
            } else {
                textoGarantia.setText("N/A");
            }
        } else {
            textoFechaInicioGarantia.setText("N/A");
            textoFechaFinGarantia.setText("N/A");
            textoGarantia.setText("N/A");
        }
        warrantyCursor.close();
    }

    private void setButtonListeners() {
        botonVolver.setOnClickListener(v -> finish());

        actionEditar.setOnClickListener(v -> {
            Intent intent = new Intent(DetalleFacturaActivity.this, EditarFacturaActivity.class);
            intent.putExtra("invoice_id", invoiceId);
            startActivity(intent);
        });

        actionBorrar.setOnClickListener(v -> {
            FaSafeDB dbHelper = new FaSafeDB(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deleted = db.delete("invoices", "id = ?", new String[]{String.valueOf(invoiceId)});
            db.close();
            if (deleted > 0) {
                Toast.makeText(this, "Factura borrada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al borrar la factura", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        actionCancelar.setOnClickListener(v -> finish());
    }
}