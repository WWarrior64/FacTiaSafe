package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CameraEscaneoActivity extends AppCompatActivity {

    private GmsDocumentScanner scanner;
    private static final String TAG = "CameraEscaneoActivity";

    private final ActivityResultLauncher<IntentSenderRequest> scannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        GmsDocumentScanningResult scanResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                        if (scanResult != null) {
                            handleScanResult(scanResult);
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.escaneo_cancelado, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_escaneo);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupScannerClient();
        startDocumentScanner();

    }

    private void setupScannerClient() {

        GmsDocumentScannerOptions options =
                new GmsDocumentScannerOptions.Builder()
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .setGalleryImportAllowed(true)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                        .setPageLimit(1)
                        .build();

        scanner = GmsDocumentScanning.getClient(options);
    }


    private void startDocumentScanner() {
        scanner.getStartScanIntent(this)
                .addOnSuccessListener(intentSender -> {
                    IntentSenderRequest request = new IntentSenderRequest.Builder(intentSender).build();
                    scannerLauncher.launch(request);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, getString(R.string.error_escaneo) + e.getMessage(), e);
                    Toast.makeText(this, getString(R.string.error2_escaneo) + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }



    private void handleScanResult(GmsDocumentScanningResult result) {
        List<GmsDocumentScanningResult.Page> pages = result.getPages();


        if (pages != null && !pages.isEmpty()) {

            GmsDocumentScanningResult.Page page = pages.get(0);
            Uri imageUri = page.getImageUri();

            // NO copiar aquí - solo pasar la URI temporal
            // Se guardará permanentemente solo si el usuario lo confirma en EscanearActivity

            ArrayList<String> imagePaths = new ArrayList<>();
            imagePaths.add(imageUri.toString());

            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra("photo_paths", imagePaths);
            setResult(RESULT_OK, resultIntent);


            Toast.makeText(this, R.string.escaneo_exito, Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(this, R.string.escaneo_fallo, Toast.LENGTH_LONG).show();
        }

        finish();
    }
    private String convertUriToFilePath(Uri uri) {
        try {
            String uriString = uri.toString();


            if (uriString.startsWith("file://")) {
                String filePath = uriString.substring(7);
                File file = new File(filePath);
                if (file.exists()) {
                    return filePath;
                } else {
                    return null;
                }
            }

            if (uriString.startsWith("content://")) {
                try {
                    String[] proj = { android.provider.MediaStore.Images.Media.DATA };
                    android.database.Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int column_index = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
                        String filePath = cursor.getString(column_index);
                        cursor.close();
                        if (filePath != null) {
                            return filePath;
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                }
            }

            return null;

        } catch (Exception e) {
            return null;
        }
    }



}