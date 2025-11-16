package sv.edu.catolica.factiasafe;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatosExtraFragment extends Fragment {

    private static final String TAG = "DatosExtraFragment";

    private TextInputLayout inputTiendaComercio, inputDatosExtras;
    private AutoCompleteTextView autoCompleteCategoria;
    private ImageView imageProductThumbnail;
    private int invoiceId = -1;

    // ruta local (almacenamiento interno) de la imagen seleccionada (si hay)
    private String productImagePath = "";

    // launcher para seleccionar imagen
    private ActivityResultLauncher<String[]> pickImageLauncher;

    private ArrayAdapter<String> categoriesAdapter;
    private List<String> categoriesList = new ArrayList<>();

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

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri == null) return;
                        try {
                            String savedPath = copyUriToInternalStorage(uri);
                            if (!TextUtils.isEmpty(savedPath)) {
                                productImagePath = savedPath;
                                loadProductImageIntoView(savedPath);
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
        return inflater.inflate(R.layout.fragment_datos_extra, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // después de hacer findViewById(...)
        inputTiendaComercio = view.findViewById(R.id.input_tienda_comercio);
        autoCompleteCategoria = view.findViewById(R.id.auto_complete_categoria);
        inputDatosExtras = view.findViewById(R.id.input_datos_extras);
        imageProductThumbnail = view.findViewById(R.id.image_product_thumbnail);

        // referencia al TextInputLayout contenedor (para controlar el end icon)
        final TextInputLayout inputCategoriaLayout = view.findViewById(R.id.input_categoria);

        // adapter ya configurado
        categoriesAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoriesList);
        autoCompleteCategoria.setAdapter(categoriesAdapter);
        autoCompleteCategoria.setThreshold(0); // mostrar sin teclear

        // Asegurar foco / comportamiento táctil
        autoCompleteCategoria.setFocusable(true);
        autoCompleteCategoria.setFocusableInTouchMode(true);
        autoCompleteCategoria.setClickable(true);

        // Mostrar dropdown al tocar (request focus para que funcione)
        autoCompleteCategoria.setOnTouchListener((v, event) -> {
            autoCompleteCategoria.requestFocus();
            autoCompleteCategoria.showDropDown();
            return false; // no consumir el evento
        });

        // También al hacer click (fallback)
        autoCompleteCategoria.setOnClickListener(v -> {
            autoCompleteCategoria.requestFocus();
            autoCompleteCategoria.showDropDown();
        });

        // Si quieres abrir automáticamente cuando categorias carguen:
        autoCompleteCategoria.post(() -> {
            // forzar ancho anclado al TextInputLayout que lo contiene
            View anchor = view.findViewById(R.id.input_categoria);
            if (anchor != null) {
                try {
                    autoCompleteCategoria.setDropDownAnchor(anchor.getId());
                    autoCompleteCategoria.setDropDownWidth(anchor.getWidth() > 0 ? anchor.getWidth() : autoCompleteCategoria.getWidth());
                } catch (Exception ignored) {}
            }
        });


        // listeners del selector de imagen (igual que antes)
        View card = view.findViewById(R.id.photo_upload_area_articulo);
        View overlay = view.findViewById(R.id.photo_placeholder_overlay);
        ImageView img = view.findViewById(R.id.image_product_thumbnail);
        View.OnClickListener launchPicker = v -> pickImageLauncher.launch(new String[]{"image/*"});
        if (card != null) card.setOnClickListener(launchPicker);
        if (overlay != null) overlay.setOnClickListener(launchPicker);
        if (img != null) img.setOnClickListener(launchPicker);
        if (imageProductThumbnail != null) {
            imageProductThumbnail.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        }

        // Cargamos categorías desde DB y después los datos de la factura
        loadCategories(); // llena categoriesList y actualiza adapter
        loadData();
    }

    /**
     * Carga lista de categorías desde la tabla 'categories' y actualiza el adapter del AutoCompleteTextView.
     */
// Reemplaza tu loadCategories() por esta versión de debug/robusta
    private void loadCategories() {
        final List<String> tmp = new ArrayList<>();

        FaSafeDB dbHelper = new FaSafeDB(requireContext());
        SQLiteDatabase db = null;
        Cursor c = null;
        Cursor ccnt = null;
        try {
            db = dbHelper.getReadableDatabase();

            // count (opcional, silencioso)
            try {
                ccnt = db.rawQuery("SELECT COUNT(*) FROM categories", null);
                if (ccnt != null && ccnt.moveToFirst()) {
                    // no logging
                }
            } catch (Exception ignore) {
            } finally {
                if (ccnt != null) ccnt.close();
            }

            // select names (trim + dedupe preserving order)
            try {
                c = db.rawQuery(
                        "SELECT id, name, TRIM(name) AS nm, LENGTH(TRIM(COALESCE(name,''))) AS nm_len " +
                                "FROM categories ORDER BY name COLLATE NOCASE ASC", null);

                java.util.LinkedHashSet<String> uniq = new java.util.LinkedHashSet<>();
                while (c != null && c.moveToNext()) {
                    String trimmed = c.isNull(2) ? "" : c.getString(2);
                    if (!TextUtils.isEmpty(trimmed)) uniq.add(trimmed);
                }
                tmp.addAll(uniq);
            } catch (Exception ignore) {
            }

        } catch (Exception ignore) {
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }

        // actualizar adapter en UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (tmp.isEmpty()) {
                    // dejar adapter vacío si existe
                    if (categoriesAdapter != null) {
                        categoriesAdapter.clear();
                        categoriesAdapter.notifyDataSetChanged();
                    }
                    return;
                }

                // crear adapter nuevo y asignar
                ArrayAdapter<String> newAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, tmp);
                autoCompleteCategoria.setAdapter(newAdapter);
                categoriesAdapter = newAdapter;

                // ajustar ancho del dropdown si es posible
                View anchor = getView() != null ? getView().findViewById(R.id.input_categoria) : null;
                int w = (anchor != null) ? anchor.getWidth() : autoCompleteCategoria.getWidth();
                if (w > 0) autoCompleteCategoria.setDropDownWidth(w);

                // ---- listeners seguros (configúralos aquí para que no se reasignen continuamente) ----

                // Click -> pedir foco y mostrar dropdown (mostrar todas las opciones)
                autoCompleteCategoria.setOnClickListener(v -> {
                    autoCompleteCategoria.requestFocus();
                    if (categoriesAdapter != null) {
                        categoriesAdapter.getFilter().filter(null); // fuerza lista completa
                    }
                    if (!autoCompleteCategoria.isPopupShowing()) {
                        autoCompleteCategoria.showDropDown();
                    }
                });

                // Focus -> mostrar dropdown si hay elementos y no está ya abierto
                autoCompleteCategoria.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus && categoriesAdapter != null && categoriesAdapter.getCount() > 0) {
                        if (!autoCompleteCategoria.isPopupShowing()) {
                            autoCompleteCategoria.post(() -> {
                                try { autoCompleteCategoria.showDropDown(); } catch (Exception ignored) {}
                            });
                        }
                    }
                });

                // Item seleccionado -> cerrar, quitar foco y ocultar teclado
                autoCompleteCategoria.setOnItemClickListener((parent, view, position, id) -> {
                    autoCompleteCategoria.clearFocus();
                    try { autoCompleteCategoria.dismissDropDown(); } catch (Exception ignored) {}
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(autoCompleteCategoria.getWindowToken(), 0);
                });

                // End icon del TextInputLayout (abrir sin toggle)
                try {
                    TextInputLayout inputCategoriaLayout = getView() != null ? getView().findViewById(R.id.input_categoria) : null;
                    if (inputCategoriaLayout != null) {
                        inputCategoriaLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
                        inputCategoriaLayout.setEndIconOnClickListener(v -> {
                            if (categoriesAdapter != null) categoriesAdapter.getFilter().filter(null);
                            if (!autoCompleteCategoria.isPopupShowing()) autoCompleteCategoria.showDropDown();
                        });
                    }
                } catch (Exception ignored) {}
            });
        }
    }



    private void loadData() {
        if (invoiceId == -1) return;

        FaSafeDB dbHelper = new FaSafeDB(requireContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT store_id, category_id, notes, product_image_path FROM invoices WHERE id = ?",
                new String[]{String.valueOf(invoiceId)});
        if (cursor.moveToFirst()) {
            int storeId = cursor.isNull(0) ? -1 : cursor.getInt(0);
            int categoryId = cursor.isNull(1) ? -1 : cursor.getInt(1);
            String notes = cursor.isNull(2) ? "" : cursor.getString(2);
            String imgPath = cursor.isNull(3) ? "" : cursor.getString(3);

            if (inputTiendaComercio != null && inputTiendaComercio.getEditText() != null && storeId != -1) {
                String storeName = getStoreName(db, storeId);
                inputTiendaComercio.getEditText().setText(storeName);
            }

            if (autoCompleteCategoria != null && categoryId != -1) {
                String catName = getCategoryName(db, categoryId);
                // setText con filter = false evita lanzar el filtro inmediatamente
                autoCompleteCategoria.setText(catName, false);
            }


            if (inputDatosExtras != null && inputDatosExtras.getEditText() != null) {
                inputDatosExtras.getEditText().setText(notes);
            }

            if (!TextUtils.isEmpty(imgPath)) {
                productImagePath = imgPath;
                loadProductImageIntoView(imgPath);
            }
        }
        cursor.close();
        db.close();
    }

    private void loadProductImageIntoView(String path) {
        try {
            if (imageProductThumbnail == null) return;
            if (TextUtils.isEmpty(path)) {
                // si no hay path, mostrar overlay y limpiar imagen
                imageProductThumbnail.setVisibility(View.GONE);
                View overlay = getView() != null ? getView().findViewById(R.id.photo_placeholder_overlay) : null;
                if (overlay != null) overlay.setVisibility(View.VISIBLE);
                imageProductThumbnail.setImageDrawable(null);
                return;
            }

            File f = new File(path);
            if (f.exists()) {
                imageProductThumbnail.setImageURI(Uri.fromFile(f));
                imageProductThumbnail.setVisibility(View.VISIBLE);
                // ocultar placeholder
                View overlay = getView() != null ? getView().findViewById(R.id.photo_placeholder_overlay) : null;
                if (overlay != null) overlay.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadProductImageIntoView error", e);
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

            String filename = "product_image_" + invoiceId + ".jpg";
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

    // helpers para nombres <-> ids
    private String getStoreName(SQLiteDatabase db, int storeId) {
        Cursor c = db.rawQuery("SELECT name FROM stores WHERE id = ?", new String[]{String.valueOf(storeId)});
        String name = "";
        if (c.moveToFirst()) name = c.isNull(0) ? "" : c.getString(0);
        c.close();
        return name;
    }

    private String getCategoryName(SQLiteDatabase db, int categoryId) {
        Cursor c = db.rawQuery("SELECT name FROM categories WHERE id = ?", new String[]{String.valueOf(categoryId)});
        String name = "";
        if (c.moveToFirst()) name = c.isNull(0) ? "" : c.getString(0);
        c.close();
        return name;
    }

    private int getStoreIdByName(SQLiteDatabase db, String storeName) {
        if (TextUtils.isEmpty(storeName)) return -1;
        Cursor c = db.rawQuery("SELECT id FROM stores WHERE name = ?", new String[]{storeName});
        int id = -1;
        if (c.moveToFirst()) id = c.getInt(0);
        c.close();
        return id;
    }

    private int getCategoryIdByName(SQLiteDatabase db, String categoryName) {
        if (TextUtils.isEmpty(categoryName)) return -1;
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE name = ?", new String[]{categoryName});
        int id = -1;
        if (c.moveToFirst()) id = c.getInt(0);
        c.close();
        return id;
    }

    /**
     * Guarda/actualiza store_id, category_id, notes y product_image_path en la fila invoices.
     * Usa la misma SQLiteDatabase que la Activity (NO inicia/termina transacción).
     */
    public void saveIntoDatabase(SQLiteDatabase db) throws Exception {
        if (invoiceId == -1 || db == null) return;

        String storeName = getTienda();
        String categoryName = getCategoria();
        String notes = getNotas();

        int storeId = getStoreIdByName(db, storeName);
        int categoryId = getCategoryIdByName(db, categoryName);

        // Si quieres crear tiendas/categorías si no existen, descomenta y ajusta:
        /*
        if (storeId == -1 && !TextUtils.isEmpty(storeName)) {
            db.execSQL("INSERT INTO stores (name) VALUES (?)", new Object[]{storeName});
            storeId = (int) db.compileStatement("SELECT last_insert_rowid()").simpleQueryForLong();
        }
        if (categoryId == -1 && !TextUtils.isEmpty(categoryName)) {
            db.execSQL("INSERT INTO categories (name) VALUES (?)", new Object[]{categoryName});
            categoryId = (int) db.compileStatement("SELECT last_insert_rowid()").simpleQueryForLong();
        }
        */

        // Actualizar solo las columnas relacionadas
        db.execSQL("UPDATE invoices SET store_id = ?, category_id = ?, notes = ?, product_image_path = ? WHERE id = ?",
                new Object[]{ (storeId == -1) ? null : storeId, (categoryId == -1) ? null : categoryId, notes, TextUtils.isEmpty(productImagePath) ? null : productImagePath, invoiceId });
    }

    /**
     * Método público para guardar datos extra con un invoiceId específico.
     * Se usa en EntradaManualActivity cuando se crea una nueva factura.
     */
    public void saveIntoDatabaseWithId(SQLiteDatabase db, int newInvoiceId) throws Exception {
        if (newInvoiceId == -1 || db == null) return;

        String storeName = getTienda();
        String categoryName = getCategoria();
        String notes = getNotas();

        int storeId = getStoreIdByName(db, storeName);
        int categoryId = getCategoryIdByName(db, categoryName);

        // Actualizar solo las columnas relacionadas
        db.execSQL("UPDATE invoices SET store_id = ?, category_id = ?, notes = ?, product_image_path = ? WHERE id = ?",
                new Object[]{ (storeId == -1) ? null : storeId, (categoryId == -1) ? null : categoryId, notes, TextUtils.isEmpty(productImagePath) ? null : productImagePath, newInvoiceId });
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

    public String getProductImagePath() { return productImagePath; }
}
