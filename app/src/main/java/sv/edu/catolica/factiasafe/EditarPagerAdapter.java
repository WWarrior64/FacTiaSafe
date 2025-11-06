package sv.edu.catolica.factiasafe;

import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class EditarPagerAdapter extends FragmentStateAdapter {
    private final int invoiceId;
    // Guardamos las referencias a los fragments por posición
    private final SparseArray<Fragment> fragmentMap = new SparseArray<>();

    public EditarPagerAdapter(@NonNull FragmentActivity fragmentActivity, int invoiceId) {
        super(fragmentActivity);
        this.invoiceId = invoiceId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        if (position == 0) {
            fragment = DatosPrincipalesFragment.newInstance(invoiceId);
        } else {
            fragment = DatosExtraFragment.newInstance(invoiceId);
        }
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    // Método público para obtener la referencia al fragment si ya fue creado
    public Fragment getFragment(int position) {
        return fragmentMap.get(position);
    }
}
