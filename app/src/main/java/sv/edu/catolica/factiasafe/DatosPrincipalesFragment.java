package sv.edu.catolica.factiasafe;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class DatosPrincipalesFragment extends Fragment {

    private NestedScrollView nestedScrollView;
    private TextInputEditText editFecha;
    private TabLayout tabLayout; // Para manejar el evento de re-selección de pestaña

    public DatosPrincipalesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_datos_principales, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Inicializar vistas dentro de este Fragment
        // El ID debe estar en fragment_datos_principales.xml
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        editFecha = view.findViewById(R.id.edit_fecha);

        // 2. Obtener la referencia al TabLayout de la Activity anfitriona
        // Esto es necesario para añadir el listener de re-selección.
        tabLayout = requireActivity().findViewById(R.id.tab_layout);

        setupFragmentListeners();
    }

    private void setupFragmentListeners() {
        // Manejar el click en el campo de Fecha (para abrir un DatePickerDialog)
        if (editFecha != null) {
            editFecha.setOnClickListener(v -> {
                // Aquí va tu código para abrir el DatePickerDialog (showDatePickerDialog())
                Toast.makeText(getContext(), "Abrir selector de fecha (lógica de fragment)", Toast.LENGTH_SHORT).show();
            });
        }

        // Manejar la re-selección de pestaña para desplazar hacia arriba
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                // No necesitamos lógica al seleccionar/deseleccionar aquí, el ViewPager2 lo maneja
                @Override
                public void onTabSelected(TabLayout.Tab tab) {}
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // La pestaña "Datos Principales" es la posición 0
                    if (tab.getPosition() == 0 && nestedScrollView != null) {
                        nestedScrollView.smoothScrollTo(0, 0);
                    }
                }
            });
        }
    }
}