package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class EscanearActivity extends AppCompatActivity {

    private Button botonImagen, botonTexto, buttonCancelar;
    private View vistaImagen, fragmentTextoContainer;
    private EscanearImagenFragment imagenFragment;
    private EscanearTextoFragment textoFragment;
    private final int FRAGMENT_CONTAINER_ID = R.id.contenedor_fragment;
    private String fragmentTagActual = "IMAGEN_TAG";
    private View bottomBar;
    private LinearLayout opcionEditar;

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

        botonImagen = findViewById(R.id.boton_imagen);
        botonTexto = findViewById(R.id.boton_texto);
        fragmentTextoContainer = findViewById(R.id.fragment_texto_container);
        bottomBar = findViewById(R.id.barra_navegacion_inferior);


        if (savedInstanceState == null) {
            imagenFragment = new EscanearImagenFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(FRAGMENT_CONTAINER_ID, imagenFragment, "IMAGEN_TAG")
                    .commit();
        }

        opcionEditar = findViewById(R.id.opcion_editar);
        opcionEditar.setOnClickListener(v -> abrirEditorImagen());
    }

    public void VolverFacturas(View view) {
        finish();
    }

    private void cambiarFragment(Fragment nuevoFragment, String tag) {
        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(FRAGMENT_CONTAINER_ID, nuevoFragment, tag);
            transaction.commit();
            fragmentTagActual = tag;
        }
    }


    public void mostrarVistaImagen(View view) {

        botonImagen.setBackgroundResource(R.drawable.fondo_seleccionado4);
        botonImagen.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        botonTexto.setBackgroundResource(android.R.color.transparent);
        botonTexto.setTextColor(obtenerColor(com.google.android.material.R.attr.colorOnSecondaryFixed));
        if (imagenFragment == null) {
            imagenFragment = new EscanearImagenFragment();
        }
        cambiarFragment(imagenFragment, "IMAGEN_TAG");
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
        startActivity(intent);
    }
}