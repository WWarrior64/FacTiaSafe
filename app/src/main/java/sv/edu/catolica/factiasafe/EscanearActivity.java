package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

public class EscanearActivity extends AppCompatActivity {

    private Button botonImagen, botonTexto;
    private View fragmentTextoContainer;
    private EscanearTextoFragment textoFragment;
    private final int FRAGMENT_CONTAINER_ID = R.id.contenedor_fragment;
    private String fragmentTagActual = "IMAGEN_TAG";
    private View bottomBar;
    private LinearLayout opcionEditar, opcionDetalles;
    private static final String TAG = "EscanearActivity";
    private String imagenEscaneadaPath = null;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private boolean camaraYaLanzada = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_escanear);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        if (savedInstanceState != null) {
            imagenEscaneadaPath = savedInstanceState.getString("IMAGEN_PATH");
            fragmentTagActual = savedInstanceState.getString("FRAGMENT_TAG", "IMAGEN_TAG");
            camaraYaLanzada = savedInstanceState.getBoolean("CAMARA_LANZADA", false);
        }

        configurarScannerLauncher();

        botonImagen = findViewById(R.id.boton_imagen);
        botonTexto = findViewById(R.id.boton_texto);
        fragmentTextoContainer = findViewById(R.id.fragment_texto_container);
        bottomBar = findViewById(R.id.barra_navegacion_inferior);

        opcionEditar = findViewById(R.id.opcion_editar);
        opcionDetalles = findViewById(R.id.opcion_detalles);
        opcionEditar.setOnClickListener(v -> abrirEditorImagen());
        opcionDetalles.setOnClickListener(v -> abrirDetalles());

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!camaraYaLanzada) {
            camaraYaLanzada = true;
            lanzarEscaner();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("FRAGMENT_TAG", fragmentTagActual);
        outState.putString("IMAGEN_PATH", imagenEscaneadaPath);
        outState.putBoolean("CAMARA_LANZADA", camaraYaLanzada);
    }

    private void configurarScannerLauncher() {
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    camaraYaLanzada = true;

                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();

                        if (data != null) {
                            ArrayList<String> photoPaths = data.getStringArrayListExtra("photo_paths");
                            if (photoPaths != null && !photoPaths.isEmpty()) {
                                String photoPath = photoPaths.get(0);
                                imagenEscaneadaPath = photoPath;
                                crearFragmentConImagen(photoPath);
                            } else {
                                finish();
                            }
                        } else {
                            finish();


                        }
                    } else {
                        finish();
                    }
                }
        );
    }


    private void crearFragmentConImagen(String photoPath) {
        EscanearImagenFragment fragmentConImagen = EscanearImagenFragment.newInstance(photoPath);
        getSupportFragmentManager().beginTransaction()
                .replace(FRAGMENT_CONTAINER_ID, fragmentConImagen, "IMAGEN_TAG")
                .commitNow();
    }

    private void crearFragmentVacio() {
        imagenEscaneadaPath = null;
        EscanearImagenFragment fragmentVacio = EscanearImagenFragment.newInstance(null);
        getSupportFragmentManager().beginTransaction()
                .replace(FRAGMENT_CONTAINER_ID, fragmentVacio, "IMAGEN_TAG")
                .commit();
    }

    private EscanearImagenFragment obtenerFragmentImagenActual() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("IMAGEN_TAG");
        if (fragment instanceof EscanearImagenFragment) {
            return (EscanearImagenFragment) fragment;
        }

        fragment = getSupportFragmentManager().findFragmentById(FRAGMENT_CONTAINER_ID);
        if (fragment instanceof EscanearImagenFragment) {
            return (EscanearImagenFragment) fragment;
        }

        for (Fragment frag : getSupportFragmentManager().getFragments()) {
            if (frag instanceof EscanearImagenFragment) {
                return (EscanearImagenFragment) frag;
            }
        }
        return null;
    }

    public void VolverFacturas(View view) {
        finish();
    }

    private void cambiarFragment(Fragment nuevoFragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, nuevoFragment, tag);
        transaction.commit();
        fragmentTagActual = tag;
    }

    public void mostrarVistaImagen(View view) {


        botonImagen.setBackgroundResource(R.drawable.fondo_seleccionado4);
        botonImagen.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        botonTexto.setBackgroundResource(android.R.color.transparent);
        botonTexto.setTextColor(obtenerColor(com.google.android.material.R.attr.colorOnSecondaryFixed));


        EscanearImagenFragment fragmentExistente = obtenerFragmentImagenActual();

        if (fragmentExistente == null) {
            fragmentExistente = EscanearImagenFragment.newInstance(imagenEscaneadaPath);
        }

        cambiarFragment(fragmentExistente, "IMAGEN_TAG");
        bottomBar.setVisibility(View.VISIBLE);
    }

    public void mostrarVistaTexto(View view) {
        botonTexto.setBackgroundResource(R.drawable.fondo_seleccionado4);
        botonTexto.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        botonImagen.setBackgroundResource(android.R.color.transparent);
        botonImagen.setTextColor(obtenerColor(com.google.android.material.R.attr.colorOnSecondaryFixed));

        if (textoFragment == null) {
            textoFragment = new EscanearTextoFragment();
        }
        cambiarFragment(textoFragment, "TEXTO_TAG");
        bottomBar.setVisibility(View.GONE);
    }

    private int obtenerColor(int atributo) {
        android.util.TypedValue valorTipeado = new android.util.TypedValue();
        getTheme().resolveAttribute(atributo, valorTipeado, true);
        return valorTipeado.data;
    }

    private void abrirEditorImagen() {
        Intent intent = new Intent(this, OpcionEditarActivity.class);
        if (imagenEscaneadaPath != null) {
            intent.putExtra("photo_paths", imagenEscaneadaPath);
        }
        startActivity(intent);
    }

    private void abrirDetalles() {
        Intent intent = new Intent(this, DetallesActivity.class);
        startActivity(intent);
    }

    public void lanzarEscaner() {
        Intent intent = new Intent(this, CameraEscaneoActivity.class);
        scannerLauncher.launch(intent);
    }
}