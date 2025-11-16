package sv.edu.catolica.factiasafe;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.text.TextUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
public class EscanearTextoFragment extends Fragment {
    private static final String TAG = "EscanearTextoFragment";
    private Button buttonCancelar, buttonGuardar;
    private TextInputEditText editEmpresa, editFactura, editFecha, editItems, editImpuestoPorc, editImpuestoCant,
            editDescuentoPorc, editDescuentoCant, editGarantiaStart, editGarantiaEnd, editGarantiaMeses,
            editTienda, editDatosExtras;
    private SwitchMaterial switchImpuesto;
    private android.widget.TextView textSubtotal, textTotal;
    private MaterialAutoCompleteTextView autoCompleteCategoria;
    private ArrayList<Integer> categoryIds = new ArrayList<>();
    private int selectedCategoryId = -1;
    private HashMap<String, Object> datosExtraidos;
    private InvoiceDAO invoiceDAO;
    private String imagenEscaneadaPath;
    private String textoOcr; // OCR crudo desde ML Kit
    private String tiendaComercio = "";
    private String datosExtras = "";
    private int categoryId = -1;
    private String productImagePath = "";
    private ImageView imageProductThumbnail;
    private LinearLayout photoPlaceholderOverlay;
    // Variables para almacenar valores calculados
    private double subtotalCalculado = 0.0;
    private double totalCalculado = 0.0;
    private double taxAmountCalculado = 0.0;
    private double discountAmountCalculado = 0.0;


    // ✅ NUEVO: Launcher para seleccionar imagen del producto
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            // Copiar URI a almacenamiento interno persistente
                            String savedPath = copyUriToInternalStorage(uri);
                            if (!TextUtils.isEmpty(savedPath)) {
                                productImagePath = savedPath;
                                imageProductThumbnail.setImageURI(Uri.fromFile(new File(savedPath)));
                                imageProductThumbnail.setVisibility(View.VISIBLE);
                                photoPlaceholderOverlay.setVisibility(View.GONE);
                                Log.d(TAG, "Foto del producto guardada en: " + productImagePath);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error guardando imagen: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Error guardando imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_escanear_texto, container, false);
        if (getArguments() != null) {
            datosExtraidos = (HashMap<String, Object>) getArguments().getSerializable("datos_extraidos");
            imagenEscaneadaPath = getArguments().getString("imagen_path");
            textoOcr = getArguments().getString("ocr_text");
        }
        invoiceDAO = new InvoiceDAO(getContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views existentes
        editEmpresa = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_empresa)).getEditText();
        editFactura = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_factura)).getEditText();
        editFecha = view.findViewById(R.id.edit_fecha);
        editItems = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_items)).getEditText();
        editImpuestoPorc = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_porcentaje_impuesto)).getEditText();
        editImpuestoCant = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_cantidad_impuesto)).getEditText();
        editDescuentoPorc = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_porcentaje_descuento)).getEditText();
        editDescuentoCant = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_cantidad_descuento)).getEditText();
        editGarantiaStart = view.findViewById(R.id.edit_garantia_start);
        editGarantiaEnd = view.findViewById(R.id.edit_garantia_end);
        editGarantiaMeses = view.findViewById(R.id.edit_garantia_meses);
        switchImpuesto = view.findViewById(R.id.switch_impuesto);
        textSubtotal = view.findViewById(R.id.text_subtotal_value);
        textTotal = view.findViewById(R.id.text_total_value);
        buttonCancelar = view.findViewById(R.id.button_cancelar);
        buttonGuardar = view.findViewById(R.id.button_guardar);

        // ✅ NUEVOS: Views de Detalles integrados
        editTienda = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_tienda_comercio)).getEditText();
        editDatosExtras = (TextInputEditText) ((TextInputLayout) view.findViewById(R.id.input_datos_extras)).getEditText();
        autoCompleteCategoria = view.findViewById(R.id.auto_complete_categoria);
        imageProductThumbnail = view.findViewById(R.id.image_product_thumbnail);
        photoPlaceholderOverlay = view.findViewById(R.id.photo_placeholder_overlay);

        // Obtener los TextInputLayout para los iconos de calendario
        TextInputLayout inputGarantiaStart = view.findViewById(R.id.input_garantia_start);
        TextInputLayout inputGarantiaEnd = view.findViewById(R.id.input_garantia_end);
        TextInputLayout inputFecha = view.findViewById(R.id.input_fecha);

        // Hacer campos de cantidad y meses read-only
        editImpuestoCant.setEnabled(false);
        editImpuestoCant.setFocusable(false);
        editDescuentoCant.setEnabled(false);
        editDescuentoCant.setFocusable(false);
        editGarantiaMeses.setEnabled(false);
        editGarantiaMeses.setFocusable(false);

        // ✅ NUEVO: Cargar categorías en el spinner (lógica de DetallesActivity)
        cargarCategoriasEnSpinner();


        view.findViewById(R.id.photo_upload_area_articulo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        setupListeners();
        setupDatePickers(inputFecha, inputGarantiaStart, inputGarantiaEnd);

        // Recuperar datos del Bundle si vienen desde EscanearActivity
        if (getArguments() != null) {
            datosExtraidos = (HashMap<String, Object>) getArguments().getSerializable("datos_extraidos");
            imagenEscaneadaPath = getArguments().getString("imagen_path");
            textoOcr = getArguments().getString("ocr_text");
        }

        if (datosExtraidos != null) {
            actualizarConDatos(datosExtraidos);
        }

        // ✅ NUEVO: Setear valores adicionales si hay en argumentos
        if (tiendaComercio != null && !tiendaComercio.isEmpty()) {
            editTienda.setText(tiendaComercio);
        }
        if (datosExtras != null && !datosExtras.isEmpty()) {
            editDatosExtras.setText(datosExtras);
        }
        if (categoryId > 0) {
            selectedCategoryId = categoryId;
        }
    }

    private void cargarCategoriasEnSpinner() {
        FaSafeDB dbHelper = new FaSafeDB(getContext());
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT id, name FROM categories ORDER BY name", null);
        ArrayList<String> categorias = new ArrayList<>();
        categoryIds.clear();
        categorias.add("Sin categoría");
        categoryIds.add(-1);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            categoryIds.add(id);
            categorias.add(name);
        }
        cursor.close();
        db.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, categorias);
        autoCompleteCategoria.setAdapter(adapter);
        autoCompleteCategoria.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < categoryIds.size()) {
                selectedCategoryId = categoryIds.get(position);
            }
        });
    }

    private void setupListeners() {
        buttonCancelar.setOnClickListener(v -> requireActivity().onBackPressed());
        buttonGuardar.setOnClickListener(v -> guardarFactura());
        editItems.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calcularTotales();
            }
        });
        editImpuestoPorc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calcularTotales();
            }
        });
        editDescuentoPorc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calcularTotales();
            }
        });
        switchImpuesto.setOnCheckedChangeListener((buttonView, isChecked) -> calcularTotales());
// Listeners para las fechas de garantía para calcular meses
        editGarantiaStart.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calcularMesesGarantia();
            }
        });
        editGarantiaEnd.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                calcularMesesGarantia();
            }
        });
    }
    private void setupDatePickers(TextInputLayout inputFecha, TextInputLayout inputGarantiaStart, TextInputLayout inputGarantiaEnd) {
// DatePicker para fecha de emisión
        setupDatePickerIcon(inputFecha, editFecha);
// DatePicker para fecha inicio garantía
        setupDatePickerIcon(inputGarantiaStart, editGarantiaStart);
// DatePicker para fecha fin garantía
        setupDatePickerIcon(inputGarantiaEnd, editGarantiaEnd);
    }
    private void setupDatePickerIcon(TextInputLayout layout, TextInputEditText editText) {
        layout.setEndIconOnClickListener(v -> showDatePicker(editText));
    }
    private void showDatePicker(TextInputEditText editText) {
        final Calendar calendar = Calendar.getInstance();
// Intentar obtener la fecha actual del campo de texto
        String currentDate = editText.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                calendar.setTime(parsearFecha(currentDate));
            } catch (Exception e) {
// Si hay error, usar la fecha actual del sistema
            }
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Cambiado a formato estándar
                    editText.setText(sdf.format(selectedDate.getTime()));
                },
                year, month, day
        );
        datePickerDialog.show();
    }
    public void actualizarConDatos(HashMap<String, Object> datos) {
        this.datosExtraidos = datos;
        if (editEmpresa == null) return;
        editEmpresa.setText((String) datos.get("empresa"));
        editFactura.setText((String) datos.get("factura"));
        editFecha.setText((String) datos.get("fecha"));
        ArrayList<HashMap<String, String>> items = (ArrayList<HashMap<String, String>>) datos.get("items");
        StringBuilder itemsText = new StringBuilder();
        for (HashMap<String, String> item : items) {
            itemsText.append(item.get("producto")).append(" ; ").append(item.get("cantidad")).append(" ; ").append(item.get("precio")).append("\n");
        }
        editItems.setText(itemsText.toString());
        int aplicado = datos.containsKey("impuesto_aplicado") ? (Integer) datos.get("impuesto_aplicado") : 0;
        switchImpuesto.setChecked(aplicado == 1);
        editImpuestoPorc.setText((String) datos.get("impuesto_porcentaje"));
        editImpuestoCant.setText((String) datos.get("impuesto_cantidad"));
        editDescuentoPorc.setText((String) datos.get("descuento_porcentaje"));
        editDescuentoCant.setText((String) datos.get("descuento_cantidad"));
        // Garantía
        editGarantiaStart.setText((String) datos.get("garantia_start"));
        editGarantiaEnd.setText((String) datos.get("garantia_end"));
        // Meses de garantía
        if (datos.containsKey("garantia_meses")) {
            Object mesesObj = datos.get("garantia_meses");
            if (mesesObj instanceof Integer) {
                editGarantiaMeses.setText(String.valueOf(mesesObj));
            } else {
                editGarantiaMeses.setText(mesesObj != null ? mesesObj.toString() : "");
            }
        }
        // ✅ NUEVO: Setear defaults para nuevos fields desde OCR si no hay valores
        if (tiendaComercio.isEmpty() && datos.containsKey("empresa")) {
            tiendaComercio = (String) datos.get("empresa");
            editTienda.setText(tiendaComercio);
        }
        if (datosExtras.isEmpty() && datos.containsKey("notas_extras")) {
            datosExtras = (String) datos.get("notas_extras");
            editDatosExtras.setText(datosExtras);
        }

        calcularTotales();
        calcularMesesGarantia();
        // Manejar si porcentaje vacío pero cantidad no
        if (TextUtils.isEmpty(editImpuestoPorc.getText()) && !TextUtils.isEmpty(editImpuestoCant.getText()) && subtotalCalculado > 0 && switchImpuesto.isChecked()) {
            double cant = parseDouble(editImpuestoCant.getText().toString());
            double porc = (cant / subtotalCalculado) * 100;
            editImpuestoPorc.setText(String.format(Locale.getDefault(), "%.2f", porc));
            calcularTotales(); // Recalcular con nuevo porcentaje
        }
        if (TextUtils.isEmpty(editDescuentoPorc.getText()) && !TextUtils.isEmpty(editDescuentoCant.getText()) && subtotalCalculado > 0) {
            double cant = parseDouble(editDescuentoCant.getText().toString());
            double porc = (cant / subtotalCalculado) * 100;
            editDescuentoPorc.setText(String.format(Locale.getDefault(), "%.2f", porc));
            calcularTotales();
        }
    }
    private HashMap<String, Object> recopilarDatosDelFormulario() {
        HashMap<String, Object> datos = new HashMap<>();
        datos.put("empresa", editEmpresa.getText().toString());
        datos.put("factura", editFactura.getText().toString());
        datos.put("fecha", editFecha.getText().toString());
        datos.put("impuesto_porcentaje", editImpuestoPorc.getText().toString());
        datos.put("impuesto_cantidad", editImpuestoCant.getText().toString());
        datos.put("impuesto_aplicado", switchImpuesto.isChecked() ? 1 : 0);
        datos.put("descuento_porcentaje", editDescuentoPorc.getText().toString());
        datos.put("descuento_cantidad", editDescuentoCant.getText().toString());
        datos.put("subtotal", subtotalCalculado);
        datos.put("total", totalCalculado);
        datos.put("ocr_text", textoOcr);
        // ✅ NUEVOS: Incluir datos adicionales
        tiendaComercio = editTienda.getText().toString();
        datosExtras = editDatosExtras.getText().toString();
        categoryId = selectedCategoryId;
        datos.put("tienda_comercio", tiendaComercio);
        datos.put("datos_extras", datosExtras);
        datos.put("category_id", categoryId);
        datos.put("product_image_path", productImagePath);

        // Items
        ArrayList<HashMap<String, String>> items = new ArrayList<>();
        String itemsStr = editItems.getText().toString();
        String[] lines = itemsStr.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    HashMap<String, String> item = new HashMap<>();
                    item.put("producto", parts[0].trim());
                    item.put("cantidad", parts[1].trim());
                    item.put("precio", parts[2].trim());
                    items.add(item);
                }
            }
        }
        datos.put("items", items);
// Garantía
        datos.put("garantia_start", editGarantiaStart.getText().toString());
        datos.put("garantia_end", editGarantiaEnd.getText().toString());
        String mesesStr = editGarantiaMeses.getText().toString().trim();
        if (!mesesStr.isEmpty()) {
            try {
                datos.put("garantia_meses", Integer.parseInt(mesesStr));
            } catch (NumberFormatException e) {
                datos.put("garantia_meses", 0);
            }
        }
        return datos;
    }
    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    private void calcularMesesGarantia() {
        String fechaInicio = editGarantiaStart.getText().toString().trim();
        String fechaFin = editGarantiaEnd.getText().toString().trim();
        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            editGarantiaMeses.setText("");
            return;
        }
        try {
            Calendar inicio = Calendar.getInstance();
            Calendar fin = Calendar.getInstance();
            inicio.setTime(parsearFecha(fechaInicio));
            fin.setTime(parsearFecha(fechaFin));
            int years = fin.get(Calendar.YEAR) - inicio.get(Calendar.YEAR);
            int months = fin.get(Calendar.MONTH) - inicio.get(Calendar.MONTH);
            int totalMonths = years * 12 + months;
            if (fin.get(Calendar.DAY_OF_MONTH) < inicio.get(Calendar.DAY_OF_MONTH)) {
                totalMonths--;
            }
            totalMonths = Math.max(0, totalMonths);
            editGarantiaMeses.setText(String.valueOf(totalMonths));
        } catch (Exception e) {
            editGarantiaMeses.setText("");
        }
    }
    private java.util.Date parsearFecha(String fecha) throws Exception {
// Formatos soportados, priorizando yyyy-MM-dd
        String[] formatos = {
                "yyyy-MM-dd", // Prioridad para formato DB
                "dd/MM/yyyy",
                "dd-MM-yyyy"
        };
        for (String formato : formatos) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(formato, Locale.getDefault());
                return sdf.parse(fecha);
            } catch (Exception e) {
// Continuar
            }
        }
        throw new Exception("Formato de fecha no reconocido: " + fecha);
    }
    private void calcularTotales() {
        subtotalCalculado = 0;
        String itemsStr = editItems.getText().toString();
        String[] lines = itemsStr.split("\n");
        for (String line : lines) {
            String[] parts = line.split(";");
            if (parts.length == 3) {
                try {
                    double cant = Double.parseDouble(parts[1].trim());
                    double precio = Double.parseDouble(parts[2].trim());
                    subtotalCalculado += cant * precio;
                } catch (NumberFormatException e) {}
            }
        }
        subtotalCalculado = redondear2Decimales(subtotalCalculado);
        textSubtotal.setText("$ " + String.format("%.2f", subtotalCalculado));
        double tax = 0, disc = 0;
        if (switchImpuesto.isChecked()) {
            try {
                String porcStr = editImpuestoPorc.getText().toString();
                if (!TextUtils.isEmpty(porcStr) && Double.parseDouble(porcStr) > 0) {
                    tax = subtotalCalculado * (Double.parseDouble(porcStr) / 100);
                } else {
                    String cantStr = editImpuestoCant.getText().toString();
                    if (!TextUtils.isEmpty(cantStr)) {
                        tax = Double.parseDouble(cantStr);
                    }
                }
            } catch (NumberFormatException e) {}
        }
        try {
            String porcStr = editDescuentoPorc.getText().toString();
            if (!TextUtils.isEmpty(porcStr) && Double.parseDouble(porcStr) > 0) {
                disc = subtotalCalculado * (Double.parseDouble(porcStr) / 100);
            } else {
                String cantStr = editDescuentoCant.getText().toString();
                if (!TextUtils.isEmpty(cantStr)) {
                    disc = Double.parseDouble(cantStr);
                }
            }
        } catch (NumberFormatException e) {}
        taxAmountCalculado = redondear2Decimales(tax);
        discountAmountCalculado = redondear2Decimales(disc);
// Actualizar campos de cantidad
        editImpuestoCant.setText(String.format(Locale.getDefault(), "%.2f", taxAmountCalculado));
        editDescuentoCant.setText(String.format(Locale.getDefault(), "%.2f", discountAmountCalculado));
        totalCalculado = subtotalCalculado + taxAmountCalculado - discountAmountCalculado;
        if (totalCalculado < 0) totalCalculado = 0;
        textTotal.setText("$ " + String.format("%.2f", totalCalculado));
    }
    private double redondear2Decimales(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
    private void guardarFactura() {
        calcularTotales();
        calcularMesesGarantia();
        HashMap<String, Object> datos = recopilarDatosDelFormulario();
        datos.put("imagen_escaneada_path", imagenEscaneadaPath);
        // Logs para datos adicionales
        if (!tiendaComercio.isEmpty()) {
            Log.d(TAG, "Guardando con tienda_comercio: " + tiendaComercio);
        }
        if (!datosExtras.isEmpty()) {
            Log.d(TAG, "Guardando con datos_extras: " + datosExtras);
        }
        if (categoryId > 0) {
            Log.d(TAG, "Guardando con category_id: " + categoryId);
        }
        if (!productImagePath.isEmpty()) {
            Log.d(TAG, "Guardando con product_image_path: " + productImagePath);
        }
        Log.d(TAG, "Datos finales para guardar: " + datos.toString());
        long invoiceId = invoiceDAO.insertInvoice(datos);
        if (invoiceId > 0) {
            Toast.makeText(getContext(), "Factura guardada con ID: " + invoiceId, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Factura guardada exitosamente con ID: " + invoiceId);
            requireActivity().finish();
        } else {
            Toast.makeText(getContext(), "Error al insertar la factura", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error al insertar factura");
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

            // Usar timestamp para nombre único si invoiceId aún no existe
            String filename = "product_image_" + System.currentTimeMillis() + ".jpg";
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
}