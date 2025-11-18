package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.button.MaterialButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

public class ImportPdfActivity extends AppCompatActivity {

    // Launcher para seleccionar archivos PDF
    private FaSafeDB dbHelper;
    private static final String SETTINGS_TABLE = "settings";
    private static final String KEY_PDF_PATH = "pdf_save_path";
    private ActivityResultLauncher<Intent> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_import_pdf);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        PDFBoxResourceLoader.init(getApplicationContext());
        dbHelper = new FaSafeDB(this);

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String nombreArchivo = obtenerNombreArchivo(uri);
                            Toast.makeText(this, "Procesando: " + nombreArchivo, Toast.LENGTH_LONG).show();

                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );

                            procesarPDF(uri); // ← AQUÍ LLAMAS AL PROCESAMIENTO
                        }
                    } else {
                        Toast.makeText(this, "Selección de PDF cancelada", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Habilitar el botón de retroceso
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        MaterialButton importPdfButton = findViewById(R.id.import_pdf_button_main);
        importPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí iría la lógica para abrir el selector de archivos
                Toast.makeText(ImportPdfActivity.this, "", Toast.LENGTH_SHORT).show();
                // Por ejemplo, iniciar una intención para seleccionar un PDF:
                // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                // intent.setType("application/pdf");
                // startActivityForResult(intent, PICK_PDF_REQUEST);
                abrirSelectorPdf();
            }
        });

        findViewById(R.id.upload_area_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ImportPdfActivity.this, "Clic en la zona de carga", Toast.LENGTH_SHORT).show();
                // Duplica la lógica del botón principal o la redirige
                abrirSelectorPdf();
            }
        });
    }

    private void abrirSelectorPdf() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");


        String pdfPathUriStr = getSetting(KEY_PDF_PATH);


        if (pdfPathUriStr != null && !pdfPathUriStr.isEmpty()) {
            try {
                Uri initialUri = Uri.parse(pdfPathUriStr);


                intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, initialUri);

            } catch (Exception e) {

            }
        }


        pdfPickerLauncher.launch(intent);
    }


    private String getSetting(String key) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {

            db = dbHelper.getReadableDatabase();
            c = db.rawQuery("SELECT value FROM " + SETTINGS_TABLE + " WHERE [key] = ?", new String[]{ key });
            if (c != null && c.moveToFirst()) {
                return c.isNull(0) ? null : c.getString(0);
            }
        } catch (Exception e) {

        } finally {
            try { if (c != null) c.close(); } catch (Exception ignored) {}
            try { if (db != null) db.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    private void procesarPDF(Uri pdfUri) {
        new Thread(() -> {
            try {
                String textoExtraido = extraerTextoDePDF(pdfUri);

                runOnUiThread(() -> {
                    if (textoExtraido != null && !textoExtraido.trim().isEmpty()) {
                        Intent intent = new Intent(ImportPdfActivity.this, TextoImportadoActivity.class);
                        intent.putExtra("TEXTO_PDF", textoExtraido);
                        intent.putExtra("PDF_URI", pdfUri.toString());
                        startActivity(intent);


                        finish();
                    } else {
                        Toast.makeText(ImportPdfActivity.this,
                                "No se pudo extraer texto del PDF", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ImportPdfActivity.this,
                            "Error al procesar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String extraerTextoDePDF(Uri pdfUri) {
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(pdfUri);
            PDFTextStripper pdfStripper = new PDFTextStripper();


            pdfStripper.setStartPage(1);
            pdfStripper.setEndPage(Integer.MAX_VALUE);
            pdfStripper.setSortByPosition(true);

            PDDocument document = PDDocument.load(inputStream);
            String texto = pdfStripper.getText(document);
            document.close();

            return texto.trim();

        } catch (Exception e) {

            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {}
        }
    }

    private String obtenerNombreArchivo(Uri uri) {
        try {
            DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
            if (documentFile != null && documentFile.getName() != null) {
                return documentFile.getName();
            }
        } catch (Exception e) {

            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    return path.substring(cut + 1);
                }
            }
        }
        return "documento.pdf";
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Simplemente vuelve a la actividad anterior
        return true;
    }
}