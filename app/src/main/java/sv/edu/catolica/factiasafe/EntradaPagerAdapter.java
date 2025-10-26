package sv.edu.catolica.factiasafe; // Ajusta el paquete

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import sv.edu.catolica.factiasafe.DatosExtraFragment;
import sv.edu.catolica.factiasafe.DatosPrincipalesFragment;

public class EntradaPagerAdapter extends FragmentStateAdapter {

    public EntradaPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Retorna el Fragment correspondiente a cada posición de pestaña
        switch (position) {
            case 0:
                return new DatosPrincipalesFragment();
            case 1:
                return new DatosExtraFragment();
            default:
                return new DatosPrincipalesFragment(); // Fallback
        }
    }

    @Override
    public int getItemCount() {
        // Tenemos dos pestañas
        return 2;
    }
}