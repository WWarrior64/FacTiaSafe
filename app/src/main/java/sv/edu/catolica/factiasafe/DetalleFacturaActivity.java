package sv.edu.catolica.factiasafe;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


public class DetalleFacturaActivity extends AppCompatActivity {

    private static final int REQ_WRITE_STORAGE = 1234;
    private static final String TAG = "DetalleFacturaActivity";

    private TextView textoNombreEmpresa, textoNumeroFactura, textoFecha, textSubtotalValue,
            textoImpuestoAplicado, textoPorcentajeImpuesto, textoCantidadImpuesto,
            textoPorcentajeDescuento, textoCantidadDescuento, textTotalValue,
            textoFechaInicioGarantia, textoFechaFinGarantia, textoGarantia,
            textoTiendaComercio, textoCategoria, textoItems, textoDatosExtras;

    private ImageView imagenFactura, imagenProducto;

    private ImageButton botonVolver;

    private LinearLayout actionEditar, actionBorrar, actionExportarPdf, actionCancelar;

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

    @Override
    protected void onResume() {
        super.onResume();
        // Cargar datos cada vez que la actividad se resume (se carga o regresa)
        loadInvoiceData();
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
        imagenProducto = findViewById(R.id.imagen_producto);
        actionEditar = findViewById(R.id.action_editar);
        actionBorrar = findViewById(R.id.action_borrar);
        actionExportarPdf = findViewById(R.id.action_exportar_pdf);
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
        String productImagePath = invoiceCursor.getString(invoiceCursor.getColumnIndexOrThrow("product_image_path"));
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

        // Cargar imagen del producto si existe
        if (productImagePath != null && !productImagePath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(productImagePath);
            if (bitmap != null) {
                imagenProducto.setImageBitmap(bitmap);
            } else {
                // Fallback si bitmap es null
                imagenProducto.setImageResource(R.drawable.factura_placeholder4); // Usa el mismo placeholder o crea uno nuevo para "sin foto de producto"
            }
        } else {
            imagenProducto.setImageResource(R.drawable.factura_placeholder4); // Placeholder si path null o vacío, asume que representa "sin foto de producto"
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
            
            // Borrar garantía
            db.delete("warranties", "invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            
            // Borrar items
            db.delete("invoice_items", "invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            
            // Borrar attachments
            db.delete("attachments", "invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            
            // Borrar factura
            int deleted = db.delete("invoices", "id = ?", new String[]{String.valueOf(invoiceId)});
            db.close();
            
            if (deleted > 0) {
                Toast.makeText(this, "Factura borrada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al borrar la factura", Toast.LENGTH_SHORT).show();
            }
            finish();
        });

        actionExportarPdf.setOnClickListener(v -> {
            // verificar permiso si es necesario (solo Android < Q requiere WRITE_EXTERNAL_STORAGE)
            if (!ensureStoragePermission()) {
                // si no está concedido, la petición se hará y el usuario deberá volver a intentar
                return;
            }

            actionExportarPdf.setEnabled(false);
            Toast.makeText(DetalleFacturaActivity.this, "Generando PDF...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    String outPath = generateInvoicePdf();
                    runOnUiThread(() -> {
                        actionExportarPdf.setEnabled(true);
                        if (outPath != null) {
                            Toast.makeText(DetalleFacturaActivity.this, "PDF guardado: " + outPath, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(DetalleFacturaActivity.this, "Error generando PDF", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error generando PDF", e);
                    runOnUiThread(() -> {
                        actionExportarPdf.setEnabled(true);
                        Toast.makeText(DetalleFacturaActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });


        actionCancelar.setOnClickListener(v -> finish());
    }

    private boolean ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ no necesita WRITE_EXTERNAL_STORAGE para MediaStore RELATIVE_PATH
            return true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_STORAGE);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE_STORAGE) {
            boolean ok = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if (!ok) {
                Toast.makeText(this, "Permiso denegado: no se puede guardar en carpeta pública.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso concedido. Pulse Exportar PDF nuevamente.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------- Helpers para exportar PDF -------
    private static class PdfItem {
        String description;
        double quantity;
        double unitPrice;
        double lineTotal;
    }

    private String generateInvoicePdf() {
        FaSafeDB dbHelper = null;
        SQLiteDatabase db = null;
        PdfDocument pdf = null;
        try {
            dbHelper = new FaSafeDB(this);
            db = dbHelper.getReadableDatabase();
            // Leer settings relevantes
            String settingPath = getSetting(db, "pdf_save_path"); // ej: "Documents/FactiaSafe/Facturas"
            String includeImagesSetting = getSetting(db, "incluir_productos-tiendas"); // "true" o "false"
            boolean includeImages = includeImagesSetting != null && (includeImagesSetting.equalsIgnoreCase("true") || includeImagesSetting.equals("1"));
            // Releer datos invoice (igual que antes)
            Cursor c = db.rawQuery("SELECT * FROM invoices WHERE id = ?", new String[]{String.valueOf(invoiceId)});
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }
            String companyName = c.getString(c.getColumnIndexOrThrow("company_name"));
            String externalId = c.getString(c.getColumnIndexOrThrow("external_id"));
            String date = c.getString(c.getColumnIndexOrThrow("date"));
            double subtotal = c.getDouble(c.getColumnIndexOrThrow("subtotal"));
            double taxPercentage = c.getDouble(c.getColumnIndexOrThrow("tax_percentage"));
            double taxAmount = c.getDouble(c.getColumnIndexOrThrow("tax_amount"));
            double discountPercentage = c.getDouble(c.getColumnIndexOrThrow("discount_percentage"));
            double discountAmount = c.getDouble(c.getColumnIndexOrThrow("discount_amount"));
            double total = c.getDouble(c.getColumnIndexOrThrow("total"));
            String currency = c.getString(c.getColumnIndexOrThrow("currency"));
            String notes = c.getString(c.getColumnIndexOrThrow("notes"));
            String thumbnailPath = c.getString(c.getColumnIndexOrThrow("thumbnail_path"));
            String productImagePath = c.getString(c.getColumnIndexOrThrow("product_image_path"));
            int storeId = c.getInt(c.getColumnIndexOrThrow("store_id"));
            int categoryId = c.getInt(c.getColumnIndexOrThrow("category_id"));
            c.close();
            // Items
            List<PdfItem> items = new ArrayList<>();
            Cursor ic = db.rawQuery("SELECT description, quantity, unit_price, line_total FROM invoice_items WHERE invoice_id = ? ORDER BY id ASC", new String[]{String.valueOf(invoiceId)});
            while (ic.moveToNext()) {
                PdfItem it = new PdfItem();
                it.description = ic.isNull(0) ? "" : ic.getString(0);
                it.quantity = ic.getDouble(1);
                it.unitPrice = ic.getDouble(2);
                it.lineTotal = ic.getDouble(3);
                items.add(it);
            }
            ic.close();
            // Warranty
            String warrantyStart = null, warrantyEnd = null, warrantyNotes = null;
            Integer warrantyMonths = null;
            Cursor wc = db.rawQuery("SELECT warranty_start, warranty_end, warranty_months, notes FROM warranties WHERE invoice_id = ? LIMIT 1", new String[]{String.valueOf(invoiceId)});
            if (wc.moveToFirst()) {
                warrantyStart = wc.isNull(0) ? null : wc.getString(0);
                warrantyEnd = wc.isNull(1) ? null : wc.getString(1);
                if (!wc.isNull(2)) warrantyMonths = wc.getInt(2);
                warrantyNotes = wc.isNull(3) ? null : wc.getString(3);
            }
            wc.close();
            // Attachments
            List<String> attachments = new ArrayList<>();
            Cursor ac = db.rawQuery("SELECT path FROM attachments WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            while (ac.moveToNext()) {
                if (!ac.isNull(0)) attachments.add(ac.getString(0));
            }
            ac.close();
            String storeName = getStoreName(db, storeId);
            String categoryName = getCategoryName(db, categoryId);
            // --- Crear PDF ---
            pdf = new PdfDocument();
            // Page config A4-like
            int pageWidth = 595;
            int pageHeight = 842;
            // Márgenes aumentados
            int margin = 48;
            int rightLimit = pageWidth - margin;
            int contentWidth = pageWidth - margin * 2;
            Paint paint = new Paint();
            Paint bold = new Paint();
            bold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            bold.setTextSize(12);
            paint.setTextSize(11);
            int y; // cursor vertical
            int lineSpacing = 14;
            PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdf.startPage(pi);
            Canvas canvas = page.getCanvas();
            y = margin;
            // Header (con margen aplicado) — usar drawTextWrapped para respetar ambos márgenes
            bold.setTextSize(16);
            // company name: envolver si es largo
            int headerMaxWidth = contentWidth - 220; // reservar espacio aprox para el bloque derecho (número factura)
            y = drawTextWrapped(canvas, companyName != null ? companyName : "Empresa", margin, y, headerMaxWidth, bold, 18);
            // Número/etiquetas a la derecha (dibujo en bloque limitado a rightLimit - invoiceLabelX)
            bold.setTextSize(11);
            float invoiceLabelX = rightLimit - 200f;
            int invoiceLabelMaxWidth = (int)(rightLimit - invoiceLabelX);
            String invoiceLabel = "Factura: " + (externalId != null ? externalId : String.valueOf(invoiceId));
            y = drawTextWrapped(canvas, invoiceLabel, (int)invoiceLabelX, y - 18, invoiceLabelMaxWidth, bold, 14); // y-18 para alinearlo con company name
            y += 6; // ajustar espacio después del header
            paint.setTextSize(10);
            // Fecha (columna izquierda) — limitar anchura
            int leftInfoWidth = contentWidth / 2;
            y = drawTextWrapped(canvas, "Fecha: " + (date != null ? date : ""), margin, y, leftInfoWidth - 4, paint, 12);
            // Tienda (columna derecha) — limitar anchura
            int rightInfoX = margin + leftInfoWidth;
            int rightInfoWidth = contentWidth - leftInfoWidth;
            y = drawTextWrapped(canvas, "Tienda: " + (storeName != null ? storeName : ""), rightInfoX, y - 12, rightInfoWidth - 4, paint, 12);
            y += 8; // Espacio extra
            y += 4;
            // Items header (dibujado con márgenes)
            bold.setTextSize(12);
            // Definir anchos proporcionales para que sumen contentWidth y ocupen todo el espacio
            int descWidth = (int)(contentWidth * 0.50f); // 50% para descripción (más ancho)
            int quantWidth = (int)(contentWidth * 0.15f); // 15% para cantidad
            int priceWidth = (int)(contentWidth * 0.175f); // 17.5% para precio
            int totalWidth = contentWidth - (descWidth + quantWidth + priceWidth); // Resto para total (ajuste automático)
            int descX = margin;
            int quantX = descX + descWidth;
            int priceX = quantX + quantWidth;
            int totalX = priceX + priceWidth;
            canvas.drawText("Descripción", descX, y, bold);
            // Alinear headers de números a la derecha para mejor visual
            float tw = bold.measureText("Cant.");
            canvas.drawText("Cant.", quantX + quantWidth - tw, y, bold);
            tw = bold.measureText("Precio");
            canvas.drawText("Precio", priceX + priceWidth - tw, y, bold);
            tw = bold.measureText("Total");
            canvas.drawText("Total", totalX + totalWidth - tw, y, bold);
            y += 16;
            paint.setTextSize(10);
            // Items rows — controlar salto de página simple
            for (PdfItem it : items) {
                // Si no hay espacio para 3 líneas de texto, nueva página
                if (y > pageHeight - margin - 140) {
                    pdf.finishPage(page);
                    pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }
                y = drawTextWrapped(canvas, it.description, descX, y, descWidth, paint, lineSpacing);
                String q = String.format(Locale.getDefault(), "%.2f", it.quantity);
                String up = String.format(Locale.getDefault(), "%.2f", it.unitPrice);
                String lt = String.format(Locale.getDefault(), "%.2f", it.lineTotal);

                // Las posiciones verticales deben estar en la misma línea base de la última línea dibujada
                float baselineY = y - lineSpacing; // ya que drawTextWrapped avanzó y está al final de la línea
                // Alinear números a la derecha en sus columnas
                tw = paint.measureText(q);
                canvas.drawText(q, quantX + quantWidth - tw, baselineY, paint);
                tw = paint.measureText(up);
                canvas.drawText(up, priceX + priceWidth - tw, baselineY, paint);
                tw = paint.measureText(lt);
                canvas.drawText(lt, totalX + totalWidth - tw, baselineY, paint);
                y += 6; // separación entre filas
            }
            // Totals block (mantener margen inferior)
            if (y > pageHeight - margin - 120) {
                pdf.finishPage(page);
                pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                page = pdf.startPage(pi);
                canvas = page.getCanvas();
                y = margin;
            }
            y += 10;
            bold.setTextSize(12);
            float totalsX = rightLimit - 200f;
            canvas.drawText("Subtotal:", totalsX, y, bold);
            canvas.drawText((currency != null ? currency : "") + " " + String.format(Locale.getDefault(), "%.2f", subtotal), rightLimit - 80, y, paint);
            y += 16;
            canvas.drawText("Impuesto (" + String.format(Locale.getDefault(),"%.2f", taxPercentage) + "%):", totalsX, y, bold);
            canvas.drawText((currency != null ? currency : "") + " " + String.format(Locale.getDefault(),"%.2f", taxAmount), rightLimit - 80, y, paint);
            y += 16;
            canvas.drawText("Descuento (" + String.format(Locale.getDefault(),"%.2f", discountPercentage) + "%):", totalsX, y, bold);
            canvas.drawText((currency != null ? currency : "") + " " + String.format(Locale.getDefault(),"%.2f", discountAmount), rightLimit - 80, y, paint);
            y += 20;
            bold.setTextSize(14);
            canvas.drawText("Total:", totalsX, y, bold);
            canvas.drawText((currency != null ? currency : "") + " " + String.format(Locale.getDefault(),"%.2f", total), rightLimit - 80, y, bold);
            y += 26;
            // Warranty block (si aplica)
            if (warrantyStart != null || warrantyEnd != null || warrantyMonths != null || (warrantyNotes != null && !warrantyNotes.trim().isEmpty())) {
                if (y > pageHeight - margin - 120) {
                    pdf.finishPage(page);
                    pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }
                bold.setTextSize(12);
                canvas.drawText("Garantía", margin, y, bold);
                y += 14;
                paint.setTextSize(10);
                if (warrantyStart != null) {
                    canvas.drawText("Inicio: " + warrantyStart, margin, y, paint);
                    y += 12;
                }
                if (warrantyEnd != null) {
                    canvas.drawText("Fin: " + warrantyEnd, margin, y, paint);
                    y += 12;
                }
                if (warrantyMonths != null) {
                    canvas.drawText("Meses: " + warrantyMonths, margin, y, paint);
                    y += 12;
                }
                if (warrantyNotes != null && !warrantyNotes.trim().isEmpty()) {
                    y = drawTextWrapped(canvas, "Notas: " + warrantyNotes, margin, y, contentWidth, paint, 12);
                    y += 6;
                }
                y += 8;
            }
            // Notes
            if (notes != null && !notes.trim().isEmpty()) {
                if (y > pageHeight - margin - 120) {
                    pdf.finishPage(page);
                    pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }
                bold.setTextSize(12);
                canvas.drawText("Notas", margin, y, bold);
                y += 14;
                paint.setTextSize(10);
                y = drawTextWrapped(canvas, notes, margin, y, contentWidth, paint, 12);
                y += 8;
            }
            // Attachments list (paths)
            if (!attachments.isEmpty()) {
                if (y > pageHeight - margin - 120) {
                    pdf.finishPage(page);
                    pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }
                bold.setTextSize(12);
                canvas.drawText("Attachments", margin, y, bold);
                y += 14;
                paint.setTextSize(9);
                for (String pth : attachments) {
                    y = drawTextWrapped(canvas, pth, margin, y, contentWidth, paint, 10);
                    y += 6;
                }
            }
            // --- IMÁGENES AL FINAL (si el setting lo permite) ---
            if ( productImagePath != null && !productImagePath.isEmpty()) {
                // asegurar espacio: si no hay, terminar la página y crear nueva
                if (y > pageHeight - margin - 280) {
                    pdf.finishPage(page);
                    pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }
                // Espacio antes de imágenes
                y += 6;
                bold.setTextSize(12);
                canvas.drawText("Imágenes", margin, y, bold);
                y += 14;
                int imgMaxWidth = contentWidth;
                int imgMaxHeight = 550; // altura máxima por imagen

                // Tu Paint está perfecto. Mantenlo.
                Paint imagePaint = new Paint();
                imagePaint.setFilterBitmap(true); // Suaviza al escalar
                imagePaint.setAntiAlias(true);   // Suaviza bordes
                imagePaint.setDither(true);      // Mejora gradientes de color

                final int spacingAfterImage = 8;

                // --- DEFINIR FACTOR DE CALIDAD ---
                // 1.0f = 72 DPI (baja calidad, lo que tenías)
                // 2.0f = 144 DPI (buena calidad, buen equilibrio)
                // 3.0f = 216 DPI (alta calidad, archivos PDF más pesados)
                final float qualityFactor = 2.0f;

                // --- Thumbnail (factura) ---
                if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(thumbnailPath, opts);

                        // --- ¡EL CAMBIO CLAVE! ---
                        // Calcular los píxeles requeridos multiplicando los puntos por el factor de calidad.
                        int reqWidth = (int) (imgMaxWidth * qualityFactor);
                        int reqHeight = (int) (imgMaxHeight * qualityFactor);
                        // --- FIN DEL CAMBIO CLAVE ---

                        // Opciones de carga de alta calidad
                        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        opts.inScaled = false; // Importante

                        // Calcular inSampleSize usando los píxeles requeridos (ej. 499 * 2 = 998px)
                        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                        opts.inJustDecodeBounds = false;

                        // Cargar el bitmap con una resolución decente
                        Bitmap thumb = BitmapFactory.decodeFile(thumbnailPath, opts);

                        if (thumb != null) {
                            // Calcular el tamaño de destino en PUNTOS (en el lienzo del PDF)
                            // Esta lógica de "scale" previene que imágenes pequeñas se agranden (pixelen)
                            float scale = Math.min(1.0f, Math.min((float) imgMaxWidth / thumb.getWidth(), (float) imgMaxHeight / thumb.getHeight()));
                            int iw = (int) (thumb.getWidth() * scale);
                            int ih = (int) (thumb.getHeight() * scale);

                            // Control de salto de página
                            if (y + ih > pageHeight - margin) {
                                pdf.finishPage(page);
                                pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                                page = pdf.startPage(pi);
                                canvas = page.getCanvas();
                                y = margin;
                            }

                            // Calcular el rectángulo de destino (en PUNTOS)
                            int left = margin + (contentWidth - iw) / 2;
                            int right = left + iw;
                            Rect dst = new Rect(left, y, right, y + ih);

                            // --- OPTIMIZACIÓN ---
                            // No crees un 'Bitmap scaled' intermedio. Es innecesario y gasta memoria.
                            // Dibuja el 'thumb' (que ya tiene alta resolución) directamente en 'dst'.
                            // El 'imagePaint' se encarga de escalarlo suavemente.
                            //
                            // ELIMINADO: Bitmap scaled = Bitmap.createScaledBitmap(thumb, iw, ih, true);
                            // ELIMINADO: scaled.setDensity(DisplayMetrics.DENSITY_XXHIGH);
                            // ELIMINADO: canvas.drawBitmap(scaled, null, dst, imagePaint);

                            // CÓDIGO CORRECTO:
                            canvas.drawBitmap(thumb, null, dst, imagePaint);

                            // Reciclar el único bitmap que cargamos
                            thumb.recycle();

                            y += ih + spacingAfterImage;
                        }
                    } catch (Exception ignored) {}
                }

                // --- Product image ---
                if (includeImages && ( productImagePath != null && !productImagePath.isEmpty())) {
                    try {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(productImagePath, opts);

                        // Aplicar el mismo factor de calidad
                        int reqWidth = (int) (imgMaxWidth * qualityFactor);
                        int reqHeight = (int) (imgMaxHeight * qualityFactor);

                        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        opts.inScaled = false;

                        opts.inSampleSize = calculateInSampleSize(opts, reqWidth, reqHeight);
                        opts.inJustDecodeBounds = false;

                        Bitmap prod = BitmapFactory.decodeFile(productImagePath, opts);

                        if (prod != null) {
                            float scale = Math.min(1.0f, Math.min((float) imgMaxWidth / prod.getWidth(), (float) imgMaxHeight / prod.getHeight()));
                            int iw = (int) (prod.getWidth() * scale);
                            int ih = (int) (prod.getHeight() * scale);

                            if (y + ih > pageHeight - margin) {
                                pdf.finishPage(page);
                                pi = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                                page = pdf.startPage(pi);
                                canvas = page.getCanvas();
                                y = margin;
                            }

                            int left = margin + (contentWidth - iw) / 2;
                            int right = left + iw;
                            Rect dst = new Rect(left, y, right, y + ih);

                            // Dibujar directamente, igual que antes
                            canvas.drawBitmap(prod, null, dst, imagePaint);
                            prod.recycle();

                            y += ih + spacingAfterImage;
                        }
                    } catch (Exception ignored) {}
                }
            }
            // finish last page
            pdf.finishPage(page);
            // Guardar PDF usando el setting de ruta (si existe)
            // timestamp
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            // Obtener nombre de la compañía (usa la variable companyName ya leída del cursor más arriba)
            String companyForFilename = (companyName != null) ? companyName.trim() : "";
            // fallback si viene vacío
            if (companyForFilename.isEmpty()) companyForFilename = "Factura";
            // sanitizar: quitar caracteres inválidos en nombres de fichero y normalizar espacios
            // dejamos sólo letras, números, guiones bajos y guiones medios
            companyForFilename = companyForFilename.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            // opcional: limitar longitud para evitar nombres excesivamente largos
            int MAX_COMPANY_LEN = 40;
            if (companyForFilename.length() > MAX_COMPANY_LEN) {
                companyForFilename = companyForFilename.substring(0, MAX_COMPANY_LEN);
            }
            // construir nombre descriptivo
            String fname = "Factura_" + companyForFilename + "_" + invoiceId + "_" + ts + ".pdf";
            String savedRef = null;
            try {
                // Intento principal / con normalización interna del savePdf method
                savedRef = savePdfToPublicDownloadsAndClose(pdf, fname, settingPath);
            } catch (Exception eSave) {
                Log.e(TAG, "savePdfToPublicDownloadsAndClose fallo: " + eSave.getMessage(), eSave);
                // Intento fallback: guardar en carpeta privada externa de la app (getExternalFilesDir)
                try {
                    File fallbackDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if (fallbackDir == null) fallbackDir = getFilesDir();
                    if (!fallbackDir.exists()) fallbackDir.mkdirs();
                    File outFile = new File(fallbackDir, fname);
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        // IMPORTANTE: pdf sigue abierto aquí (no lo cerramos en savePdf...), así que podemos escribir
                        pdf.writeTo(fos);
                        fos.flush();
                    }
                    savedRef = outFile.getAbsolutePath();
                    // actualizar campo pdf_path en la DB
                    try {
                        SQLiteDatabase w = dbHelper.getWritableDatabase();
                        ContentValues cv = new ContentValues();
                        cv.put("pdf_path", savedRef);
                        w.update("invoices", cv, "id = ?", new String[]{String.valueOf(invoiceId)});
                        w.close();
                    } catch (Exception ignored) {}
                    Log.i(TAG, "PDF guardado en carpeta privada de app (fallback): " + savedRef);
                } catch (Exception fallbackEx) {
                    Log.e(TAG, "Fallback guardar PDF falló", fallbackEx);
                    // propagar para que el outer catch lo capture; NO cerrar pdf aquí (lo hará el finally)
                    throw new RuntimeException("Error guardando PDF: " + eSave.getMessage() + " | Fallback: " + fallbackEx.getMessage(), fallbackEx);
                }
            } finally {
                // Cerrar pdf aquí, una vez terminadas todas las operaciones (éste es el lugar correcto)
                try {
                    if (pdf != null) pdf.close();
                } catch (Exception ignored) {}
            }
            // actualizar campo pdf_path en la tabla invoices (guardamos el savedRef real)
            try {
                SQLiteDatabase w = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("pdf_path", savedRef);
                w.update("invoices", cv, "id = ?", new String[]{String.valueOf(invoiceId)});
                w.close();
            } catch (Exception ignored) {}
            // Construir una ruta legible para el usuario (mostrar en Toast)
            // Si el usuario configuró settingPath, la usaremos como base para mostrar:
            // ej: settingPath = "Documents/FactiaSafe/Facturas" -> "/sdcard/Documents/FactiaSafe/Facturas/filename"
            String displayPath;
            if (settingPath != null && !settingPath.trim().isEmpty()) {
                String s = settingPath.trim();
                while (s.startsWith("/")) s = s.substring(1);
                // Normalizar para mostrar en /sdcard/... (p. ej. Documents/... o Downloads/...)
                String basePrefix = "/sdcard/";
                displayPath = basePrefix + s;
                if (!displayPath.endsWith("/")) displayPath += "/";
                displayPath += fname;
            } else {
                // si no hay configuración, preferimos mostrar savedRef si es una ruta absoluta,
                // sino construir Downloads por defecto
                if (savedRef != null && savedRef.startsWith("/")) {
                    displayPath = savedRef;
                } else {
                    displayPath = "/sdcard/Download/FactiaSafe/" + fname;
                }
            }
            // devolver la ruta legible (para que el Toast muestre esto)
            return displayPath;
        } catch (Exception e) {
            Log.e(TAG, "generateInvoicePdf error", e);
            if (pdf != null) pdf.close();
            return null;
        } finally {
            try {
                if (db != null) db.close();
            } catch (Exception ignored) {}
            try {
                if (dbHelper != null) dbHelper.close();
            } catch (Exception ignored) {}
        }
    }


    /**
     * Guarda el PdfDocument usando MediaStore. NO cierra el PdfDocument (caller lo cierra).
     * -> Si settingPath apunta a Documents/... usa MediaStore.Files y RELATIVE_PATH = Documents/...
     * -> Si no, usa MediaStore.Downloads.
     * Devuelve URI string (Android Q+) o path absoluto (API < 29).
     */
    private String savePdfToPublicDownloadsAndClose(PdfDocument pdf, String filename, String settingPath) throws Exception {
        // No cerramos pdf aquí.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Normalizar settingPath
            String rel = null;
            if (settingPath != null) {
                rel = settingPath.trim();
                while (rel.startsWith("/")) rel = rel.substring(1);
                if (rel.isEmpty()) rel = null;
            }

            // Decidir colección y RELATIVE_PATH
            Uri collection;
            String relativePath;
            boolean wantDocuments = false;
            if (rel != null && rel.toLowerCase().startsWith("documents")) {
                wantDocuments = true;
                // extraer subpath después de "Documents/"
                String sub = rel.replaceFirst("(?i)^documents/*", "").replaceAll("^/+", "");
                if (sub.isEmpty()) sub = "FactiaSafe";
                relativePath = Environment.DIRECTORY_DOCUMENTS + "/" + sub;
                // Para documentos usamos MediaStore.Files
                collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            } else {
                // Default a Downloads
                String sub = (rel != null) ? rel : "FactiaSafe";
                // si el usuario nominó algo que empieza por "Downloads" o "Download"
                if (sub.toLowerCase().startsWith("downloads") || sub.toLowerCase().startsWith("download")) {
                    sub = sub.replaceFirst("(?i)^downloads/*|(?i)^download/*", "");
                    if (sub.startsWith("/")) sub = sub.substring(1);
                    if (sub.isEmpty()) sub = "FactiaSafe";
                }
                relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + sub;
                collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = null;
            try {
                uri = getContentResolver().insert(collection, values);
            } catch (IllegalArgumentException | SecurityException ex) {
                // si falla por restricciones de colección (ej: ROM extra estricta),
                // si intentamos Documents y falla, caemos a Downloads/FactiaSafe
                Log.w(TAG, "Insert en MediaStore falló para collection " + collection + ": " + ex.getMessage());
                if (wantDocuments) {
                    // reintentar con Downloads/FactiaSafe
                    collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FactiaSafe");
                    uri = getContentResolver().insert(collection, values);
                } else {
                    // si ya era Downloads, volver a lanzar la excepción
                    throw ex;
                }
            }

            if (uri == null) throw new Exception("No se pudo crear URI en MediaStore (uri==null)");

            // escribir el PDF
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("No se pudo abrir OutputStream para URI");
                pdf.writeTo(out); // pdf debería estar abierto
                out.flush();
            }

            // marcar como no pendiente
            try {
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
            } catch (Exception updEx) {
                Log.w(TAG, "No se pudo actualizar IS_PENDING (no crítico): " + updEx.getMessage());
            }

            return uri.toString();

        } else {
            // API < 29: escribir en public directories con File API como fallback
            File base;
            if (settingPath != null && !settingPath.trim().isEmpty()) {
                String s = settingPath.trim();
                if (s.toLowerCase().startsWith("documents") || s.startsWith("/Documents") || s.toLowerCase().startsWith("document")) {
                    base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    String sub = s.replaceFirst("(?i)^/*documents/*", "").replaceAll("^/+", "");
                    if (sub.isEmpty()) sub = "FactiaSafe";
                    File dir = new File(base, sub);
                    if (!dir.exists()) dir.mkdirs();
                    File outFile = new File(dir, filename);
                    try (FileOutputStream fos = new FileOutputStream(outFile)) { pdf.writeTo(fos); fos.flush(); }
                    return outFile.getAbsolutePath();
                } else {
                    base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    String sub = s;
                    File dir = new File(base, sub);
                    if (!dir.exists()) dir.mkdirs();
                    File outFile = new File(dir, filename);
                    try (FileOutputStream fos = new FileOutputStream(outFile)) { pdf.writeTo(fos); fos.flush(); }
                    return outFile.getAbsolutePath();
                }
            } else {
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File dir = new File(downloads, "FactiaSafe");
                if (!dir.exists()) dir.mkdirs();
                File outFile = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) { pdf.writeTo(fos); fos.flush(); }
                return outFile.getAbsolutePath();
            }
        }
    }

    /**
     * Lee un valor de la tabla settings. Devuelve null si no existe.
     */
    private String getSetting(SQLiteDatabase db, String key) {
        if (db == null || key == null) return null;
        Cursor cur = null;
        try {
            cur = db.rawQuery("SELECT value FROM settings WHERE [key] = ? LIMIT 1", new String[]{ key });
            if (cur != null && cur.moveToFirst()) {
                return cur.isNull(0) ? null : cur.getString(0);
            }
        } catch (Exception ignored) {}
        finally { if (cur != null) cur.close(); }
        return null;
    }


    /**
     * Dibuja texto con envoltura (wrap) en la anchura `maxWidth`. Devuelve el nuevo Y (abajo del texto).
     */
    private int drawTextWrapped(Canvas canvas, String text, int x, int y, int maxWidth, Paint paint, int lineSpacingPx) {
        if (text == null) return y;
        String[] paragraphs = text.split("\n");
        for (String para : paragraphs) {
            String remaining = para.trim();
            while (remaining.length() > 0) {
                int cut = paint.breakText(remaining, true, maxWidth, null);
                String line = remaining.substring(0, cut);
                // si el corte no es final y el siguiente char no es espacio, intentar retroceder hasta espacio
                if (cut < remaining.length()) {
                    int lastSpace = line.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        line = line.substring(0, lastSpace);
                        cut = lastSpace;
                    }
                }
                canvas.drawText(line, x, y, paint);
                y += lineSpacingPx;
                remaining = remaining.substring(Math.min(cut, remaining.length())).trim();
            }
            // párrafo: añadir espacio extra
            y += 2;
        }
        return y;
    }

    /**
     * Calcula el inSampleSize óptimo para cargar una imagen con el tamaño requerido.
     * Esto mejora la calidad al cargar imágenes optimizadas en lugar de versiones downsampled.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}