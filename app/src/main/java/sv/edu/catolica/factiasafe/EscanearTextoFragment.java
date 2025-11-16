package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;

public class EscanearTextoFragment extends Fragment {
    private Button buttonCancelar, buttonGuardar;
    private TextInputEditText editEmpresa, editFactura, editFecha, editItems, editImpuestoPorc, editImpuestoCant, editDescuentoPorc, editDescuentoCant, editGarantiaStart, editGarantiaEnd, editGarantiaMeses;
    private SwitchMaterial switchImpuesto;
    private android.widget.TextView textSubtotal, textTotal;
    private HashMap<String, Object> datosExtraidos;
    private InvoiceDAO invoiceDAO;
    private String imagenEscaneadaPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_escanear_texto, container, false);
        if (getArguments() != null) {
            datosExtraidos = (HashMap<String, Object>) getArguments().getSerializable("datos_extraidos");
            imagenEscaneadaPath = getArguments().getString("imagen_path");
        }
        invoiceDAO = new InvoiceDAO(getContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        setupListeners();
        if (datosExtraidos != null) {
            actualizarConDatos(datosExtraidos);
        }
    }

    private void setupListeners() {
        buttonCancelar.setOnClickListener(v -> requireActivity().onBackPressed());

        buttonGuardar.setOnClickListener(v -> {
            HashMap<String, Object> datosAGuardar = recopilarDatosDelFormulario();
            // Agregar imagen escaneada y meses de garantía
            datosAGuardar.put("imagen_escaneada_path", imagenEscaneadaPath);
            String mesesStr = editGarantiaMeses.getText().toString().trim();
            if (!mesesStr.isEmpty()) {
                try {
                    int meses = Integer.parseInt(mesesStr);
                    datosAGuardar.put("garantia_meses", meses);
                } catch (NumberFormatException e) {
                    datosAGuardar.put("garantia_meses", 0);
                }
            }
            long id = invoiceDAO.insertInvoice(datosAGuardar);
            if (id > 0) {
                Toast.makeText(getContext(), "Factura guardada con ID: " + id, Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        });

        editItems.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularTotales(); }
        });
        editImpuestoPorc.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularTotales(); }
        });
        editImpuestoCant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularTotales(); }
        });
        editDescuentoPorc.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularTotales(); }
        });
        editDescuentoCant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calcularTotales(); }
        });
        switchImpuesto.setOnCheckedChangeListener((buttonView, isChecked) -> calcularTotales());
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

        calcularTotales();
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
        double subtotal = parseDouble(textSubtotal.getText().toString().replace("$ ", ""));
        datos.put("subtotal", subtotal);
        double total = parseDouble(textTotal.getText().toString().replace("$ ", ""));
        datos.put("total", total);

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

    private void calcularTotales() {
        double subtotal = 0;
        String itemsStr = editItems.getText().toString();
        String[] lines = itemsStr.split("\n");
        for (String line : lines) {
            String[] parts = line.split(";");
            if (parts.length == 3) {
                try {
                    double cant = Double.parseDouble(parts[1].trim());
                    double precio = Double.parseDouble(parts[2].trim());
                    subtotal += cant * precio;
                } catch (NumberFormatException e) {}
            }
        }
        textSubtotal.setText("$ " + String.format("%.2f", subtotal));

        double tax = 0, disc = 0;
        if (switchImpuesto.isChecked()) {
            try {
                if (!editImpuestoPorc.getText().toString().isEmpty()) {
                    tax = subtotal * (Double.parseDouble(editImpuestoPorc.getText().toString()) / 100);
                } else if (!editImpuestoCant.getText().toString().isEmpty()) {
                    tax = Double.parseDouble(editImpuestoCant.getText().toString());
                }
            } catch (NumberFormatException e) {}
        }
        try {
            if (!editDescuentoPorc.getText().toString().isEmpty()) {
                disc = subtotal * (Double.parseDouble(editDescuentoPorc.getText().toString()) / 100);
            } else if (!editDescuentoCant.getText().toString().isEmpty()) {
                disc = Double.parseDouble(editDescuentoCant.getText().toString());
            }
        } catch (NumberFormatException e) {}

        double total = subtotal + tax - disc;
        textTotal.setText("$ " + String.format("%.2f", total));
    }
}