package sv.edu.catolica.factiasafe;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.tabs.TabLayout;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DatosPrincipalesFragment extends Fragment {

    private static final String TAG = "DatosPrincipalesFrag";

    private NestedScrollView nestedScrollView;
    private TextInputLayout inputEmpresa, inputFactura, inputFecha, inputItems,
            inputPorcentajeImpuesto, inputCantidadImpuesto,
            inputPorcentajeDescuento, inputCantidadDescuento;
    private TextInputLayout inputGarantiaStart, inputGarantiaEnd;
    private EditText editGarantiaStart, editGarantiaEnd;
    private TextView textSubtotalValue, textTotalValue;
    private TabLayout tabLayout;
    private ImageView imageThumbnail;
    private View photoUploadArea;

    private int invoiceId = -1;

    // valores leídos
    private double subtotal = 0.0;
    private double taxPercentage = 0.0;
    private double taxAmount = 0.0;
    private double discountPercentage = 0.0;
    private double discountAmount = 0.0;
    private double total = 0.0;
    private String currency = "";
    private String thumbnailPath = "";
    private String warrantyStart = "";
    private String warrantyEnd = "";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Launcher para seleccionar imagen desde dispositivo (OpenDocument)
    private ActivityResultLauncher<String[]> pickImageLauncher;

    public DatosPrincipalesFragment() { }

    public static DatosPrincipalesFragment newInstance(int invoiceId) {
        DatosPrincipalesFragment f = new DatosPrincipalesFragment();
        Bundle args = new Bundle();
        args.putInt("invoice_id", invoiceId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) invoiceId = getArguments().getInt("invoice_id", -1);

        // Registrar launcher en onCreate (antes de onViewCreated)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri == null) return;
                        try {
                            String savedPath = copyUriToInternalStorage(uri);
                            if (!TextUtils.isEmpty(savedPath)) {
                                thumbnailPath = savedPath;
                                loadThumbnailIntoView(savedPath);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error guardando imagen: " + e.getMessage(), e);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_datos_principales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nestedScrollView = view.findViewById(R.id.nestedScrollView);

        inputEmpresa = view.findViewById(R.id.input_empresa);
        inputFactura = view.findViewById(R.id.input_factura);
        inputFecha = view.findViewById(R.id.input_fecha);
        inputItems = view.findViewById(R.id.input_items);

        inputPorcentajeImpuesto = view.findViewById(R.id.input_porcentaje_impuesto);
        inputCantidadImpuesto = view.findViewById(R.id.input_cantidad_impuesto);
        inputPorcentajeDescuento = view.findViewById(R.id.input_porcentaje_descuento);
        inputCantidadDescuento = view.findViewById(R.id.input_cantidad_descuento);

        textSubtotalValue = view.findViewById(R.id.text_subtotal_value);
        textTotalValue = view.findViewById(R.id.text_total_value);

        inputGarantiaStart = view.findViewById(R.id.input_garantia_start);
        inputGarantiaEnd = view.findViewById(R.id.input_garantia_end);
        editGarantiaStart = view.findViewById(R.id.edit_garantia_start);
        editGarantiaEnd = view.findViewById(R.id.edit_garantia_end);

        tabLayout = requireActivity().findViewById(R.id.tab_layout);

        // Photo area and thumbnail
        photoUploadArea = view.findViewById(R.id.photo_upload_area);
        imageThumbnail = view.findViewById(R.id.image_thumbnail);

        setupListeners();
        loadData();
    }

    private void setupListeners() {
        // Fecha emisión click -> DatePicker
        if (inputFecha != null && inputFecha.getEditText() != null) {
            inputFecha.getEditText().setOnClickListener(v -> showDatePicker((EditText) inputFecha.getEditText()));
            inputFecha.setEndIconOnClickListener(v -> showDatePicker((EditText) inputFecha.getEditText()));
        }

        // Garantía start/end click -> DatePicker
        if (editGarantiaStart != null) {
            editGarantiaStart.setOnClickListener(v -> showDatePicker(editGarantiaStart));
            inputGarantiaStart.setEndIconOnClickListener(v -> showDatePicker(editGarantiaStart));
        }
        if (editGarantiaEnd != null) {
            editGarantiaEnd.setOnClickListener(v -> showDatePicker(editGarantiaEnd));
            inputGarantiaEnd.setEndIconOnClickListener(v -> showDatePicker(editGarantiaEnd));
        }

        // Photo upload click -> open document chooser
        if (photoUploadArea != null) {
            photoUploadArea.setOnClickListener(v -> {
                // Lanzar selector de imágenes: tipos permitidos
                pickImageLauncher.launch(new String[]{"image/*"});
            });
        }

        // reselección de pestaña -> scroll top
        if (tabLayout != null && nestedScrollView != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {}
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 0) nestedScrollView.smoothScrollTo(0, 0);
                }
            });
        }
    }

    private void showDatePicker(final EditText target) {
        final Calendar cal = Calendar.getInstance();
        // si ya tiene texto en formato yyyy-MM-dd, usarlo como valor inicial
        String current = target.getText() != null ? target.getText().toString() : "";
        if (!TextUtils.isEmpty(current)) {
            try {
                cal.setTime(sdf.parse(current));
            } catch (ParseException ignored) { }
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(requireContext(), (DatePicker view, int y, int m, int d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d);
            target.setText(sdf.format(sel.getTime()));
        }, year, month, day);
        dpd.show();
    }

    private void loadData() {
        if (invoiceId == -1) return;

        FaSafeDB dbHelper = new FaSafeDB(requireContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM invoices WHERE id = ?", new String[]{String.valueOf(invoiceId)});
        if (!c.moveToFirst()) {
            c.close();
            db.close();
            return;
        }

        try {
            String companyName = safeGetString(c, "company_name");
            String externalId = safeGetString(c, "external_id");
            String date = safeGetString(c, "date");

            subtotal = safeGetDouble(c, "subtotal");
            taxPercentage = safeGetDouble(c, "tax_percentage");
            taxAmount = safeGetDouble(c, "tax_amount");
            discountPercentage = safeGetDouble(c, "discount_percentage");
            discountAmount = safeGetDouble(c, "discount_amount");
            total = safeGetDouble(c, "total");
            currency = safeGetString(c, "currency");
            String notes = safeGetString(c, "notes");
            thumbnailPath = safeGetString(c, "thumbnail_path");

            if (inputEmpresa != null && inputEmpresa.getEditText() != null)
                inputEmpresa.getEditText().setText(companyName);
            if (inputFactura != null && inputFactura.getEditText() != null)
                inputFactura.getEditText().setText(externalId);
            if (inputFecha != null && inputFecha.getEditText() != null)
                inputFecha.getEditText().setText(date);

            if (textSubtotalValue != null) textSubtotalValue.setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", subtotal) : String.format("%.2f", subtotal));
            if (textTotalValue != null) textTotalValue.setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", total) : String.format("%.2f", total));

            if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                inputPorcentajeImpuesto.getEditText().setText(String.valueOf(taxPercentage));
            if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                inputCantidadImpuesto.getEditText().setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", taxAmount) : String.valueOf(taxAmount));

            if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null)
                inputPorcentajeDescuento.getEditText().setText(String.valueOf(discountPercentage));
            if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
                inputCantidadDescuento.getEditText().setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", discountAmount) : String.valueOf(discountAmount));

            if (inputItems != null && inputItems.getEditText() != null)
                inputItems.getEditText().setText(loadItemsText(db));

            // Warranty table
            Cursor w = db.rawQuery("SELECT warranty_start, warranty_end FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            if (w.moveToFirst()) {
                warrantyStart = w.isNull(0) ? "" : w.getString(0);
                warrantyEnd = w.isNull(1) ? "" : w.getString(1);
                if (editGarantiaStart != null) editGarantiaStart.setText(warrantyStart);
                if (editGarantiaEnd != null) editGarantiaEnd.setText(warrantyEnd);
            }
            if (w != null) w.close();

            // Cargar thumbnail si existe
            if (!TextUtils.isEmpty(thumbnailPath)) {
                loadThumbnailIntoView(thumbnailPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
            db.close();
        }
    }

    private void loadThumbnailIntoView(String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bm != null && imageThumbnail != null) {
                    imageThumbnail.setImageBitmap(bm);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadThumbnailIntoView error: " + e.getMessage(), e);
        }
    }

    /**
     * Copia el contenido de la Uri seleccionada en almacenamiento interno de la app y devuelve la ruta guardada
     */
    private String copyUriToInternalStorage(Uri uri) {
        if (uri == null) return "";
        try {
            ContentResolver cr = requireContext().getContentResolver();
            InputStream is = cr.openInputStream(uri);
            if (is == null) return "";

            File invoicesDir = new File(requireContext().getFilesDir(), "invoices");
            if (!invoicesDir.exists()) invoicesDir.mkdirs();

            String filename = "thumbnail_" + invoiceId + ".jpg";
            File outFile = new File(invoicesDir, filename);

            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            fos.close();
            is.close();
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "copyUriToInternalStorage error: " + e.getMessage(), e);
            return "";
        }
    }

    private String loadItemsText(SQLiteDatabase db) {
        StringBuilder sb = new StringBuilder();
        Cursor itemsCursor = db.rawQuery("SELECT description, quantity, unit_price FROM invoice_items WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        while (itemsCursor.moveToNext()) {
            String desc = itemsCursor.isNull(0) ? "" : itemsCursor.getString(0);
            double qty = itemsCursor.isNull(1) ? 0.0 : itemsCursor.getDouble(1);
            double price = itemsCursor.isNull(2) ? 0.0 : itemsCursor.getDouble(2);
            sb.append(desc).append(" ; ").append(qty).append(" ; ").append(price).append("\n");
        }
        itemsCursor.close();
        return sb.length() > 0 ? sb.toString().trim() : "";
    }

    /**
     * Guarda (reemplaza) los items de la factura en invoice_items y
     * devuelve el subtotal calculado (sum(quantity * unit_price)).
     * NOTA: NO maneja transacciones; el llamador debe abrir/ cerrar la transacción.
     */
    private double saveItemsToDb(SQLiteDatabase db) throws Exception {
        if (invoiceId == -1 || db == null) return 0.0;

        String itemsText = inputItems != null && inputItems.getEditText() != null
                ? inputItems.getEditText().getText().toString() : "";

        // Borrar items previos
        db.execSQL("DELETE FROM invoice_items WHERE invoice_id = ?", new Object[]{invoiceId});

        double subtotalLocal = 0.0;

        String[] lines = itemsText.split("\\r?\\n");
        for (String line : lines) {
            if (TextUtils.isEmpty(line.trim())) continue;
            // separar por ; (permitimos espacio alrededor)
            String[] parts = line.split("\\s*;\\s*");
            String desc = parts.length > 0 ? parts[0].trim() : "";
            double qty = 0.0;
            double unit = 0.0;
            if (parts.length > 1) {
                try { qty = Double.parseDouble(parts[1]); } catch (Exception ignored) {}
            }
            if (parts.length > 2) {
                try { unit = Double.parseDouble(parts[2]); } catch (Exception ignored) {}
            }

            // Insert
            db.execSQL("INSERT INTO invoice_items (invoice_id, description, quantity, unit_price) VALUES (?, ?, ?, ?)",
                    new Object[]{invoiceId, desc, qty, unit});

            // acumular subtotal
            subtotalLocal += qty * unit;
        }

        return subtotalLocal;
    }

    /**
     * Guarda thumbnail_path en la tabla invoices
     */
    private void saveThumbnailPathToDb(SQLiteDatabase db) {
        if (invoiceId == -1 || TextUtils.isEmpty(thumbnailPath) || db == null) return;
        db.execSQL("UPDATE invoices SET thumbnail_path = ? WHERE id = ?", new Object[]{thumbnailPath, invoiceId});
    }

    /**
     * Método público para persistir desde el fragment: items + thumbnail + warranty (si deseas).
     * La Activity puede llamarlo en su flujo de guardar. Si la Activity ya maneja la transacción
     * puedes abrir y pasar su SQLiteDatabase para evitar transacciones anidadas; aquí abrimos/cierramos por separado.
     */
    public void persistChanges() {
        FaSafeDB dbHelper = new FaSafeDB(requireContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // items
            saveItemsToDb(db);

            // thumbnail
            if (!TextUtils.isEmpty(thumbnailPath)) {
                saveThumbnailPathToDb(db);
            }

            // warranty
            String start = editGarantiaStart != null ? editGarantiaStart.getText().toString() : "";
            String end = editGarantiaEnd != null ? editGarantiaEnd.getText().toString() : "";
            Cursor checkWarranty = db.rawQuery("SELECT id FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
            if (checkWarranty.moveToFirst()) {
                db.execSQL("UPDATE warranties SET warranty_start = ?, warranty_end = ? WHERE invoice_id = ?", new Object[]{start, end, invoiceId});
            } else if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
                db.execSQL("INSERT INTO warranties (invoice_id, warranty_start, warranty_end) VALUES (?, ?, ?)", new Object[]{invoiceId, start, end});
            }
            checkWarranty.close();

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "persistChanges error: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // ----- getters públicos para que la Activity los use al guardar (los tienes ya) -----
    public String getCompanyName() {
        return inputEmpresa != null && inputEmpresa.getEditText() != null ? inputEmpresa.getEditText().getText().toString() : "";
    }
    public String getExternalId() {
        return inputFactura != null && inputFactura.getEditText() != null ? inputFactura.getEditText().getText().toString() : "";
    }
    public String getDate() {
        return inputFecha != null && inputFecha.getEditText() != null ? inputFecha.getEditText().getText().toString() : "";
    }
    public double getSubtotal() { return subtotal; }
    public double getTaxPercentage() {
        try { return inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null ?
                Double.parseDouble(inputPorcentajeImpuesto.getEditText().getText().toString()) : taxPercentage;
        } catch (Exception e) { return taxPercentage; }
    }
    public double getTaxAmount() {
        try {
            String s = inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null ? inputCantidadImpuesto.getEditText().getText().toString() : String.valueOf(taxAmount);
            s = s.replace(currency, "").replace("$", "").trim();
            return Double.parseDouble(s);
        } catch (Exception e) { return taxAmount; }
    }
    public double getDiscountPercentage() {
        try { return inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null ?
                Double.parseDouble(inputPorcentajeDescuento.getEditText().getText().toString()) : discountPercentage;
        } catch (Exception e) { return discountPercentage; }
    }
    public double getDiscountAmount() {
        try {
            String s = inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null ? inputCantidadDescuento.getEditText().getText().toString() : String.valueOf(discountAmount);
            s = s.replace(currency, "").replace("$", "").trim();
            return Double.parseDouble(s);
        } catch (Exception e) { return discountAmount; }
    }
    public double getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getItems() { return inputItems != null && inputItems.getEditText() != null ? inputItems.getEditText().getText().toString() : ""; }
    public String getWarrantyStart() { return editGarantiaStart != null ? editGarantiaStart.getText().toString() : warrantyStart; }
    public String getWarrantyEnd() { return editGarantiaEnd != null ? editGarantiaEnd.getText().toString() : warrantyEnd; }

    // helpers seguros
    private String safeGetString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx == -1) return "";
        return c.isNull(idx) ? "" : c.getString(idx);
    }
    private double safeGetDouble(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx == -1) return 0.0;
        return c.isNull(idx) ? 0.0 : c.getDouble(idx);
    }

    // helpers: parsear double de un TextInputLayout y formatear
    private double parseDoubleFromTextInputLayout(TextInputLayout til) {
        if (til == null || til.getEditText() == null) return 0.0;
        String s = til.getEditText().getText().toString();
        if (TextUtils.isEmpty(s)) return 0.0;
        // eliminar moneda y símbolos
        s = s.replace(currency, "").replace("$", "").replace(",", "").trim();
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatMoney(double value) {
        return String.format(Locale.getDefault(), "%s %.2f", TextUtils.isEmpty(currency) ? "" : currency, value).trim();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }


    /**
     * Guarda items, thumbnail_path y warranty usando la SQLiteDatabase proporcionada.
     * Además actualiza la columna invoices.subtotal con el subtotal calculado
     * (no recalcula impuestos/total aquí — si quieres, se pueden añadir).
     * No inicia/termina transacción: el llamador (Activity) debe hacerlo.
     */
    public void saveIntoDatabase(SQLiteDatabase db) throws Exception {
        if (invoiceId == -1 || db == null) return;

        // 1) Guardar items y obtener subtotal
        double newSubtotal = saveItemsToDb(db);
        newSubtotal = round2(newSubtotal);

        // 2) Guardar thumbnail_path (si existe)
        if (!TextUtils.isEmpty(thumbnailPath)) {
            db.execSQL("UPDATE invoices SET thumbnail_path = ? WHERE id = ?", new Object[]{thumbnailPath, invoiceId});
        }

        // 3) Guardar warranty (mantengo tu lógica)
        String start = editGarantiaStart != null ? editGarantiaStart.getText().toString() : "";
        String end = editGarantiaEnd != null ? editGarantiaEnd.getText().toString() : "";
        Cursor checkWarranty = db.rawQuery("SELECT id FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        if (checkWarranty.moveToFirst()) {
            db.execSQL("UPDATE warranties SET warranty_start = ?, warranty_end = ? WHERE invoice_id = ?", new Object[]{start, end, invoiceId});
        } else if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
            db.execSQL("INSERT INTO warranties (invoice_id, warranty_start, warranty_end) VALUES (?, ?, ?)",
                    new Object[]{invoiceId, start, end});
        }
        checkWarranty.close();

        // 4) Leer porcentajes / montos (UI)
        double taxPctInput = parseDoubleFromTextInputLayout(inputPorcentajeImpuesto);     // % preferencia
        double taxAmtInput = parseDoubleFromTextInputLayout(inputCantidadImpuesto);       // $ alternativa

        double discPctInput = parseDoubleFromTextInputLayout(inputPorcentajeDescuento);
        double discAmtInput = parseDoubleFromTextInputLayout(inputCantidadDescuento);

        // 5) Determinar valores finales: prioridad a porcentaje si > 0, si no usar monto explícito
        double taxAmountCalculated = 0.0;
        if (taxPctInput > 0.0) {
            taxAmountCalculated = round2(newSubtotal * taxPctInput / 100.0);
        } else {
            taxAmountCalculated = round2(taxAmtInput);
        }

        double discountAmountCalculated = 0.0;
        if (discPctInput > 0.0) {
            discountAmountCalculated = round2(newSubtotal * discPctInput / 100.0);
        } else {
            discountAmountCalculated = round2(discAmtInput);
        }

        // 6) Calcular total y asegurarse >= 0
        double newTotal = round2(newSubtotal + taxAmountCalculated - discountAmountCalculated);
        if (newTotal < 0.0) newTotal = 0.0;

        // 7) Actualizar la fila invoices con todos los campos calculados
        db.execSQL("UPDATE invoices SET subtotal = ?, tax_percentage = ?, tax_amount = ?, discount_percentage = ?, discount_amount = ?, total = ? WHERE id = ?",
                new Object[]{newSubtotal, taxPctInput, taxAmountCalculated, discPctInput, discountAmountCalculated, newTotal, invoiceId});

        // 8) Actualizar campos locales y UI
        subtotal = newSubtotal;
        taxPercentage = taxPctInput;
        taxAmount = taxAmountCalculated;
        discountPercentage = discPctInput;
        discountAmount = discountAmountCalculated;
        total = newTotal;

        // Actualizar UI en hilo principal
        final String subtotalDisplay = !TextUtils.isEmpty(currency) ? String.format(Locale.getDefault(), "%s %.2f", currency, subtotal) : String.format(Locale.getDefault(), "%.2f", subtotal);
        final String totalDisplay = !TextUtils.isEmpty(currency) ? String.format(Locale.getDefault(), "%s %.2f", currency, total) : String.format(Locale.getDefault(), "%.2f", total);

        if (getActivity() != null) {
            double finalTaxAmountCalculated = taxAmountCalculated;
            double finalDiscountAmountCalculated = discountAmountCalculated;
            getActivity().runOnUiThread(() -> {
                try {
                    if (textSubtotalValue != null) textSubtotalValue.setText(subtotalDisplay);
                    if (textTotalValue != null) textTotalValue.setText(totalDisplay);

                    // mostrar porcentajes/montos en los campos correspondientes
                    if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                        inputPorcentajeImpuesto.getEditText().setText(String.valueOf(taxPctInput > 0.0 ? taxPctInput : ""));

                    if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                        inputCantidadImpuesto.getEditText().setText(finalTaxAmountCalculated > 0.0 ? formatMoney(finalTaxAmountCalculated) : "");

                    if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null)
                        inputPorcentajeDescuento.getEditText().setText(String.valueOf(discPctInput > 0.0 ? discPctInput : ""));

                    if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
                        inputCantidadDescuento.getEditText().setText(finalDiscountAmountCalculated > 0.0 ? formatMoney(finalDiscountAmountCalculated) : "");

                } catch (Exception ignored) { }
            });
        } else {
            try {
                if (textSubtotalValue != null) textSubtotalValue.setText(subtotalDisplay);
                if (textTotalValue != null) textTotalValue.setText(totalDisplay);
            } catch (Exception ignored) {}
        }
    }
}
