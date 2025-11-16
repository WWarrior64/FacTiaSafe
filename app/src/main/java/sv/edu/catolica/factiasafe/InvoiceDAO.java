package sv.edu.catolica.factiasafe;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class InvoiceDAO {
    private static final String TAG = "InvoiceDAO";
    private FaSafeDB dbHelper;
    private Context context;

    public InvoiceDAO(Context context) {
        this.context = context;
        this.dbHelper = new FaSafeDB(context);
    }

    /**
     * Inserta una factura en la tabla 'invoices', sus ítems en 'invoice_items' y garantía en 'warranties' si aplica.
     * Usa transacción para asegurar que todo se inserte o nada.
     * @param datos HashMap con los datos extraídos o del formulario.
     * @return ID de la invoice insertada, o -1 si falla.
     */
    public long insertInvoice(HashMap<String, Object> datos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long invoiceId = -1;
        db.beginTransaction();
        try {
            ContentValues invoiceValues = new ContentValues();

            // Company / external id / date
            String company = safeGetString(datos.get("empresa"));
            if (!company.isEmpty()) invoiceValues.put("company_name", company);

            String external = safeGetString(datos.get("factura"));
            if (!external.isEmpty()) invoiceValues.put("external_id", external);

            String fecha = safeGetString(datos.get("fecha"));
            if (!fecha.isEmpty()) invoiceValues.put("date", fecha); // asumes formato DD/MM/YYYY o YYYY-MM-DD según tu flujo

            // Items desde datos (pueden venir como ArrayList<HashMap<String,String>>)
            ArrayList<HashMap<String, String>> items = (ArrayList<HashMap<String, String>>) datos.get("items");

            // Subtotal: si viene en datos, úsalo; si no, calcular desde items
            double subtotal = safeGetDouble(datos.get("subtotal"));
            if ((subtotal == 0.0) && items != null && !items.isEmpty()) {
                double calc = 0.0;
                for (HashMap<String, String> it : items) {
                    double q = safeGetDouble(it.get("cantidad"));
                    double p = safeGetDouble(it.get("precio"));
                    calc += q * p;
                }
                subtotal = calc;
            }
            invoiceValues.put("subtotal", subtotal);

            // Impuesto / descuento: leer de datos con seguridad
            int taxApplied = 0;
            if (datos.containsKey("impuesto_aplicado")) {
                Object o = datos.get("impuesto_aplicado");
                if (o instanceof Number) taxApplied = ((Number) o).intValue();
                else taxApplied = "1".equals(String.valueOf(o)) ? 1 : 0;
            }
            double taxPerc = safeGetDouble(datos.get("impuesto_porcentaje"));
            double taxAmt = safeGetDouble(datos.get("impuesto_cantidad"));

            double discPerc = safeGetDouble(datos.get("descuento_porcentaje"));
            double discAmt = safeGetDouble(datos.get("descuento_cantidad"));

            invoiceValues.put("tax_applied", taxApplied);
            invoiceValues.put("tax_percentage", taxPerc);
            invoiceValues.put("tax_amount", taxAmt);
            invoiceValues.put("discount_percentage", discPerc);
            invoiceValues.put("discount_amount", discAmt);

            // Total: si viene, úsalo; si no, calc = subtotal + taxAmt - discAmt
            double total = safeGetDouble(datos.get("total"));
            if (total == 0.0) {
                total = subtotal + taxAmt - discAmt;
            }
            invoiceValues.put("total", total);

            // has_warranty
            boolean hasWarranty = datos.containsKey("garantia_start") && datos.containsKey("garantia_end") &&
                    !safeGetString(datos.get("garantia_start")).isEmpty() && !safeGetString(datos.get("garantia_end")).isEmpty();
            invoiceValues.put("has_warranty", hasWarranty ? 1 : 0);

            // OCR / notas extras (opcional)
            if (datos.containsKey("notas_extras")) {
                String ocr = safeGetString(datos.get("notas_extras"));
                if (!ocr.isEmpty()) invoiceValues.put("ocr_text", ocr);
            }

            // Insert invoice
            invoiceId = db.insert("invoices", null, invoiceValues);
            if (invoiceId == -1) {
                throw new Exception("Error al insertar invoice");
            }

            // Insert items
            if (items != null) {
                for (HashMap<String, String> item : items) {
                    ContentValues itemValues = new ContentValues();
                    itemValues.put("invoice_id", invoiceId);

                    if (item.containsKey("producto")) {
                        itemValues.put("description", item.get("producto"));
                    } else {
                        itemValues.put("description", "");
                    }

                    itemValues.put("quantity", safeGetDouble(item.get("cantidad")));
                    itemValues.put("unit_price", safeGetDouble(item.get("precio")));
                    double lineTotal = safeGetDouble(item.get("cantidad")) * safeGetDouble(item.get("precio"));
                    itemValues.put("line_total", lineTotal);

                    long itemId = db.insert("invoice_items", null, itemValues);
                    if (itemId == -1) {
                        throw new Exception("Error al insertar item");
                    }
                }
            }

            // Guardar attachment (imagen escaneada) si existe
            String imagenPath = safeGetString(datos.get("imagen_escaneada_path"));
            if (!imagenPath.isEmpty()) {
                ContentValues attachmentValues = new ContentValues();
                attachmentValues.put("invoice_id", invoiceId);
                attachmentValues.put("type", "invoice_image");
                attachmentValues.put("path", imagenPath);
                attachmentValues.put("mime", "image/jpeg");
                long attachmentId = db.insert("attachments", null, attachmentValues);
                if (attachmentId == -1) {
                    Log.w(TAG, "Error al insertar attachment para imagen");
                }
            }

            // Insertar garantía si aplica (mantenemos tu lógica)
            if (hasWarranty) {
                ContentValues warrantyValues = new ContentValues();
                warrantyValues.put("invoice_id", invoiceId);
                warrantyValues.putNull("invoice_item_id");

                String productName = company;
                if (items != null && !items.isEmpty() && items.get(0).containsKey("producto")) {
                    productName = items.get(0).get("producto");
                }
                warrantyValues.put("product_name", productName);

                String startStr = safeGetString(datos.get("garantia_start"));
                String endStr = safeGetString(datos.get("garantia_end"));
                warrantyValues.put("warranty_start", startStr);
                warrantyValues.put("warranty_end", endStr);

                // Usar meses de garantía si viene en datos, sino calcular
                int warrantyMonths = 0;
                if (datos.containsKey("garantia_meses")) {
                    Object mesesObj = datos.get("garantia_meses");
                    if (mesesObj instanceof Number) {
                        warrantyMonths = ((Number) mesesObj).intValue();
                    } else {
                        try {
                            warrantyMonths = Integer.parseInt(String.valueOf(mesesObj));
                        } catch (NumberFormatException e) {
                            warrantyMonths = calcularMesesEntreFechas(startStr, endStr);
                        }
                    }
                } else {
                    warrantyMonths = calcularMesesEntreFechas(startStr, endStr);
                }
                warrantyValues.put("warranty_months", warrantyMonths);

                int reminderDays = 7;
                warrantyValues.put("reminder_days_before", reminderDays);

                String notifyAt = calcularNotifyAt(endStr, reminderDays);
                if (notifyAt != null) {
                    warrantyValues.put("notify_at", notifyAt);
                } else {
                    warrantyValues.putNull("notify_at");
                }

                warrantyValues.put("status", "active");
                String notes = datos.containsKey("garantia_notes") ? safeGetString(datos.get("garantia_notes")) : "";
                warrantyValues.put("notes", notes);

                long warrantyId = db.insert("warranties", null, warrantyValues);
                if (warrantyId == -1) {
                    throw new Exception("Error al insertar warranty");
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Factura insertada con ID: " + invoiceId + (hasWarranty ? " (con garantía)" : ""));
            Toast.makeText(context, "Factura guardada exitosamente", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al insertar factura: " + e.getMessage(), e);
            Toast.makeText(context, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            invoiceId = -1;
        } finally {
            db.endTransaction();
        }
        return invoiceId;
    }

    private String safeGetString(Object o) {
        if (o == null) return "";
        return String.valueOf(o);
    }

    private double safeGetDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            String s = String.valueOf(o).trim().replaceAll("[^0-9,\\.\\-]", "");
            if (s.isEmpty()) return 0.0;
            // caso 1.234,56  OR 1,234.56 etc.
            if (s.contains(",") && s.contains(".")) {
                // si termina en ,dd asumimos coma decimal (p.ej "1.234,56")
                if (s.matches(".*,[0-9]{2}$")) {
                    s = s.replace(".", "").replace(',', '.');
                } else {
                    s = s.replace(",", "");
                }
            } else if (s.contains(",")) {
                s = s.replace(',', '.');
            }
            return Double.parseDouble(s);
        } catch (Exception ex) {
            return 0.0;
        }
    }



    // Método helper: Calcular meses aproximados entre dos fechas (YYYY-MM-DD)
    private int calcularMesesEntreFechas(String startStr, String endStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse(startStr);
            Date end = sdf.parse(endStr);
            Calendar calStart = Calendar.getInstance();
            calStart.setTime(start);
            Calendar calEnd = Calendar.getInstance();
            calEnd.setTime(end);
            int months = (calEnd.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)) * 12 +
                    (calEnd.get(Calendar.MONTH) - calStart.get(Calendar.MONTH));
            return months;
        } catch (ParseException e) {
            Log.e(TAG, "Error parseando fechas para meses: " + e.getMessage());
            return 0;
        }
    }

    // Método helper: Calcular notify_at (YYYY-MM-DD HH:mm:ss) restando días a end
    private String calcularNotifyAt(String endStr, int daysBefore) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date end = sdf.parse(endStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(end);
            cal.add(Calendar.DAY_OF_MONTH, -daysBefore);
            SimpleDateFormat sdfFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdfFull.format(cal.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Error calculando notify_at: " + e.getMessage());
            return null;
        }
    }

    // Puedes agregar más métodos, ej: updateInvoice, getInvoiceById, etc.
}