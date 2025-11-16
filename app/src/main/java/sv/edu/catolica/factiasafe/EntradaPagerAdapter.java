package sv.edu.catolica.factiasafe;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import sv.edu.catolica.factiasafe.DatosExtraFragment;
import sv.edu.catolica.factiasafe.DatosPrincipalesFragment;

public class EntradaPagerAdapter extends FragmentStateAdapter {
    private final int invoiceId;
    private final SparseArray<Fragment> fragmentMap = new SparseArray<>();

    public EntradaPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.invoiceId = -1; // Para nueva entrada, sin ID
    }

    public EntradaPagerAdapter(@NonNull FragmentActivity fragmentActivity, int invoiceId) {
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

    public Fragment getFragment(int position) {
        return fragmentMap.get(position);
    }
}