package sv.edu.catolica.factiasafe;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DatosExtraFragment extends Fragment {


    public DatosExtraFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_datos_extra, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Aquí puedes inicializar y configurar el campo de texto input_datos_extras si es necesario.
        // Por ejemplo:
        // TextInputEditText inputDatosExtras = view.findViewById(R.id.input_datos_extras);
        // inputDatosExtras.setText("Texto predefinido");
    }
}