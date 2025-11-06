package sv.edu.catolica.factiasafe;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

public class DatosExtraFragment extends Fragment {

    private TextInputLayout inputTiendaComercio, inputDatosExtras;
    private AutoCompleteTextView autoCompleteCategoria;
    private int invoiceId = -1;

    public DatosExtraFragment() { }

    public static DatosExtraFragment newInstance(int invoiceId) {
        DatosExtraFragment f = new DatosExtraFragment();
        Bundle args = new Bundle();
        args.putInt("invoice_id", invoiceId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) invoiceId = getArguments().getInt("invoice_id", -1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_datos_extra, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputTiendaComercio = view.findViewById(R.id.input_tienda_comercio);
        // auto_complete_categoria existe en tu XML y tiene id
        autoCompleteCategoria = view.findViewById(R.id.auto_complete_categoria);
        inputDatosExtras = view.findViewById(R.id.input_datos_extras);

        loadData();
    }

    private void loadData() {
        if (invoiceId == -1) return;

        FaSafeDB dbHelper = new FaSafeDB(requireContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT store_id, category_id, notes FROM invoices WHERE id = ?",
                new String[]{String.valueOf(invoiceId)});
        if (cursor.moveToFirst()) {
            int storeId = cursor.isNull(0) ? -1 : cursor.getInt(0);
            int categoryId = cursor.isNull(1) ? -1 : cursor.getInt(1);
            String notes = cursor.getString(2);

            if (inputTiendaComercio != null && inputTiendaComercio.getEditText() != null && storeId != -1) {
                String storeName = getStoreName(db, storeId);
                inputTiendaComercio.getEditText().setText(storeName);
            }

            if (autoCompleteCategoria != null && categoryId != -1) {
                String catName = getCategoryName(db, categoryId);
                autoCompleteCategoria.setText(catName);
            }

            if (inputDatosExtras != null && inputDatosExtras.getEditText() != null) {
                inputDatosExtras.getEditText().setText(notes);
            }
        }
        cursor.close();
        db.close();
    }

    private String getStoreName(SQLiteDatabase db, int storeId) {
        Cursor c = db.rawQuery("SELECT name FROM stores WHERE id = ?", new String[]{String.valueOf(storeId)});
        String name = "";
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    private String getCategoryName(SQLiteDatabase db, int categoryId) {
        Cursor c = db.rawQuery("SELECT name FROM categories WHERE id = ?", new String[]{String.valueOf(categoryId)});
        String name = "";
        if (c.moveToFirst()) name = c.getString(0);
        c.close();
        return name;
    }

    // Getters públicos
    public String getTienda() {
        return inputTiendaComercio != null && inputTiendaComercio.getEditText() != null
                ? inputTiendaComercio.getEditText().getText().toString() : "";
    }

    public String getCategoria() {
        return autoCompleteCategoria != null ? autoCompleteCategoria.getText().toString() : "";
    }

    public String getNotas() {
        return inputDatosExtras != null && inputDatosExtras.getEditText() != null
                ? inputDatosExtras.getEditText().getText().toString() : "";
    }
}
