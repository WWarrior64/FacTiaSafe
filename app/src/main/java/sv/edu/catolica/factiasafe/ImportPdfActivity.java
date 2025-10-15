package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class ImportPdfActivity extends AppCompatActivity {

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
            }
        });

        findViewById(R.id.upload_area_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ImportPdfActivity.this, "Clic en la zona de carga", Toast.LENGTH_SHORT).show();
                // Duplica la lógica del botón principal o la redirige
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Simplemente vuelve a la actividad anterior
        return true;
    }
}