package sv.edu.catolica.factiasafe;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.InputStream;

public class EscanearImagenFragment extends Fragment {

    private static final String ARG_PHOTO_PATH = "photo_path";
    private static final String TAG = "EscanearImagenFragment";

    private LinearLayout containerImagenes;
    private LinearLayout layoutAnadirImagen;
    private String photoPath;
    public static EscanearImagenFragment newInstance() {
        return newInstance(null);
    }

    public static EscanearImagenFragment newInstance(String photoPath) {
        EscanearImagenFragment fragment = new EscanearImagenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photoPath = getArguments().getString(ARG_PHOTO_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_escanear_imagen, container, false);
        containerImagenes = view.findViewById(R.id.container_imagenes);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mostrarImagen();
    }

    private void mostrarImagen() {
        Log.d(TAG, "🖼️ mostrarImagen llamado - path: " + photoPath);

        if (containerImagenes == null) {
            return;
        }

        containerImagenes.removeAllViews();

        if (photoPath == null || photoPath.isEmpty()) {
            mostrarPlaceholder();
        } else {
            cargarImagenDesdePath(photoPath);
        }
    }

    private void mostrarPlaceholder() {
        try {
            ImageView placeholderView = new ImageView(getContext());
            placeholderView.setImageResource(R.drawable.factura_placeholder4);
            placeholderView.setAdjustViewBounds(true);
            placeholderView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            placeholderView.setPadding(8, 8, 8, 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = android.view.Gravity.CENTER;
            placeholderView.setLayoutParams(params);

            containerImagenes.setGravity(android.view.Gravity.CENTER);
            containerImagenes.addView(placeholderView);
        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar placeholder", e);
        }
    }



    private void cargarImagenDesdePath(String path) {
        try {
            ImageView imageView = new ImageView(getContext());
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setPadding(8, 8, 8, 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = android.view.Gravity.CENTER;
            imageView.setLayoutParams(params);

            containerImagenes.setGravity(android.view.Gravity.CENTER);

            Uri uri;
            if (path.startsWith("content://") || path.startsWith("file://")) {
                uri = Uri.parse(path);
            } else {
                uri = Uri.fromFile(new File(path));
            }

            try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        containerImagenes.addView(imageView);
                    } else {
                        mostrarPlaceholder();
                    }
                } else {
                    mostrarPlaceholder();
                }
            }
        } catch (Exception e) {
            mostrarPlaceholder();
        }
    }



    private void abrirCameraActivity() {
        if (getActivity() instanceof EscanearActivity) {
            ((EscanearActivity) getActivity()).lanzarEscaner();
        }
    }


    public void agregarImagen(String nuevaImagen) {

        if (nuevaImagen == null || nuevaImagen.isEmpty()) {
            return;
        }

        photoPath = nuevaImagen;

        if (getArguments() == null) {
            setArguments(new Bundle());
        }
        getArguments().putString(ARG_PHOTO_PATH, photoPath);

        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                if (containerImagenes != null) {
                    mostrarImagen();
                }
            });
        }
    }

    public String getPhotoPath() {
        return photoPath;
    }

}