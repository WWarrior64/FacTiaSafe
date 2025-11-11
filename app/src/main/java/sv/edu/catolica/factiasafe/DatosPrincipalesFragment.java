package sv.edu.catolica.factiasafe;

import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.text.TextWatcher;
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

import com.google.android.material.switchmaterial.SwitchMaterial;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
    private SwitchMaterial switchImpuesto;
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
    private int taxApplied = 0;

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

        switchImpuesto = view.findViewById(R.id.switch_impuesto);

        tabLayout = requireActivity().findViewById(R.id.tab_layout);

        // Photo area and thumbnail
        photoUploadArea = view.findViewById(R.id.photo_upload_area);
        imageThumbnail = view.findViewById(R.id.image_thumbnail);

        // Hacer cantidad_impuesto y cantidad_descuento read-only si existen
        if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null) {
            inputCantidadImpuesto.getEditText().setEnabled(false);
            inputCantidadImpuesto.getEditText().setFocusable(false);
        }
        if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null) {
            inputCantidadDescuento.getEditText().setEnabled(false);
            inputCantidadDescuento.getEditText().setFocusable(false);
        }

        // Primero cargar datos (para que cuando añadamos listeners no se dispare lógica con campos vacíos)
        loadData();

        // Luego listeners
        setupListeners();
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
            if (inputGarantiaStart != null) inputGarantiaStart.setEndIconOnClickListener(v -> showDatePicker(editGarantiaStart));
        }
        if (editGarantiaEnd != null) {
            editGarantiaEnd.setOnClickListener(v -> showDatePicker(editGarantiaEnd));
            if (inputGarantiaEnd != null) inputGarantiaEnd.setEndIconOnClickListener(v -> showDatePicker(editGarantiaEnd));
        }

        // Photo upload click -> open document chooser
        if (photoUploadArea != null) {
            photoUploadArea.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        }

        // Switch (protegido contra NPE)
        if (switchImpuesto != null) {
            switchImpuesto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    taxApplied = isChecked ? 1 : 0;
                    updateTaxFieldsState();
                    calculateTotal();
                } catch (Exception e) {
                    Log.e(TAG, "switchImpuesto listener error", e);
                }
            });
        }

        // TextWatcher para items (sólo si existe EditText)
        if (inputItems != null && inputItems.getEditText() != null) {
            inputItems.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        calculateSubtotalFromItems();
                    } catch (Exception e) {
                        Log.e(TAG, "calculateSubtotalFromItems error", e);
                    }
                }
            });
        }

        // Porcentaje impuesto watcher (sólo si campo existe)
        if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null) {
            inputPorcentajeImpuesto.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        taxPercentage = safeParse(s != null ? s.toString() : "");
                        if (taxApplied == 1) {
                            taxAmount = round2(subtotal * taxPercentage / 100.0);
                            if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                                inputCantidadImpuesto.getEditText().setText(String.format(Locale.getDefault(), "%.2f", taxAmount));
                        }
                        calculateTotal();
                    } catch (Exception e) {
                        Log.e(TAG, "tax % watcher error", e);
                    }
                }
            });
        }

        // Porcentaje descuento watcher
        if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null) {
            inputPorcentajeDescuento.getEditText().addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        discountPercentage = safeParse(s != null ? s.toString() : "");
                        discountAmount = round2(subtotal * discountPercentage / 100.0);
                        if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
                            inputCantidadDescuento.getEditText().setText(String.format(Locale.getDefault(), "%.2f", discountAmount));
                        calculateTotal();
                    } catch (Exception e) {
                        Log.e(TAG, "discount % watcher error", e);
                    }
                }
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

    private void updateTaxFieldsState() {
        try {
            if (inputPorcentajeImpuesto != null) inputPorcentajeImpuesto.setEnabled(taxApplied == 1);
            if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                inputCantidadImpuesto.getEditText().setEnabled(false); // always read-only
            if (taxApplied == 1) {
                if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                    inputPorcentajeImpuesto.getEditText().setText(String.valueOf(taxPercentage > 0.0 ? taxPercentage : ""));
                if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                    inputCantidadImpuesto.getEditText().setText(taxAmount > 0.0 ? String.format(Locale.getDefault(), "%.2f", taxAmount) : "");
            } else {
                taxPercentage = 0.0;
                taxAmount = 0.0;
                if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                    inputPorcentajeImpuesto.getEditText().setText("");
                if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                    inputCantidadImpuesto.getEditText().setText("");
            }
        } catch (Exception e) {
            Log.e(TAG, "updateTaxFieldsState error", e);
        }
    }

    private void showDatePicker(final EditText target) {
        final Calendar cal = Calendar.getInstance();
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

    private void calculateSubtotalFromItems() {
        subtotal = 0.0;
        if (inputItems == null || inputItems.getEditText() == null) {
            updateSubtotalUI();
            return;
        }
        String itemsText = inputItems.getEditText().getText() != null ? inputItems.getEditText().getText().toString() : "";
        if (TextUtils.isEmpty(itemsText)) {
            subtotal = 0.0;
            updateSubtotalUI();
            // recalc amounts
            if (taxApplied == 1) taxAmount = round2(subtotal * taxPercentage / 100.0);
            discountAmount = round2(subtotal * discountPercentage / 100.0);
            updateAmountsUI();
            calculateTotal();
            return;
        }

        String[] lines = itemsText.split("\\r?\\n");
        for (String line : lines) {
            if (TextUtils.isEmpty(line.trim())) continue;
            String[] parts = line.split("\\s*;\\s*"); // consistente con guardado
            if (parts.length >= 3) {
                double qty = safeParse(parts[1]);
                double price = safeParse(parts[2]);
                subtotal += qty * price;
            }
        }
        subtotal = round2(subtotal);
        updateSubtotalUI();

        // recalcular impuesto y descuento
        if (taxApplied == 1) {
            taxAmount = round2(subtotal * taxPercentage / 100.0);
        } else {
            taxAmount = 0.0;
        }
        discountAmount = round2(subtotal * discountPercentage / 100.0);

        updateAmountsUI();
        calculateTotal();
    }

    private void updateSubtotalUI() {
        if (textSubtotalValue == null) return;
        final String display = !TextUtils.isEmpty(currency) ? currency + " " + String.format(Locale.getDefault(), "%.2f", subtotal)
                : String.format(Locale.getDefault(), "%.2f", subtotal);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try { textSubtotalValue.setText(display); } catch (Exception ignored) {}
            });
        } else {
            try { textSubtotalValue.setText(display); } catch (Exception ignored) {}
        }
    }

    private void updateAmountsUI() {
        if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
            inputCantidadImpuesto.getEditText().setText(taxAmount > 0.0 ? String.format(Locale.getDefault(), "%.2f", taxAmount) : "");
        if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
            inputCantidadDescuento.getEditText().setText(discountAmount > 0.0 ? String.format(Locale.getDefault(), "%.2f", discountAmount) : "");
    }

    private void calculateTotal() {
        total = round2(subtotal + taxAmount - discountAmount);
        if (total < 0.0) total = 0.0;
        if (textTotalValue == null) return;
        final String display = !TextUtils.isEmpty(currency) ? currency + " " + String.format(Locale.getDefault(), "%.2f", total)
                : String.format(Locale.getDefault(), "%.2f", total);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try { textTotalValue.setText(display); } catch (Exception ignored) {}
            });
        } else {
            try { textTotalValue.setText(display); } catch (Exception ignored) {}
        }
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
            taxApplied = c.getColumnIndex("tax_applied") != -1 ? c.getInt(c.getColumnIndexOrThrow("tax_applied")) : 0;
            taxPercentage = safeGetDouble(c, "tax_percentage");
            taxAmount = safeGetDouble(c, "tax_amount");
            discountPercentage = safeGetDouble(c, "discount_percentage");
            discountAmount = safeGetDouble(c, "discount_amount");
            total = safeGetDouble(c, "total");
            currency = safeGetString(c, "currency");
            String notes = safeGetString(c, "notes");
            thumbnailPath = safeGetString(c, "thumbnail_path");

            // marcar switch pero no ejecutar listener durante setChecked (listener ya no registrado)
            if (switchImpuesto != null) switchImpuesto.setChecked(taxApplied == 1);
            updateTaxFieldsState();

            if (inputEmpresa != null && inputEmpresa.getEditText() != null)
                inputEmpresa.getEditText().setText(companyName);
            if (inputFactura != null && inputFactura.getEditText() != null)
                inputFactura.getEditText().setText(externalId);
            if (inputFecha != null && inputFecha.getEditText() != null)
                inputFecha.getEditText().setText(date);

            if (textSubtotalValue != null) textSubtotalValue.setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", subtotal) : String.format("%.2f", subtotal));
            if (textTotalValue != null) textTotalValue.setText(!TextUtils.isEmpty(currency) ? currency + " " + String.format("%.2f", total) : String.format("%.2f", total));

            if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                inputPorcentajeImpuesto.getEditText().setText(taxPercentage > 0.0 ? String.valueOf(taxPercentage) : "");
            if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                inputCantidadImpuesto.getEditText().setText(taxAmount > 0.0 ? String.format(Locale.getDefault(), "%.2f", taxAmount) : "");

            if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null)
                inputPorcentajeDescuento.getEditText().setText(discountPercentage > 0.0 ? String.valueOf(discountPercentage) : "");
            if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
                inputCantidadDescuento.getEditText().setText(discountAmount > 0.0 ? String.format(Locale.getDefault(), "%.2f", discountAmount) : "");

            if (inputItems != null && inputItems.getEditText() != null)
                inputItems.getEditText().setText(loadItemsText(db));

            // tabla de garantia
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

            // recalcular desde items por si hay inconsistencia
            calculateSubtotalFromItems();

        } catch (Exception e) {
            Log.e(TAG, "loadData error", e);
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
                qty = safeParse(parts[1]);
            }
            if (parts.length > 2) {
                unit = safeParse(parts[2]);
            }

            double lineTotal = round2(qty * unit);

            // Insert incluyendo line_total (tu tabla sí la tiene)
            db.execSQL(
                    "INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?)",
                    new Object[]{invoiceId, desc, qty, unit, lineTotal}
            );

            subtotalLocal += lineTotal;
        }

        return subtotalLocal;
    }

    private void saveThumbnailPathToDb(SQLiteDatabase db) {
        if (invoiceId == -1 || TextUtils.isEmpty(thumbnailPath) || db == null) return;
        db.execSQL("UPDATE invoices SET thumbnail_path = ? WHERE id = ?", new Object[]{thumbnailPath, invoiceId});
    }

    /**
     * Método público para persistir desde el fragment: items + thumbnail + warranty (si deseas).
     * La Activity puede llamarlo en su flujo de guardar. Aquí se abre/ cierra transacción.
     */

    // ----- getters públicos -----
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
    public double getTaxPercentage() { return taxPercentage; }
    public double getTaxAmount() { return taxAmount; }
    public double getDiscountPercentage() { return discountPercentage; }
    public double getDiscountAmount() { return discountAmount; }
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

    private double safeParse(String s) {
        if (TextUtils.isEmpty(s)) return 0.0;
        try {
            s = s.replace(currency, "").replace("$", "").replace(",", "").trim();
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void saveIntoDatabase(SQLiteDatabase db) throws Exception {
        if (invoiceId == -1 || db == null) return;

        // 1) Guardar items y obtener subtotal
        double newSubtotal = saveItemsToDb(db); // tu método existente
        newSubtotal = round2(newSubtotal);

        // 2) Guardar thumbnail_path si existe
        if (!TextUtils.isEmpty(thumbnailPath)) {
            saveThumbnailPathToDb(db); // tu método existente
        }

        // 3) Guardar/actualizar warranty
        String start = editGarantiaStart != null ? editGarantiaStart.getText().toString() : "";
        String end = editGarantiaEnd != null ? editGarantiaEnd.getText().toString() : "";
        Cursor checkWarranty = db.rawQuery("SELECT id FROM warranties WHERE invoice_id = ?", new String[]{String.valueOf(invoiceId)});
        try {
            int reminderDays = getReminderDaysDefault(db);
            String productName = getCompanyName();
            Integer invoiceItemId = null;

            ContentValues cv = new ContentValues();
            if (invoiceItemId != null) cv.put("invoice_item_id", invoiceItemId);
            else cv.putNull("invoice_item_id");
            cv.put("product_name", productName != null ? productName : "");
            cv.put("warranty_start", start);
            cv.put("warranty_end", end);
            cv.put("warranty_months", calculateMonthsBetween(start, end));
            cv.put("reminder_days_before", reminderDays);
            // REMOVIDO: cv.put("notify_at", ... ) — ya no necesario, múltiples notifs
            cv.put("status", "active");
            cv.put("notes", "");

            if (checkWarranty.moveToFirst()) {
                int warrantyId = checkWarranty.getInt(0);
                db.update("warranties", cv, "id = ?", new String[]{ String.valueOf(warrantyId) });
                // Eliminar notifs previas
                db.delete("notifications", "source_table = ? AND source_id = ?", new String[]{"warranties", String.valueOf(warrantyId)});
            } else {
                if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
                    long row = db.insert("warranties", null, cv);
                    // No agregar a toSchedule
                }
            }
        } finally {
            if (checkWarranty != null) checkWarranty.close();
        }


        // 4) Leer porcentajes / montos desde UI (uso safeParse para evitar NPE/format errors)
        double taxPctInput = 0.0;
        double taxAmtInput = 0.0;
        double discPctInput = 0.0;
        double discAmtInput = 0.0;

        if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
            taxPctInput = safeParse(inputPorcentajeImpuesto.getEditText().getText().toString());

        if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
            taxAmtInput = safeParse(inputCantidadImpuesto.getEditText().getText().toString());

        if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null)
            discPctInput = safeParse(inputPorcentajeDescuento.getEditText().getText().toString());
        else {
            discPctInput = 0.0;
        }

        if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
            discAmtInput = safeParse(inputCantidadDescuento.getEditText().getText().toString());

        // 5) Determinar valores finales: prioridad a porcentaje si > 0, si no usar monto explícito
        double taxAmountCalculated = 0.0;
        if (taxApplied == 1) {
            if (taxPctInput > 0.0) {
                taxAmountCalculated = round2(newSubtotal * taxPctInput / 100.0);
            } else {
                taxAmountCalculated = round2(taxAmtInput);
            }
        } else {
            taxPctInput = 0.0;
            taxAmountCalculated = 0.0;
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
        db.execSQL("UPDATE invoices SET subtotal = ?, tax_applied = ?, tax_percentage = ?, tax_amount = ?, discount_percentage = ?, discount_amount = ?, total = ? WHERE id = ?",
                new Object[]{newSubtotal, taxApplied, taxPctInput, taxAmountCalculated, discPctInput, discountAmountCalculated, newTotal, invoiceId});

        // 8) Actualizar campos locales y UI
        subtotal = newSubtotal;
        taxPercentage = taxPctInput;
        taxAmount = taxAmountCalculated;
        discountPercentage = discPctInput;
        discountAmount = discountAmountCalculated;
        total = newTotal;

        final String subtotalDisplay = !TextUtils.isEmpty(currency) ? String.format(Locale.getDefault(), "%s %.2f", currency, subtotal) : String.format(Locale.getDefault(), "%.2f", subtotal);
        final String totalDisplay = !TextUtils.isEmpty(currency) ? String.format(Locale.getDefault(), "%s %.2f", currency, total) : String.format(Locale.getDefault(), "%.2f", total);

        if (getActivity() != null) {
            double finalTaxAmountCalculated = taxAmountCalculated;
            double finalDiscountAmountCalculated = discountAmountCalculated;
            double finalTaxPctInput = taxPctInput;
            double finalDiscPctInput = discPctInput;
            getActivity().runOnUiThread(() -> {
                try {
                    if (textSubtotalValue != null) textSubtotalValue.setText(subtotalDisplay);
                    if (textTotalValue != null) textTotalValue.setText(totalDisplay);

                    // Mostrar porcentajes/montos en los campos correspondientes (vaciar si 0)
                    if (inputPorcentajeImpuesto != null && inputPorcentajeImpuesto.getEditText() != null)
                        inputPorcentajeImpuesto.getEditText().setText(finalTaxPctInput > 0.0 ? String.valueOf(finalTaxPctInput) : "");

                    if (inputCantidadImpuesto != null && inputCantidadImpuesto.getEditText() != null)
                        inputCantidadImpuesto.getEditText().setText(finalTaxAmountCalculated > 0.0 ? String.format(Locale.getDefault(), "%.2f", finalTaxAmountCalculated) : "");

                    if (inputPorcentajeDescuento != null && inputPorcentajeDescuento.getEditText() != null)
                        inputPorcentajeDescuento.getEditText().setText(finalDiscPctInput > 0.0 ? String.valueOf(finalDiscPctInput) : "");

                    if (inputCantidadDescuento != null && inputCantidadDescuento.getEditText() != null)
                        inputCantidadDescuento.getEditText().setText(finalDiscountAmountCalculated > 0.0 ? String.format(Locale.getDefault(), "%.2f", finalDiscountAmountCalculated) : "");
                } catch (Exception ignored) {}
            });
        } else {
            try {
                if (textSubtotalValue != null) textSubtotalValue.setText(subtotalDisplay);
                if (textTotalValue != null) textTotalValue.setText(totalDisplay);
            } catch (Exception ignored) {}
        }
    }

    private int getReminderDaysDefault(SQLiteDatabase db) {
        int def = 7;
        if (db == null) return def;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT value FROM settings WHERE [key] = ? LIMIT 1", new String[]{"notifications_lead_time_days"});  // CAMBIO: clave consistente
            if (c != null && c.moveToFirst()) {
                String v = c.isNull(0) ? "" : c.getString(0);
                try { def = Integer.parseInt(v); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) { }
        finally { if (c != null) c.close(); }
        return def;
    }

    private int calculateMonthsBetween(String startYYYYMMDD, String endYYYYMMDD) {
        if (TextUtils.isEmpty(startYYYYMMDD) || TextUtils.isEmpty(endYYYYMMDD)) return 0;
        try {
            java.util.Date s = sdf.parse(startYYYYMMDD);
            java.util.Date e = sdf.parse(endYYYYMMDD);
            if (s == null || e == null) return 0;
            Calendar cs = Calendar.getInstance(); cs.setTime(s);
            Calendar ce = Calendar.getInstance(); ce.setTime(e);
            int years = ce.get(Calendar.YEAR) - cs.get(Calendar.YEAR);
            int months = ce.get(Calendar.MONTH) - cs.get(Calendar.MONTH);
            int total = years * 12 + months;
            if (ce.get(Calendar.DAY_OF_MONTH) < cs.get(Calendar.DAY_OF_MONTH)) total--;
            return Math.max(0, total);
        } catch (Exception ex) {
            return 0;
        }
    }

    abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

}
