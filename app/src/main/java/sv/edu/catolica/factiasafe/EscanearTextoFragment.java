package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


public class EscanearTextoFragment extends Fragment {

    private Button buttonCancelar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escanear_texto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buttonCancelar = view.findViewById(R.id.button_cancelar);


        setupListeners();
    }

    private void setupListeners() {
        buttonCancelar.setOnClickListener(v -> requireActivity().onBackPressed());


    }
}