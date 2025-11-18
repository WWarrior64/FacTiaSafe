package sv.edu.catolica.factiasafe;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @noinspection NonAsciiCharacters, TryFinallyCanBeTryWithResources , CallToPrintStackTrace */
public class TextoImportadoActivity extends AppCompatActivity {

    private TextInputEditText editEmpresa, editFactura, editFecha, editTienda;
    private TextInputEditText editItems, editPorcentajeImpuesto, editCantidadImpuesto;
    private TextInputEditText editPorcentajeDescuento, editCantidadDescuento;
    private TextInputEditText editGarantiaStart, editGarantiaEnd, editGarantiaMeses;
    private TextInputEditText editDatosExtras;
    private MaterialAutoCompleteTextView autoCompleteCategoria;
    private TextView textSubtotalValue, textTotalValue;
    private Switch switchImpuesto;
    private ImageView imageProductThumbnail, imageFacturaThumbnail;
    private LinearLayout photoPlaceholderOverlay, facturaPlaceholderOverlay;
    private FaSafeDB dbHelper;
    private String textoPdfExtraido;
    private Uri pdfUri;
    private boolean cambioManualImpuesto = false;
    private boolean cambioManualDescuento = false;

    private Bitmap extractedBitmap = null;

    private double impuesto = 0.0;
    private Bitmap productBitmap;

    private TextWatcher textWatcherCantidadImpuesto;
    private TextWatcher textWatcherCantidadDescuento;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_texto_importado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarViews();
        configurarToolbar();
        obtenerDatosIntent();
        configurarEventos();
        configurarCategorias();
    }

    private void inicializarViews() {

        editEmpresa = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_empresa)).getEditText();
        editFactura = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_factura)).getEditText();
        editFecha = findViewById(R.id.edit_fecha);
        editTienda = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_tienda_comercio)).getEditText();


        autoCompleteCategoria = findViewById(R.id.auto_complete_categoria);


        editItems = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_items)).getEditText();


        textSubtotalValue = findViewById(R.id.text_subtotal_value);
        textTotalValue = findViewById(R.id.text_total_value);
        switchImpuesto = findViewById(R.id.switch_impuesto);
        editPorcentajeImpuesto = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_porcentaje_impuesto)).getEditText();
        editCantidadImpuesto = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_cantidad_impuesto)).getEditText();
        editPorcentajeDescuento = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_porcentaje_descuento)).getEditText();
        editCantidadDescuento = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_cantidad_descuento)).getEditText();


        editGarantiaStart = findViewById(R.id.edit_garantia_start);
        editGarantiaEnd = findViewById(R.id.edit_garantia_end);
        editGarantiaMeses = findViewById(R.id.edit_garantia_meses);


        editDatosExtras = (TextInputEditText) ((TextInputLayout) findViewById(R.id.input_datos_extras)).getEditText();


        imageFacturaThumbnail = findViewById(R.id.image_factura_thumbnail);
        facturaPlaceholderOverlay = findViewById(R.id.factura_placeholder_overlay);


        imageProductThumbnail = findViewById(R.id.image_product_thumbnail);
        photoPlaceholderOverlay = findViewById(R.id.photo_placeholder_overlay);

        photoPlaceholderOverlay.setOnClickListener(v -> {
            // Solo abrir selector de imagen si NO hay imagen de producto
            if (productBitmap == null) {
                abrirSelectorImagen();
            }
        });



        findViewById(R.id.button_guardar).setOnClickListener(v -> guardarFactura());
        findViewById(R.id.button_cancelar).setOnClickListener(v -> finish());

        dbHelper = new FaSafeDB(this);
    }

    private void abrirSelectorImagen() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");


        String[] mimeTypes = {"image/jpeg", "image/png", "image/webp"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        imagePickerLauncher.launch(intent);
    }



    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                cargarImagenDesdeUri(imageUri);
                            }
                        }
                    });

    private void cargarImagenDesdeUri(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {

                imageProductThumbnail.setImageBitmap(bitmap);
                imageProductThumbnail.setVisibility(View.VISIBLE);
                photoPlaceholderOverlay.setVisibility(View.GONE);


                productBitmap = bitmap;

                Toast.makeText(this, R.string.imagen_producto_agregada, Toast.LENGTH_SHORT).show();
            }

            if (inputStream != null) {
                inputStream.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_cargar_imagen, Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarToolbar() {
        ImageButton botonVolver = findViewById(R.id.boton_volver);
        botonVolver.setOnClickListener(v -> finish());
    }

    private void extraerYMostrarImagenesDePdf(Uri pdfUri) {
        if (pdfUri == null) {
            // No hay PDF, mostrar placeholders
            imageFacturaThumbnail.setVisibility(View.GONE);
            facturaPlaceholderOverlay.setVisibility(View.VISIBLE);
            imageProductThumbnail.setVisibility(View.GONE);
            photoPlaceholderOverlay.setVisibility(View.VISIBLE);
            return;
        }

        new Thread(() -> {
            PDDocument document = null;
            try {

                InputStream inputStream = getContentResolver().openInputStream(pdfUri);
                document = PDDocument.load(inputStream);


                List<Bitmap> todasLasImagenes = new ArrayList<>();


                for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                    PDPage page = document.getPage(pageNum);
                    PDResources resources = page.getResources();


                    for (COSName name : resources.getXObjectNames()) {
                        try {
                            if (resources.isImageXObject(name)) {
                                PDImageXObject image = (PDImageXObject) resources.getXObject(name);


                                Bitmap bitmap = image.getImage();

                                if (bitmap != null && bitmap.getWidth() > 100 && bitmap.getHeight() > 100) {

                                    todasLasImagenes.add(bitmap);
                                    Log.d("ExtraerImagen", "Imagen " + todasLasImagenes.size() +
                                            " encontrada: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                                }
                            }
                        } catch (Exception e) {
                            Log.e("ExtraerImagen", "Error extrayendo imagen individual: " + e.getMessage());
                        }
                    }
                }


                Bitmap imagenFactura = null;
                Bitmap imagenProducto = null;

                // Asignar según el orden de aparición
                if (!todasLasImagenes.isEmpty()) {
                    imagenFactura = todasLasImagenes.get(0); // Primera imagen = Factura
                }
                if (todasLasImagenes.size() >= 2) {
                    imagenProducto = todasLasImagenes.get(1); // Segunda imagen = Producto
                }


                Bitmap finalImagenFactura = imagenFactura;
                Bitmap finalImagenProducto = imagenProducto;

                runOnUiThread(() -> {
                    // Manejar imagen de factura
                    if (finalImagenFactura != null) {
                        imageFacturaThumbnail.setImageBitmap(finalImagenFactura);
                        imageFacturaThumbnail.setVisibility(View.VISIBLE);
                        facturaPlaceholderOverlay.setVisibility(View.GONE);
                        // Guardar referencia para uso futuro
                        extractedBitmap = finalImagenFactura;
                    } else {
                        imageFacturaThumbnail.setVisibility(View.GONE);
                        facturaPlaceholderOverlay.setVisibility(View.VISIBLE);
                    }

                    // Manejar imagen de producto
                    if (finalImagenProducto != null) {
                        imageProductThumbnail.setImageBitmap(finalImagenProducto);
                        imageProductThumbnail.setVisibility(View.VISIBLE);
                        photoPlaceholderOverlay.setVisibility(View.GONE);
                        // Guardar referencia para uso futuro
                        productBitmap = finalImagenProducto;
                    } else {
                        imageProductThumbnail.setVisibility(View.GONE);
                        photoPlaceholderOverlay.setVisibility(View.VISIBLE);
                        productBitmap = null;
                    }


                    if (todasLasImagenes.size() == 1) {
                        Toast.makeText(TextoImportadoActivity.this,
                                R.string.solo_imgfactura,
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    imageFacturaThumbnail.setVisibility(View.GONE);
                    facturaPlaceholderOverlay.setVisibility(View.VISIBLE);
                    imageProductThumbnail.setVisibility(View.GONE);
                    photoPlaceholderOverlay.setVisibility(View.VISIBLE);
                    Toast.makeText(TextoImportadoActivity.this,
                            R.string.error_imgpdf,
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                try {
                    if (document != null) {
                        document.close();
                    }
                } catch (IOException ignored) {

                }
            }
        }).start();
    }

    private void obtenerDatosIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            textoPdfExtraido = intent.getStringExtra("TEXTO_PDF");
            String uriString = intent.getStringExtra("PDF_URI");
            if (uriString != null) {
                pdfUri = Uri.parse(uriString);
            }

            if (textoPdfExtraido != null) {
                procesarYAutoRellenar(textoPdfExtraido);
            }

            extraerYMostrarImagenesDePdf(pdfUri);
        }
    }

    private String saveBitmapToFile(Bitmap bitmap, String companyName, Context context) {
        if (bitmap == null) return null;


        String fileName = companyName.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + System.currentTimeMillis() + ".jpeg";

        try {

            File filesDir = context.getFilesDir();
            File imageFile = new File(filesDir, fileName);

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();


            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void configurarEventos() {


        editGarantiaStart.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !TextUtils.isEmpty(editGarantiaStart.getText()) &&
                    !TextUtils.isEmpty(editGarantiaEnd.getText())) {
                calcularMesesGarantia();
            }
        });

        editGarantiaEnd.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !TextUtils.isEmpty(editGarantiaStart.getText()) &&
                    !TextUtils.isEmpty(editGarantiaEnd.getText())) {
                calcularMesesGarantia();
            }
        });


        editGarantiaMeses.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !TextUtils.isEmpty(editGarantiaStart.getText())) {
                calcularFechaFinDesdeMeses();
            }
        });


        editPorcentajeImpuesto.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cambioManualImpuesto = true;
                calcularMontoImpuestoDesdePorcentaje();
                calcularTotales();
            }
        });

        editCantidadImpuesto.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cambioManualImpuesto = true;
                calcularPorcentajeImpuestoDesdeMonto();
                calcularTotales();
            }
        });


        editPorcentajeDescuento.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cambioManualDescuento = true;
                calcularMontoDescuentoDesdePorcentaje();
                calcularTotales();
            }
        });

        editCantidadDescuento.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                cambioManualDescuento = true;
                calcularPorcentajeDescuentoDesdeMonto();
                calcularTotales();
            }
        });

        // También detectar cambios mientras se escribe
        editPorcentajeImpuesto.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                cambioManualImpuesto = true;
            }
            return false;
        });

        editCantidadImpuesto.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                cambioManualImpuesto = true;
            }
            return false;
        });

        editPorcentajeDescuento.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                cambioManualDescuento = true;
            }
            return false;
        });

        editCantidadDescuento.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                cambioManualDescuento = true;
            }
            return false;
        });


        editItems.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Cuando el usuario termina de editar los items
                calcularTotales();
            }
        });

        // También recalcular mientras se escribe
        editItems.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {

                editItems.postDelayed(() -> calcularTotales(), 500);
            }
        });


        configurarTextWatchers();

        switchImpuesto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                updateTaxFieldsState();
                calcularTotales();
            } catch (Exception e) {

                Toast.makeText(this, getString(R.string.error2_escaneo) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        configurarDatePicker();


    }

    private void configurarDatePicker() {

        editFecha.setOnClickListener(v -> mostrarDatePicker(editFecha));
        editFecha.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDatePicker(editFecha);
            }
        });


        editGarantiaStart.setOnClickListener(v -> mostrarDatePicker(editGarantiaStart));
        editGarantiaStart.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDatePicker(editGarantiaStart);
            }
        });


        editGarantiaEnd.setOnClickListener(v -> mostrarDatePicker(editGarantiaEnd));
        editGarantiaEnd.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mostrarDatePicker(editGarantiaEnd);
            }
        });
    }

    private void mostrarDatePicker(final TextInputEditText editText) {
        final Calendar calendario = Calendar.getInstance();
        int año = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);


        String fechaActual = Objects.requireNonNull(editText.getText()).toString();
        if (!TextUtils.isEmpty(fechaActual)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar fechaExistente = Calendar.getInstance();
                fechaExistente.setTime(Objects.requireNonNull(sdf.parse(fechaActual)));
                año = fechaExistente.get(Calendar.YEAR);
                mes = fechaExistente.get(Calendar.MONTH);
                dia = fechaExistente.get(Calendar.DAY_OF_MONTH);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Formatear la fecha como yyyy-MM-dd
                    String fechaSeleccionada = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    editText.setText(fechaSeleccionada);


                    if (editText == editGarantiaStart && !TextUtils.isEmpty(editGarantiaMeses.getText())) {
                        calcularFechaFinGarantia(selectedYear, selectedMonth, selectedDay);
                    }


                    if ((editText == editGarantiaStart || editText == editGarantiaEnd) &&
                            !TextUtils.isEmpty(editGarantiaStart.getText()) &&
                            !TextUtils.isEmpty(editGarantiaEnd.getText())) {
                        calcularMesesGarantia();
                    }
                },
                año, mes, dia
        );

        datePickerDialog.show();
    }

    private void calcularFechaFinGarantia(int startYear, int startMonth, int startDay) {
        try {
            String mesesStr = Objects.requireNonNull(editGarantiaMeses.getText()).toString();
            if (!TextUtils.isEmpty(mesesStr)) {
                int meses = Integer.parseInt(mesesStr);
                Calendar calendario = Calendar.getInstance();
                calendario.set(startYear, startMonth, startDay);
                calendario.add(Calendar.MONTH, meses);

                String fechaFin = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d",
                        calendario.get(Calendar.YEAR),
                        calendario.get(Calendar.MONTH) + 1,
                        calendario.get(Calendar.DAY_OF_MONTH));
                editGarantiaEnd.setText(fechaFin);
            }
        } catch (NumberFormatException ignored) {

        }
    }

    private void calcularFechaFinDesdeMeses() {
        try {
            String fechaInicioStr = Objects.requireNonNull(editGarantiaStart.getText()).toString();
            String mesesStr = Objects.requireNonNull(editGarantiaMeses.getText()).toString();

            if (!TextUtils.isEmpty(fechaInicioStr) && !TextUtils.isEmpty(mesesStr)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar calendario = Calendar.getInstance();
                calendario.setTime(Objects.requireNonNull(sdf.parse(fechaInicioStr)));

                int meses = Integer.parseInt(mesesStr);
                calendario.add(Calendar.MONTH, meses);

                String fechaFin = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d",
                        calendario.get(Calendar.YEAR),
                        calendario.get(Calendar.MONTH) + 1,
                        calendario.get(Calendar.DAY_OF_MONTH));
                editGarantiaEnd.setText(fechaFin);
            }
        } catch (Exception ignored) {

        }
    }

    private void calcularMesesGarantia() {
        try {
            String fechaInicioStr = Objects.requireNonNull(editGarantiaStart.getText()).toString();
            String fechaFinStr = Objects.requireNonNull(editGarantiaEnd.getText()).toString();

            if (!TextUtils.isEmpty(fechaInicioStr) && !TextUtils.isEmpty(fechaFinStr)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                Calendar inicio = Calendar.getInstance();
                inicio.setTime(Objects.requireNonNull(sdf.parse(fechaInicioStr)));

                Calendar fin = Calendar.getInstance();
                fin.setTime(Objects.requireNonNull(sdf.parse(fechaFinStr)));

                // Calcular diferencia en meses
                int meses = calcularDiferenciaMeses(inicio, fin);

                if (meses >= 0) {
                    editGarantiaMeses.setText(String.valueOf(meses));
                }
            }
        } catch (Exception ignored) {

        }
    }

    private int calcularDiferenciaMeses(Calendar inicio, Calendar fin) {
        int añosDiferencia = fin.get(Calendar.YEAR) - inicio.get(Calendar.YEAR);
        int mesesDiferencia = fin.get(Calendar.MONTH) - inicio.get(Calendar.MONTH);
        int diasDiferencia = fin.get(Calendar.DAY_OF_MONTH) - inicio.get(Calendar.DAY_OF_MONTH);

        int totalMeses = (añosDiferencia * 12) + mesesDiferencia;

        // Si el día del fin es menor que el día del inicio, restar un mes
        if (diasDiferencia < 0) {
            totalMeses--;
        }

        return totalMeses;
    }

    private void configurarCategorias() {

        List<String> categorias = obtenerCategoriasDesdeDB();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categorias);
        autoCompleteCategoria.setAdapter(adapter);
    }

    private List<String> obtenerCategoriasDesdeDB() {
        List<String> categorias = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT name FROM categories ORDER BY name", null);

            while (cursor.moveToNext()) {
                categorias.add(cursor.getString(0));
            }
        } catch (Exception e) {
            Log.e("TextoImportado", "Error obteniendo categorías", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }


        if (categorias.isEmpty()) {
            categorias.addAll(Arrays.asList("Compras Personales", "Electrónicos", "Hogar",
                    "Ropa", "Supermercado", "Salud", "Entretenimiento", "Otros"));
        }

        return categorias;
    }

    private void procesarYAutoRellenar(String textoPdf) {

        String empresa = extraerEmpresa(textoPdf);
        if (empresa != null) {
            editEmpresa.setText(empresa);
        }


        String numeroFactura = extraerNumeroFactura(textoPdf);
        if (numeroFactura != null) {
            editFactura.setText(numeroFactura);
        }


        String fecha = extraerFecha(textoPdf);
        if (fecha != null) {
            editFecha.setText(fecha);
        }


        String tienda = extraerTienda(textoPdf);
        if (tienda != null) {
            editTienda.setText(tienda);
        }


        String items = extraerItems(textoPdf);
        if (items != null) {
            editItems.setText(items);
        }


        extraerMontos(textoPdf);


        extraerGarantia(textoPdf);


        String notas = extraerNotas(textoPdf);
        if (notas != null) {
            editDatosExtras.setText(notas);
        }


        calcularTotales();
    }

    private String extraerEmpresa(String texto) {
        // Para la esquina superior izquierda, tomamos la primera línea no vacía
        String[] lineas = texto.split("\\n");
        for (String linea : lineas) {
            linea = linea.trim();
            if (!linea.isEmpty() &&
                    !linea.toLowerCase().contains("factura") &&
                    !linea.toLowerCase().contains("fecha") &&
                    !linea.toLowerCase().contains("tienda") &&
                    !linea.matches(".*\\d+.*")) {
                return linea;
            }
        }
        return null;
    }

    private String extraerNumeroFactura(String texto) {

        Pattern p = Pattern.compile("Factura:\\s*(\\w+\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            return Objects.requireNonNull(m.group(1)).trim();
        }
        return null;
    }

    private String extraerFecha(String texto) {

        Pattern p = Pattern.compile("Fecha:\\s*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            return Objects.requireNonNull(m.group(1)).trim();
        }
        return null;
    }

    private String extraerTienda(String texto) {

        Pattern p = Pattern.compile("Tienda:\\s*(.+?)(?=\\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            return Objects.requireNonNull(m.group(1)).trim();
        }
        return null;
    }

    private String extraerItems(String texto) {
        // Buscar sección de items basándose en patrones comunes
        StringBuilder itemsBuilder = new StringBuilder();

        // Patrón para líneas que parecen items (descripción cantidad precio)
        Pattern itemPattern = Pattern.compile("(.+?)\\s+(\\d+(?:\\.\\d{1,2})?)\\s+([$]?\\d+(?:\\.\\d{1,2})?)");
        String[] lineas = texto.split("\\n");

        boolean enSeccionItems = false;
        for (String linea : lineas) {
            linea = linea.trim();

            // Detectar inicio de sección de items
            if (linea.matches("(?i).*(descripción|cantidad|precio|items|productos).*")) {
                enSeccionItems = true;
                continue;
            }

            // Detectar fin de sección de items (totales)
            if (enSeccionItems && linea.matches("(?i).*(subtotal|total|impuesto|descuento).*")) {
                break;
            }

            if (enSeccionItems && !linea.isEmpty()) {
                Matcher m = itemPattern.matcher(linea);
                if (m.find()) {
                    String descripcion = Objects.requireNonNull(m.group(1)).trim();
                    String cantidad = Objects.requireNonNull(m.group(2)).trim();
                    String precio = Objects.requireNonNull(m.group(3)).trim().replace("$", "");

                    itemsBuilder.append(descripcion)
                            .append(" ; ")
                            .append(cantidad)
                            .append(" ; ")
                            .append(precio)
                            .append("\n");
                }
            }
        }

        return itemsBuilder.length() > 0 ? itemsBuilder.toString().trim() : null;
    }

    private void extraerMontos(String texto) {

        extraerImpuesto(texto);
        extraerDescuento(texto);


        Double total = extraerMontoEspecifico(texto, "Total:");

        calcularTotales();
    }

    private Double extraerMontoEspecifico(String texto, String etiqueta) {

        Pattern p = Pattern.compile(Pattern.quote(etiqueta) + "\\s*(?:USD\\s*|\\$)?\\s*(\\d+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            try {
                return Double.parseDouble(Objects.requireNonNull(m.group(1)));
            } catch (NumberFormatException ignored) {

            }
        }
        return null;
    }

    @SuppressLint("DefaultLocale")
    private void extraerImpuesto(String texto) {
        if (cambioManualImpuesto) return;


        Pattern pPorcentaje = Pattern.compile("Impuesto\\s*\\(\\s*(\\d+(?:\\.\\d{1,2})?)%\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher mPorcentaje = pPorcentaje.matcher(texto);
        if (mPorcentaje.find()) {
            try {
                double porcentaje = Double.parseDouble(Objects.requireNonNull(mPorcentaje.group(1)));
                editPorcentajeImpuesto.setText(String.format("%.2f", porcentaje));

                calcularMontoImpuestoDesdePorcentaje();

            } catch (NumberFormatException ignored) {

            }
        }


    }

    @SuppressLint("DefaultLocale")
    private void extraerDescuento(String texto) {
        if (cambioManualDescuento) return;


        Pattern pPorcentaje = Pattern.compile("Descuento\\s*\\(\\s*(\\d+(?:\\.\\d{1,2})?)%\\s*\\)", Pattern.CASE_INSENSITIVE);
        Matcher mPorcentaje = pPorcentaje.matcher(texto);
        if (mPorcentaje.find()) {
            try {
                double porcentaje = Double.parseDouble(Objects.requireNonNull(mPorcentaje.group(1)));
                editPorcentajeDescuento.setText(String.format("%.2f", porcentaje));


                calcularMontoDescuentoDesdePorcentaje();

            } catch (NumberFormatException ignored) {

            }
        }

    }

    @SuppressLint("DefaultLocale")
    private void calcularMontoImpuestoDesdePorcentaje() {
        try {

            String porcentajeStr = Objects.requireNonNull(editPorcentajeImpuesto.getText()).toString().trim();
            if (porcentajeStr.isEmpty()) {
                editCantidadImpuesto.setText("");
                return;
            }


            double subtotal = calcularSubtotalDesdeItems();

            double porcentaje = Double.parseDouble(porcentajeStr);
            double monto = subtotal * (porcentaje / 100.0);


            editCantidadImpuesto.removeTextChangedListener(textWatcherCantidadImpuesto);
            editCantidadImpuesto.setText(String.format("%.2f", monto));
            editCantidadImpuesto.addTextChangedListener(textWatcherCantidadImpuesto);



        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("DefaultLocale")
    private void calcularMontoDescuentoDesdePorcentaje() {
        try {
            String porcentajeStr = Objects.requireNonNull(editPorcentajeDescuento.getText()).toString().trim();
            if (porcentajeStr.isEmpty()) {
                editCantidadDescuento.setText("");
                return;
            }


            double subtotal = calcularSubtotalDesdeItems();

            double porcentaje = Double.parseDouble(porcentajeStr);
            double monto = subtotal * (porcentaje / 100.0);


            editCantidadDescuento.removeTextChangedListener(textWatcherCantidadDescuento);
            editCantidadDescuento.setText(String.format("%.2f", monto));
            editCantidadDescuento.addTextChangedListener(textWatcherCantidadDescuento);



        } catch (Exception ignored) {

        }
    }

    @SuppressLint("DefaultLocale")
    private void calcularPorcentajeImpuestoDesdeMonto() {
        try {

            double subtotal = calcularSubtotalDesdeItems();

            String montoStr = Objects.requireNonNull(editCantidadImpuesto.getText()).toString();
            if (montoStr.isEmpty()) return;

            double monto = Double.parseDouble(montoStr);

            if (subtotal > 0) {
                double porcentaje = (monto / subtotal) * 100.0;
                editPorcentajeImpuesto.setText(String.format("%.2f", porcentaje));
            }
        } catch (Exception ignored) {

        }
    }

    @SuppressLint("DefaultLocale")
    private void calcularPorcentajeDescuentoDesdeMonto() {
        try {

            double subtotal = calcularSubtotalDesdeItems();

            String montoStr = Objects.requireNonNull(editCantidadDescuento.getText()).toString();
            if (montoStr.isEmpty()) return;

            double monto = Double.parseDouble(montoStr);

            if (subtotal > 0) {
                double porcentaje = (monto / subtotal) * 100.0;
                editPorcentajeDescuento.setText(String.format("%.2f", porcentaje));
            }
        } catch (Exception ignored) {

        }
    }

    private void configurarTextWatchers() {
        // TextWatcher para el porcentaje de impuesto
        editPorcentajeImpuesto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (switchImpuesto.isChecked()) {
                    calcularMontoImpuestoDesdePorcentaje();
                    calcularTotales();
                }
            }
        });

        // TextWatcher para el porcentaje de descuento
        editPorcentajeDescuento.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calcularMontoDescuentoDesdePorcentaje();
                calcularTotales();
            }
        });
    }

    private void extraerGarantia(String texto) {
        // Extraer fecha de inicio
        Pattern pInicio = Pattern.compile("Inicio:\\s*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
        Matcher mInicio = pInicio.matcher(texto);
        if (mInicio.find()) {
            editGarantiaStart.setText(Objects.requireNonNull(mInicio.group(1)).trim());
        }

        // Extraer fecha de fin
        Pattern pFin = Pattern.compile("Fin:\\s*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
        Matcher mFin = pFin.matcher(texto);
        if (mFin.find()) {
            editGarantiaEnd.setText(Objects.requireNonNull(mFin.group(1)).trim());
        }

        // Extraer meses
        Pattern pMeses = Pattern.compile("Meses:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher mMeses = pMeses.matcher(texto);
        if (mMeses.find()) {
            editGarantiaMeses.setText(Objects.requireNonNull(mMeses.group(1)).trim());
        }
    }

    private String extraerNotas(String texto) {
        Pattern p = Pattern.compile("Notas:?\\s*(.+?)(?=\\s*Attachments|\\n\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            return Objects.requireNonNull(m.group(1)).trim();
        }
        return null;
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void calcularTotales() {
        try {

            double subtotal = calcularSubtotalDesdeItems();


            double impuesto = 0.0;
            double descuento = 0.0;


            if (switchImpuesto.isChecked()) {
                String impuestoText = Objects.requireNonNull(editCantidadImpuesto.getText()).toString().trim();

                if (!impuestoText.isEmpty()) {
                    try {
                        impuesto = Double.parseDouble(impuestoText);

                    } catch (NumberFormatException e) {
                        impuesto = 0.0;

                    }
                }
            } else {
                impuesto = 0.0;

            }

            // Calcular descuento
            String descuentoText = Objects.requireNonNull(editCantidadDescuento.getText()).toString().trim();
            Log.d("DEBUG", "Texto descuento: '" + descuentoText + "'");
            if (!descuentoText.isEmpty()) {
                try {
                    descuento = Double.parseDouble(descuentoText);

                } catch (NumberFormatException e) {
                    descuento = 0.0;

                }
            }

            double total = subtotal + impuesto - descuento;

            textSubtotalValue.setText(String.format("$ %.2f", subtotal));
            textTotalValue.setText(String.format("$ %.2f", total));



        } catch (Exception e) {

            textSubtotalValue.setText("$ 0.00");
            textTotalValue.setText("$ 0.00");

        }
    }

    private void updateTaxFieldsState() {
        try {
            boolean isTaxEnabled = switchImpuesto.isChecked();


            editPorcentajeImpuesto.setEnabled(isTaxEnabled);
            editCantidadImpuesto.setEnabled(false);

            if (!isTaxEnabled) {

                editPorcentajeImpuesto.setText("");
                editCantidadImpuesto.setText("");
            }


        } catch (Exception ignored) {

        }
    }

    private double calcularSubtotalDesdeItems() {
        try {
            String itemsText = Objects.requireNonNull(editItems.getText()).toString().trim();
            if (TextUtils.isEmpty(itemsText)) {
                return 0.0;
            }

            double subtotal = 0.0;
            String[] lineas = itemsText.split("\n");

            for (String linea : lineas) {

                String[] partes = linea.split(";");
                if (partes.length >= 3) {
                    try {
                        String descripcion = partes[0].trim();
                        String cantidadStr = partes[1].trim();
                        String precioStr = partes[2].trim().replace("$", "");


                        if (!cantidadStr.isEmpty() && !precioStr.isEmpty()) {
                            double cantidad = Double.parseDouble(cantidadStr);
                            double precio = Double.parseDouble(precioStr);

                            subtotal += cantidad * precio;
                        }
                    } catch (NumberFormatException ignored) {

                    }
                }
            }


            return subtotal;
        } catch (Exception e) {

            return 0.0;
        }
    }

    private void guardarFactura() {
        // Validar campos obligatorios básicos
        if (TextUtils.isEmpty(editEmpresa.getText())) {
            Toast.makeText(this, R.string.nombre_empresa_obligatorio, Toast.LENGTH_SHORT).show();
            editEmpresa.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(editFecha.getText())) {
            Toast.makeText(this, R.string.fecha_obligatoria, Toast.LENGTH_SHORT).show();
            editFecha.requestFocus();
            return;
        }


        // Validar garantía: si se llena un campo, deben llenarse todos
        String warrantyStart = editGarantiaStart != null && editGarantiaStart.getText() != null ?
                editGarantiaStart.getText().toString().trim() : "";
        String warrantyEnd = editGarantiaEnd != null && editGarantiaEnd.getText() != null ?
                editGarantiaEnd.getText().toString().trim() : "";
        String warrantyMonthsStr = editGarantiaMeses != null && editGarantiaMeses.getText() != null ?
                editGarantiaMeses.getText().toString().trim() : "";

        boolean hasAnyWarranty = !warrantyStart.isEmpty() || !warrantyEnd.isEmpty() || !warrantyMonthsStr.isEmpty();
        boolean hasCompleteWarranty = !warrantyStart.isEmpty() && !warrantyEnd.isEmpty();

        if (hasAnyWarranty && !hasCompleteWarranty) {
            Toast.makeText(this, R.string.instrucc_fechagarantia, Toast.LENGTH_LONG).show();
            if (warrantyStart.isEmpty()) {
                editGarantiaStart.requestFocus();
            } else {
                editGarantiaEnd.requestFocus();
            }
            return;
        }

        // Validar que la fecha de fin de garantía sea posterior a la de inicio
        if (hasCompleteWarranty) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date startDate = sdf.parse(warrantyStart);
                Date endDate = sdf.parse(warrantyEnd);

                if (startDate != null && endDate != null && endDate.before(startDate)) {
                    Toast.makeText(this, R.string.instrucc2_fechagarantia, Toast.LENGTH_LONG).show();
                    editGarantiaEnd.requestFocus();
                    return;
                }
            } catch (ParseException e) {
                Toast.makeText(this, R.string.formato_fechagarantia, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Recolectar datos de los campos actuales
        String companyName = editEmpresa.getText().toString().trim();
        String externalId = editFactura != null && editFactura.getText() != null ?
                editFactura.getText().toString().trim() : "";
        String date = editFecha.getText().toString().trim();
        String tienda = editTienda != null && editTienda.getText() != null ?
                editTienda.getText().toString().trim() : "";
        String categoria = autoCompleteCategoria != null ?
                autoCompleteCategoria.getText().toString().trim() : "";
        String notas = editDatosExtras != null && editDatosExtras.getText() != null ?
                editDatosExtras.getText().toString().trim() : "";

        // Obtener montos numéricos con manejo seguro
        double subtotal = obtenerSubtotalNumerico();
        double taxPct = 0.0;
        double taxAmount = 0.0;
        double discountPct = 0.0;
        double discountAmount = 0.0;
        double total = obtenerTotalNumerico();

        try {
            if (editPorcentajeImpuesto != null && editPorcentajeImpuesto.getText() != null) {
                taxPct = obtenerValorNumerico(editPorcentajeImpuesto.getText().toString());
            }
            if (editCantidadImpuesto != null && editCantidadImpuesto.getText() != null) {
                taxAmount = obtenerValorNumerico(editCantidadImpuesto.getText().toString());
            }
            if (editPorcentajeDescuento != null && editPorcentajeDescuento.getText() != null) {
                discountPct = obtenerValorNumerico(editPorcentajeDescuento.getText().toString());
            }
            if (editCantidadDescuento != null && editCantidadDescuento.getText() != null) {
                discountAmount = obtenerValorNumerico(editCantidadDescuento.getText().toString());
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_valoresnumericos, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que el total sea mayor a 0
        if (total <= 0 && subtotal <= 0) {
            Toast.makeText(this, R.string.montomayoracero, Toast.LENGTH_SHORT).show();
            return;
        }

        int warrantyMonths = 0;
        if (!warrantyMonthsStr.isEmpty()) {
            try {
                warrantyMonths = Integer.parseInt(warrantyMonthsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.meses_invalidos, Toast.LENGTH_SHORT).show();
                editGarantiaMeses.requestFocus();
                return;
            }
        }

        FaSafeDB dbHelper = new FaSafeDB(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean schedulingNeeded = false;

        db.beginTransaction();
        try {
            String facturaImagePath = null;
            String productImagePath = null;

            if (extractedBitmap != null) {
                facturaImagePath = saveBitmapToFile(extractedBitmap, companyName + "_factura", this);
                extractedBitmap.recycle();
                extractedBitmap = null;
            }

            if (productBitmap != null) {
                productImagePath = saveBitmapToFile(productBitmap, companyName + "_producto", this);
                productBitmap.recycle();
                productBitmap = null;
            }

            ContentValues invoiceValues = new ContentValues();
            invoiceValues.put("company_name", companyName);
            invoiceValues.put("external_id", externalId);
            invoiceValues.put("date", date);
            invoiceValues.put("subtotal", subtotal);
            invoiceValues.put("tax_percentage", taxPct);
            invoiceValues.put("tax_amount", taxAmount);
            invoiceValues.put("discount_percentage", discountPct);
            invoiceValues.put("discount_amount", discountAmount);
            invoiceValues.put("total", total);
            invoiceValues.put("currency", "USD");
            invoiceValues.put("notes", notas);

            if (facturaImagePath != null) {
                invoiceValues.put("thumbnail_path", facturaImagePath);
            } else if (pdfUri != null) {
                invoiceValues.put("thumbnail_path", pdfUri.toString());
            }

            if (productImagePath != null) {
                invoiceValues.put("product_image_path", productImagePath);
            }

            // Validar y guardar store y categoría solo si tienen valor
            if (!tienda.isEmpty()) {
                int storeId = obtenerOCrearStore(db, tienda);
                if (storeId > 0) {
                    invoiceValues.put("store_id", storeId);
                }
            }

            if (!categoria.isEmpty()) {
                int categoryId = obtenerOCrearCategoria(db, categoria);
                if (categoryId > 0) {
                    invoiceValues.put("category_id", categoryId);
                }
            }

            long invoiceId = db.insert("invoices", null, invoiceValues);
            if (invoiceId == -1) {
                throw new Exception("Error al insertar factura en la base de datos");
            }

            // Guardar items si existen
            guardarItemsEnBaseDeDatos(db, (int) invoiceId);

            // Guardar garantía solo si está completa
            if (hasCompleteWarranty) {
                saveWarranty(db, (int) invoiceId, companyName, warrantyStart, warrantyEnd);
                schedulingNeeded = true;
            }

            db.setTransactionSuccessful();
            Toast.makeText(this, R.string.factura_guardada_correctamente, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_al_guardar) + e.getMessage(), Toast.LENGTH_LONG).show();
            return;

        } finally {
            try { db.endTransaction(); } catch (Exception ignored) {}
            try { db.close(); } catch (Exception ignored) {}
            try { dbHelper.close(); } catch (Exception ignored) {}
        }

        if (schedulingNeeded) {
            new Thread(() -> {
                try {
                    NotificacionRescheduler.recreateAndScheduleAllWarrantyNotifications(getApplicationContext());
                } catch (Exception ignored) {
                    Log.e("GuardarFactura", "Error al programar notificaciones", ignored);
                }
            }).start();
        }

        finish();
    }
    private double obtenerSubtotalNumerico() {
        try {
            String subtotalText = textSubtotalValue.getText().toString()
                    .replace("$", "").replace(" ", "").trim();
            return subtotalText.isEmpty() ? 0.0 : Double.parseDouble(subtotalText);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double obtenerTotalNumerico() {
        try {
            String totalText = textTotalValue.getText().toString()
                    .replace("$", "").replace(" ", "").trim();
            return totalText.isEmpty() ? 0.0 : Double.parseDouble(totalText);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double obtenerValorNumerico(String texto) {
        try {
            return texto.isEmpty() ? 0.0 : Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void guardarItemsEnBaseDeDatos(SQLiteDatabase db, int invoiceId) {
        db.beginTransaction();
        try {
            String itemsText = Objects.requireNonNull(editItems.getText()).toString().trim();
            if (TextUtils.isEmpty(itemsText)) {
                db.endTransaction();
                return;
            }

            String[] lineas = itemsText.split("\n");

            for (String linea : lineas) {

                String[] partes = linea.split(";");
                if (partes.length >= 3) {
                    try {
                        String descripcion = partes[0].trim();
                        String cantidadStr = partes[1].trim();

                        String precioStr = partes[2].trim().replace("$", "").replace(",", "");

                        if (!descripcion.isEmpty() && !cantidadStr.isEmpty() && !precioStr.isEmpty()) {
                            double cantidad = Double.parseDouble(cantidadStr);
                            double precio = Double.parseDouble(precioStr);
                            double totalItem = cantidad * precio;

                            ContentValues itemValues = new ContentValues();
                            itemValues.put("invoice_id", invoiceId);
                            itemValues.put("description", descripcion);
                            itemValues.put("quantity", cantidad);
                            itemValues.put("unit_price", precio);

                            long idResultado = db.insert("invoice_items", null, itemValues);
                        }
                    } catch (NumberFormatException ignored) {

                    }
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception ignored) {

        } finally {
            db.endTransaction();
        }
    }

    private int obtenerOCrearStore(SQLiteDatabase db, String storeName) {
        if (TextUtils.isEmpty(storeName)) {
            return -1;
        }

        try {

            Cursor cursor = db.rawQuery("SELECT id FROM stores WHERE name = ?", new String[]{storeName});
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                cursor.close();
                return id;
            }
            cursor.close();


            ContentValues storeValues = new ContentValues();
            storeValues.put("name", storeName);
            long newStoreId = db.insert("stores", null, storeValues);
            return (int) newStoreId;

        } catch (Exception e) {

            return -1;
        }
    }

    private int obtenerOCrearCategoria(SQLiteDatabase db, String categoryName) {
        if (categoryName.isEmpty()) {
            return 0;
        }


        int categoryId = 0;
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("SELECT id FROM categories WHERE name = ?",
                    new String[]{categoryName});

            if (cursor.moveToFirst()) {
                categoryId = cursor.getInt(0);
            }
        } catch (Exception ignored) {

        } finally {
            if (cursor != null) cursor.close();
        }


        if (categoryId == 0) {
            ContentValues categoryValues = new ContentValues();
            categoryValues.put("name", categoryName);

            long newId = db.insert("categories", null, categoryValues);

            if (newId != -1) {
                categoryId = (int) newId;

            } else {

            }
        }

        return categoryId;
    }

    private void saveWarranty(SQLiteDatabase db, int invoiceId, String productName, String warrantyStart, String warrantyEnd) {
        try {
            ContentValues warrantyValues = new ContentValues();
            warrantyValues.put("invoice_id", invoiceId);
            warrantyValues.putNull("invoice_item_id");
            warrantyValues.put("product_name", productName != null ? productName : "");
            warrantyValues.put("warranty_start", warrantyStart);
            warrantyValues.put("warranty_end", warrantyEnd);
            warrantyValues.put("warranty_months", calculateMonthsBetween(warrantyStart, warrantyEnd));
            warrantyValues.put("reminder_days_before", 7);
            warrantyValues.put("status", "active");
            warrantyValues.put("notes", "");

            long result = db.insert("warranties", null, warrantyValues);
        } catch (Exception ignored) {

        }
    }
    private int calculateMonthsBetween(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            Calendar startCal = Calendar.getInstance();
            if (start != null) {
                startCal.setTime(start);
            }
            Calendar endCal = Calendar.getInstance();
            if (end != null) {
                endCal.setTime(end);
            }

            int yearDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
            int monthDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);

            return (yearDiff * 12) + monthDiff;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}