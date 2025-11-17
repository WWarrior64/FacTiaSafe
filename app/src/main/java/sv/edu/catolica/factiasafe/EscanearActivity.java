package sv.edu.catolica.factiasafe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EscanearActivity extends AppCompatActivity {

    private TessBaseAPI tess;
    private final ExecutorService ocrExecutor = Executors.newSingleThreadExecutor();
    private final String[] TESS_LANGS = {"eng", "spa"}; // idiomas que usarás (ajusta si hace falta)

    private Button botonImagen, botonTexto;
    private View fragmentTextoContainer;
    private EscanearTextoFragment textoFragment;
    private final int FRAGMENT_CONTAINER_ID = R.id.contenedor_fragment;
    private String fragmentTagActual = "IMAGEN_TAG";
    private View bottomBar;
    private LinearLayout opcionTomarDeNuevo;
    private static final String TAG = "EscanearActivity";
    private String imagenEscaneadaPath = null;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private boolean camaraYaLanzada = false;
    private HashMap<String, Object> datosExtraidos;
    private String textoOcrCrudo;

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

        // <<< Inicializar Tesseract (copiar traineddata desde res/raw a files/tessdata y init) >>>
        ensureTessDataAndInit();

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

        opcionTomarDeNuevo = findViewById(R.id.opcion_recargar);

        opcionTomarDeNuevo.setOnClickListener(v -> abrirEditorImagen());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // liberar tess en background
        ocrExecutor.submit(this::releaseTess);
        ocrExecutor.shutdown();
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
                                // Procesar OCR aquí
                                procesarOCR(photoPath);
                                // Crear fragment de imagen
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

    /**
     * Preprocesa bitmap siguiendo recomendaciones de Google ML Kit v2:
     * - Asegura suficiente resolución (texto >= 16x16 px, ideal 24x24)
     * - Mejora contraste para OCR
     * - Maneja rotación EXIF
     */
    private Bitmap preprocessBitmap(Bitmap src) {
        if (src == null) return null;
        
        Bitmap bmp = src.copy(Bitmap.Config.ARGB_8888, true);
        
        // 1) VALIDAR DIMENSIONES (Google: texto debe ser >= 16x16 px, mejor 24x24)
        int minTextPixels = 16;
        int avgCharWidth = bmp.getWidth() / 80;  // Asumir ~80 caracteres en ancho
        if (avgCharWidth < minTextPixels) {
            // Imagen muy pequeña, escalar (pero cuidado con límites de memoria)
            Log.w(TAG, "Imagen muy pequeña para OCR (" + bmp.getWidth() + "x" + bmp.getHeight() + 
                       "), escalando. Ancho/carácter: " + avgCharWidth + "px");
            float scale = (float) minTextPixels / avgCharWidth;
            // Limitar escala a máximo 2x para evitar artefactos
            scale = Math.min(scale, 2.0f);
            int newW = (int)(bmp.getWidth() * scale);
            int newH = (int)(bmp.getHeight() * scale);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, newW, newH, true);
            if (scaled != bmp) bmp.recycle();
            bmp = scaled;
            Log.d(TAG, "Imagen escalada a: " + newW + "x" + newH);
        }
        
        // 2) MEJORAR CONTRASTE (para mejor OCR)
        float contrast = 1.4f;
        float brightness = -40f;
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix(new float[] {
                contrast, 0, 0, 0, brightness,
                0, contrast, 0, 0, brightness,
                0, 0, contrast, 0, brightness,
                0, 0, 0, 1, 0
        });
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));
        android.graphics.Bitmap result = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(result);
        canvas.drawBitmap(bmp, 0, 0, paint);
        bmp.recycle();
        
        Log.d(TAG, "Imagen preprocesada: " + result.getWidth() + "x" + result.getHeight() + 
                   " (contraste=" + contrast + ", brillo=" + brightness + ")");
        return result;
    }
    
    /**
     * Detecta rotación EXIF de la imagen (recomendado por Google ML Kit)
     * Devuelve grados de rotación (0, 90, 180, 270)
     */
    private int getImageRotationDegrees(String imagePath) {
        try {
            if (imagePath == null) return 0;
            String filePath = imagePath;
            if (imagePath.startsWith("content://")) {
                // Si es content URI, intentar convertir a ruta real (limitado)
                return 0;
            }
            if (imagePath.startsWith("file://")) {
                filePath = imagePath.substring(7);
            }
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo leer rotación EXIF: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Rota un bitmap según grados (0, 90, 180, 270)
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        if (bitmap == null || degrees == 0) return bitmap;
        
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) bitmap.recycle();
        return rotated;
    }

    /**
     * Calcula el tamaño óptimo de imagen para OCR según Google ML Kit v2:
     * - Mínimo: texto 16x16 px (40-80 caracteres x línea)
     * - Óptimo: texto 24x24 px (mejor precisión)
     * - Máximo: para limitar memoria y latencia
     * Para facturas típicas (ancho ~200mm), a 96 DPI: ~750 píxeles.
     */
    private int calculateOptimalMaxDimension(int origWidth, int origHeight) {
        // Google recomienda para documentos: 720x1280 píxeles mínimo
        // Para facturas en tiempo real, podemos usar hasta 1600 para precisión
        int maxDim = 1400;  // Balance entre precisión y latencia
        
        // Si imagen es pequeña (< 640), mantener
        if (origWidth < 640 || origHeight < 640) {
            maxDim = 800;  // Menor dimensión para acelerar
        }
        // Si imagen es muy grande, reducir más
        else if (origWidth > 2000 || origHeight > 2000) {
            maxDim = 1600;  // Máximo para no sobrecargar
        }
        
        Log.d(TAG, "Dimensión máxima calculada: " + maxDim + "px (original: " + origWidth + "x" + origHeight + ")");
        return maxDim;
    }

    private void procesarOCR(String photoPath) {
        try {
            Uri uri = Uri.parse(photoPath.startsWith("content://") || photoPath.startsWith("file://") ? photoPath : "file://" + photoPath);
            InputStream inputStream = null;
            Bitmap bitmap = null;
            try {
                // 1) Leer solo bounds para calcular sampleSize (reduce memoria)
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                inputStream = getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(inputStream, null, opts);
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                
                // Google ML Kit v2: calcular máximo óptimo según dimensiones
                int maxDim = calculateOptimalMaxDimension(opts.outWidth, opts.outHeight);
                int sampleSize = 1;
                int w = opts.outWidth;
                int h = opts.outHeight;
                while (w / sampleSize > maxDim || h / sampleSize > maxDim) sampleSize *= 2;
                // 2) Decodificar con sampleSize
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = sampleSize;
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                inputStream = getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(inputStream, null, opts);
            } catch (Exception e) {
                Log.e(TAG, "Error decodificando imagen para OCR: " + e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception ignored) {}
                }
            }
            // 3) Si tenemos bitmap, aplicar rotación EXIF, preprocesado y crear InputImage
            if (bitmap != null) {
                // 3.1) Detectar y aplicar rotación EXIF (Google ML Kit v2 recomienda esto)
                int rotationDegrees = getImageRotationDegrees(photoPath);
                if (rotationDegrees > 0) {
                    Log.d(TAG, "Rotación EXIF detectada: " + rotationDegrees + " grados");
                    bitmap = rotateBitmap(bitmap, rotationDegrees);
                }
                
                // 3.2) Preprocesar (escalado y contraste)
                Bitmap processed = preprocessBitmap(bitmap);
                // si preprocess devuelve nuevo bitmap, reciclar el original para liberar memoria
                if (processed != bitmap) {
                    try {
                        bitmap.recycle();
                    } catch (Exception ignored) {}
                }
                
                // 3.3) Crear InputImage usando el bitmap preprocesado
                // NOTA: Google ML Kit v2 maneja rotación vía el parámetro, aquí usamos 0 porque ya rotamos
                InputImage image = InputImage.fromBitmap(processed, 0);
                // --- continuar con recognizer.process(image) como ya lo tienes ---
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                 textoOcrCrudo = visionText.getText(); // texto crudo
                                  
                                  // VALIDACIÓN GOOGLE ML KIT V2: Verificar calidad de resultado
                                  if (textoOcrCrudo == null || textoOcrCrudo.trim().isEmpty()) {
                                      Log.w(TAG, "⚠ OCR devolvió texto vacío. Posibles causas: " +
                                              "imagen muy pequeña, baja resolución, ángulo incorrecto");
                                      Toast.makeText(EscanearActivity.this,
                                          "OCR no detectó texto. Intenta con mejor iluminación o acerca la cámara.",
                                          Toast.LENGTH_LONG).show();
                                      return;
                                  }
                                  
                                  Log.d(TAG, "Texto OCR (crudo, " + textoOcrCrudo.length() + " chars): " + 
                                       (textoOcrCrudo.length() > 500 ? textoOcrCrudo.substring(0, 500) + "..." : textoOcrCrudo));
                                 
                                 // ============ FLUJO MEJORADO Y REORGANIZADO DE EXTRACCIÓN ============
                                 Log.d(TAG, "===== INICIO EXTRACCIÓN FACTURA =====");
                                 
                                 // 1) Extracción de metadatos (empresa, factura, fecha) - del texto crudo
                                 datosExtraidos = parsearTextoFactura(textoOcrCrudo);
                                 Log.d(TAG, "Metadatos extraídos: empresa=" + datosExtraidos.get("empresa") + 
                                         ", factura=" + datosExtraidos.get("factura") + 
                                         ", fecha=" + datosExtraidos.get("fecha"));
                                 
                                 // 2) Extracción de productos usando estructura de bloques (ML Kit - MEJOR para tablas)
                                 ParseItemsResult itemsRes = parseItemsFromBlocks(visionText);
                                 Log.d(TAG, "Items extraídos por bloques (ML Kit): " + itemsRes.items.size() + " items");
                                 
                                 // 3) Si no se extrajeron suficientes items, intentar estrategia alternativa (texto crudo)
                                 // La estrategia de texto crudo es fallback pero puede funcionar bien con tablas simples
                                 if (itemsRes.items.size() < 2) {
                                     Log.d(TAG, "Pocos items extraídos por bloques (" + itemsRes.items.size() + "), intentando extracción de texto crudo...");
                                     ParseItemsResult itemsRes2 = parseItemsFromText(textoOcrCrudo);
                                     Log.d(TAG, "Estrategia de texto crudo: " + itemsRes2.items.size() + " items");
                                     if (itemsRes2.items.size() > itemsRes.items.size()) {
                                         Log.d(TAG, "Usando estrategia de texto crudo (más items encontrados)");
                                         itemsRes = itemsRes2;
                                     } else if (itemsRes2.items.size() > 0 && itemsRes.items.isEmpty()) {
                                         Log.d(TAG, "ML Kit sin items, usando estrategia de texto crudo");
                                         itemsRes = itemsRes2;
                                     }
                                 } else {
                                     Log.d(TAG, "Extracción OK con ML Kit, usando bloques directamente");
                                 }
                                 
                                 // 4) Actualizar datos con items y subtotal
                                 datosExtraidos.put("items", itemsRes.items);
                                 datosExtraidos.put("subtotal", itemsRes.subtotal);
                                 
                                 Log.d(TAG, "===== FIN EXTRACCIÓN: " + itemsRes.items.size() + " items, subtotal=$" + 
                                         String.format(Locale.US, "%.2f", itemsRes.subtotal) + " =====");
                                 
                                 if (fragmentTagActual.equals("TEXTO_TAG") && textoFragment != null) {
                                     textoFragment.actualizarConDatos(datosExtraidos);
                                 }
                                 
                                 float avgConf = averageConfidence(visionText);
                                 // Toast siempre: advertir que OCR no es exacto
                                 Toast.makeText(EscanearActivity.this, 
                                     "OCR completado (confianza: " + String.format("%.0f", avgConf*100) + "%). " +
                                     "⚠ El OCR no es 100% exacto. Revisa y edita los datos antes de guardar.", 
                                     Toast.LENGTH_LONG).show();
                                 Log.d(TAG, "OCR completado. Confianza promedio: " + String.format("%.0f", avgConf*100) + "%");
                                 // --- Tesseract DESHABILITADO ---
                                 // ML Kit es más confiable que Tesseract para textos generales
                                 // Tesseract causa más problemas que soluciones, destruyendo descripciones
                                 Log.d(TAG, "Omitiendo re-evaluación con Tesseract (ML Kit es suficiente)");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Error OCR: " + e.getMessage());
                            }
                        });
            } else {
                Log.e(TAG, "No se pudo decodificar bitmap para OCR");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen para OCR", e);
        }
    }

    /**
     * Calcula confianza promedio de los elementos de texto (Google ML Kit v2)
     * También registra estadísticas detalladas por nivel (bloque, línea, elemento)
     */
    private float averageConfidence(Text visionText) {
        float sum = 0;
        int elementCount = 0;
        int blockCount = 0;
        int lineCount = 0;
        
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            blockCount++;
            for (Text.Line line : block.getLines()) {
                lineCount++;
                for (Text.Element el : line.getElements()) {
                    try {
                        float conf = el.getConfidence();
                        sum += conf;
                        elementCount++;
                    } catch (Exception ignored) {
                        // Si elemento no tiene confianza, ignorar
                    }
                }
            }
        }
        
        float avgConfidence = elementCount > 0 ? sum / elementCount : 0;
        Log.d(TAG, "OCR Statistics (ML Kit v2): " + blockCount + " bloques, " + 
                   lineCount + " líneas, " + elementCount + " elementos. " +
                   "Confianza promedio: " + String.format("%.1f%%", avgConfidence * 100));
        return avgConfidence;
    }

    // ==================== TOKEN / PARSE HELPERS (con soporte a correcciones) ====================

    /** Token con bbox y confidence para re-evaluación */
    private static class TokenInfo {
        String text;
        Rect box;
        float centerX, centerY;
        Float confidence; // puede ser null si no está disponible
        String correctedText; // texto corregido por Tesseract si aplica
    }

    /** Resultado del parseo de items */
    private static class ParseItemsResult {
        ArrayList<HashMap<String,String>> items = new ArrayList<>();
        double subtotal = 0.0;
        ArrayList<TokenInfo> tokenInfos = new ArrayList<>(); // tokens extraidos con bbox
    }


    /** Extrae tokens (TokenInfo) desde visionText, además genera items básicos (igual que antes) */
    private ParseItemsResult parseItemsFromBlocks(Text visionText) {
        ParseItemsResult res = new ParseItemsResult();
        if (visionText == null) return res;
        // Recolectar tokens con bbox/confidence
        ArrayList<TokenInfo> tokens = new ArrayList<>();
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element el : line.getElements()) {
                    android.graphics.Rect b = el.getBoundingBox();
                    if (b == null) continue;
                    TokenInfo t = new TokenInfo();
                    t.text = el.getText();
                    t.box = new Rect(b.left, b.top, b.right, b.bottom);
                    t.centerX = b.centerX();
                    t.centerY = b.centerY();
                    try {
                        t.confidence = el.getConfidence();
                    } catch (Exception e) {
                        t.confidence = null;
                    }
                    t.correctedText = null;
                    tokens.add(t);
                }
            }
        }
        res.tokenInfos = tokens;
        if (tokens.isEmpty()) return res;
        
        // ============ DETECCIÓN MEJORADA DE COLUMNA DE PRECIOS ============
        // Patrones más robustos para precios (soporta múltiples formatos)
        Pattern pricePattern1 = Pattern.compile("^[$]?[0-9]+([.,])[0-9]{2}$");  // $10.50 o $10,50
        Pattern pricePattern2 = Pattern.compile("^[0-9]+([.,])[0-9]{3}([.,])[0-9]{2}$");  // 1,234.56 o 1.234,56
        Pattern pricePattern3 = Pattern.compile("^[0-9]+([.,])[0-9]{2}$");  // 10.50 o 10,50
        Pattern pricePattern4 = Pattern.compile("^[$]?[0-9]{1,3}([,.])[0-9]{2}$");  // Precios cortos 5.99, 99.99
        
        ArrayList<Float> priceXs = new ArrayList<>();
        ArrayList<Integer> priceConfidences = new ArrayList<>();
        
        for (TokenInfo e : tokens) {
            String t = e.text.replaceAll("\\s", "").trim();
            if (t.isEmpty()) continue;
            
            boolean isPrice = pricePattern1.matcher(t).matches() || 
                             pricePattern2.matcher(t).matches() || 
                             pricePattern3.matcher(t).matches() ||
                             pricePattern4.matcher(t).matches();
            
            if (isPrice) {
                priceXs.add(e.centerX);
                priceConfidences.add(e.confidence != null ? (int)(e.confidence * 100) : 50);
            }
        }
        
        // Elegir columna de precios por mediana X (más robusto que promedio)
        float priceColumnX = -1f;
        if (!priceXs.isEmpty()) {
            priceXs.sort(Float::compare);
            priceColumnX = priceXs.get(priceXs.size() / 2);
            Log.d(TAG, "Columna de precios detectada en X=" + priceColumnX + " (" + priceXs.size() + " precios)");
        }
        
        // ============ AGRUPACIÓN DE FILAS CON TOLERANCIA ADAPTATIVA MEJORADA (ML KIT V2) ============
        tokens.sort((a,b)->Float.compare(a.centerY, b.centerY));
        ArrayList<ArrayList<TokenInfo>> rows = new ArrayList<>();
        
        // Calcular altura promedio de tokens para adaptar tolerancia
        float avgHeight = 0;
        float minHeight = Float.MAX_VALUE;
        float maxHeight = 0;
        for (TokenInfo t : tokens) {
            float h = t.box.height();
            avgHeight += h;
            minHeight = Math.min(minHeight, h);
            maxHeight = Math.max(maxHeight, h);
        }
        avgHeight = tokens.isEmpty() ? 20 : avgHeight / tokens.size();
        minHeight = minHeight == Float.MAX_VALUE ? 20 : minHeight;
        
        // Tolerancia más inteligente: considerar variabilidad de alturas
        // En facturas pueden haber filas con alturas variables (líneas simples vs líneas dobles)
        float yTol = Math.max(minHeight * 1.2f, avgHeight * 0.6f);
        // Pero no menos de 15px para no fragmentar filas
        yTol = Math.max(15f, Math.min(yTol, 40f));
        
        Log.d(TAG, "Agrupación de filas (ML Kit v2): altura prom=" + String.format("%.1f", avgHeight) + 
                   "px, min=" + String.format("%.1f", minHeight) + "px, max=" + String.format("%.1f", maxHeight) + 
                   "px, tolerancia Y=" + String.format("%.1f", yTol) + "px. Total tokens: " + tokens.size());
        
        for (TokenInfo e : tokens) {
            if (rows.isEmpty()) {
                ArrayList<TokenInfo> r = new ArrayList<>();
                r.add(e);
                rows.add(r);
            } else {
                ArrayList<TokenInfo> last = rows.get(rows.size()-1);
                float avgY = 0;
                for (TokenInfo le : last) avgY += le.centerY;
                avgY /= last.size();
                
                // Usar altura de token para ajustar tolerancia
                float tokenHeight = Math.max(e.box.height(), 8);
                float adaptiveTol = Math.max(yTol, tokenHeight * 0.7f);
                
                if (Math.abs(e.centerY - avgY) <= adaptiveTol) {
                    last.add(e);
                } else {
                    ArrayList<TokenInfo> r = new ArrayList<>();
                    r.add(e);
                    rows.add(r);
                }
            }
        }
        // ============ PATRONES MEJORADOS PARA DETECTAR PRECIOS Y FOOTERS (ML KIT V2) ============
        Pattern strictPrice = Pattern.compile("^[\\$]?([0-9]+(?:[\\.,][0-9]{3})*(?:[\\.,][0-9]{2}))$");
        
        // Footer pattern: líneas que son definitivamente NO productos (encabezados, totales, firmas)
        // Más conservador para NO filtrar filas válidas de productos
        Pattern footerPattern = Pattern.compile("(?i).*(^\\s*(sumas|subtotal|total|TOTAL(?:\\s+VENTA)?|ventas\\s+gravadas|" +
                "ventas\\s+no\\s+sujetas|vtas\\.?\\s+exentas|autorizaci(o|ó)n|resoluci[oó]n|firma|recibido\\s+por|" +
                "firma\\s+cajera|duplicado|nrc|nit|registro|serie(?:\\s+\\d+)?|tasa|por\\s+pagar|observacion|" +
                "nota|aclaraci|condicion|pago|pague|descuento|dscto|iva|retencion|retención)\\b.*)");
        
        Pattern subtotalPattern = Pattern.compile("(?i).*\\b(subtotal|sub[- ]?total|sumas|importe|monto)\\b.*");
        Pattern potentialQtyPattern = Pattern.compile("^(\\d+(?:[.,]\\d+)?)$");
        
        // DEBUG: Log para ver filtrado
        int footerFilteredCount = 0;
        
        // Log de depuración: mostrar cuántas filas se detectaron
        Log.d(TAG, "Procesando " + rows.size() + " filas detectadas");
        
        // Para cada fila: ordenar por X y detectar precio usando columna si existe
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            ArrayList<TokenInfo> row = rows.get(rowIdx);
            row.sort((a,b)->Float.compare(a.centerX, b.centerX));
            ArrayList<String> tokensText = new ArrayList<>();
            for (TokenInfo e : row) tokensText.add(e.text.trim());
            String rowText = String.join(" ", tokensText).trim();
            if (rowText.length() < 2) continue;
            
            // Filtro de footer MEJORADO: más estricto para no perder productos
            if (footerPattern.matcher(rowText).matches()) {
                Log.d(TAG, "Fila omitida (footer): \"" + rowText + "\"");
                footerFilteredCount++;
                continue;
            }
            
            // Log detallado para DEBUG (mostrar primeras 100 caracteres)
            if (rowIdx < 20) {  // Solo primeras 20 filas
                Log.d(TAG, "Fila " + rowIdx + ": \"" + (rowText.length() > 80 ? rowText.substring(0, 80) + "..." : rowText) + "\"");
            }
            
            // 1) Si tenemos priceColumnX, buscar el elemento más cercano a esa X en la fila
            String priceToken = null;
            int priceIdx = -1;
            if (priceColumnX > 0) {
                float minDist = Float.MAX_VALUE;
                int bestI = -1;
                for (int i = 0; i < row.size(); i++) {
                    TokenInfo e = row.get(i);
                    float d = Math.abs(e.centerX - priceColumnX);
                    if (d < minDist && d < 80f) {  // umbral de proximidad a la columna
                        minDist = d;
                        bestI = i;
                    }
                }
                if (bestI >= 0) {
                    String cand = row.get(bestI).text.replaceAll("[^0-9,\\.\\$]","").trim();
                    Matcher m = strictPrice.matcher(cand);
                    if (m.find()) {
                        priceToken = m.group(1);
                        priceIdx = bestI;
                    }
                }
            }
            
            // 2) Fallback: buscar desde final hacia atrás token que sea precio
            if (priceToken == null) {
                for (int i = tokensText.size()-1; i >= 0; i--) {
                    String t = tokensText.get(i).replaceAll("[^0-9,\\.\\$]","").trim();
                    Matcher m = strictPrice.matcher(t);
                    if (m.find()) {
                        priceToken = m.group(1);
                        priceIdx = i;
                        break;
                    }
                }
            }
            if (priceToken == null) continue;  // no precio en esta fila -> omitir
            double price = parseAmountLenient(priceToken);
            if (price <= 0) continue;
            
            // ============ EXTRACCIÓN MEJORADA DE CANTIDAD ============
            double qty = 1.0;
            int qtyIdx = -1;
            
            Log.d(TAG, "Procesando fila: " + rowText + " | Precio encontrado: " + priceToken + " en índice " + priceIdx);
            
            // Estrategia 1: token justo ANTES del precio
            if (priceIdx > 0) {
                String prev = tokensText.get(priceIdx-1).replaceAll("[^0-9,\\.]","").trim();
                if (!prev.isEmpty() && prev.matches("^\\d+(?:[\\.,]\\d+)?$")) {
                    double qCandidate = parseAmountLenient(prev);
                    if (qCandidate > 0 && qCandidate < 1000) {  // validar rango sensato
                        qty = qCandidate;
                        qtyIdx = priceIdx - 1;
                    }
                }
            }
            
            // Estrategia 2: si no encontró, buscar primer token que parece cantidad
            if (qty == 1.0) {
                for (int i = 0; i < priceIdx && i < tokensText.size(); i++) {
                    String t = tokensText.get(i).replaceAll("[^0-9,\\.]","").trim();
                    if (!t.isEmpty() && t.matches("^\\d+(?:[\\.,]\\d+)?$")) {
                        double qCandidate = parseAmountLenient(t);
                        if (qCandidate > 0 && qCandidate < 1000) {
                            qty = qCandidate;
                            qtyIdx = i;
                            break;
                        }
                    }
                }
            }
            
            // ============ EXTRACCIÓN MEJORADA DE DESCRIPCIÓN PARA TABLAS ============
            // Estrategia 1: tokens después de cantidad/código pero antes de precio
            StringBuilder descSb = new StringBuilder();
            int descEnd = priceIdx - 1;
            
            // Excluir token de cantidad si está justo antes del precio
            if (qtyIdx == descEnd) {
                descEnd--;
            }
            
            // Construir descripción desde el primer token NO-CÓDIGO/NO-CANTIDAD hasta el precio
            boolean foundDescStart = false;
            for (int i = 0; i <= descEnd; i++) {
                String tok = tokensText.get(i).trim();
                
                // Saltar si es el token de cantidad
                if (i == qtyIdx) continue;
                // Saltar códigos al inicio
                if (!foundDescStart && isCodeToken(tok)) continue;
                // Saltar números puros al inicio (pueden ser códigos/SKU)
                if (!foundDescStart && tok.matches("^\\d+$")) continue;
                // Saltar headers/labels
                if (isProbableHeaderToken(tok)) continue;
                
                if (!tok.isEmpty()) {
                    foundDescStart = true;
                    descSb.append(tok).append(" ");
                }
            }
            
            String desc = stripLeadingCodes(descSb.toString().trim());
            
            // ============ LOOKBACK: BUSCAR DESCRIPCIÓN EN FILAS ANTERIORES (tabla multi-línea) ============
            if (desc.isEmpty() && rowIdx > 0) {
                // Buscar hasta 2 filas anteriores (pueden ser línea de continuación del producto)
                for (int lookback = 1; lookback <= 2 && (rowIdx - lookback) >= 0; lookback++) {
                    ArrayList<TokenInfo> prevRow = rows.get(rowIdx - lookback);
                    
                    // La fila anterior es válida si NO tiene precio
                    boolean hasPriceInPrev = false;
                    for (TokenInfo tk : prevRow) {
                        if (tk.text.matches(".*[$]?[0-9]+[.,][0-9]{2}.*")) {
                            hasPriceInPrev = true;
                            break;
                        }
                    }
                    
                    if (!hasPriceInPrev) {
                        StringBuilder prevDescSb = new StringBuilder();
                        for (TokenInfo tk : prevRow) {
                            String t = tk.text.trim();
                            if (!isProbableHeaderToken(t) && !isCodeToken(t) && !t.isEmpty()) {
                                prevDescSb.append(t).append(" ");
                            }
                        }
                        String candDesc = stripLeadingCodes(prevDescSb.toString().trim());
                        if (!candDesc.isEmpty() && candDesc.length() > 2) {
                            desc = candDesc;
                            Log.d(TAG, "Descripción multi-línea rescatada (lookback " + lookback + "): " + desc);
                            break;
                        }
                    }
                }
            }
            
            if (desc.isEmpty()) {
                // Último recurso: tomar todos los tokens antes del precio excepto código/header
                for (int i = 0; i < priceIdx; i++) {
                    String tok = tokensText.get(i).trim();
                    if (!isCodeToken(tok) && !isProbableHeaderToken(tok) && !tok.isEmpty() && !tok.matches("^\\d+$")) {
                        desc = tok;
                        break;
                    }
                }
            }
            
            if (desc.isEmpty()) desc = "PRODUCTO";
                
                // ============ CREAR ITEM CON DATOS EXTRAÍDOS ============
                HashMap<String,String> item = new HashMap<>();
                item.put("producto", desc);
                
                // Formatear cantidad (entero si es exacto, decimal si no)
                if (Math.abs(qty - Math.round(qty)) < 0.001) {
                 item.put("cantidad", String.valueOf((int)Math.round(qty)));
                } else {
                 item.put("cantidad", String.format(Locale.US, "%.2f", qty));
                }
                
                item.put("precio", String.format(Locale.US, "%.2f", price));
                res.items.add(item);
                res.subtotal += qty * price;
                
                Log.d(TAG, "Item extraído [" + res.items.size() + "]: " + desc + 
                       " | qty=" + String.format("%.2f", qty) + " | precio=$" + 
                       String.format(Locale.US, "%.2f", price));
        }
        
        // RESUMEN FINAL DE EXTRACCIÓN (ML KIT V2)
        Log.d(TAG, "╔═══════════════════════════════════════════════════════");
        Log.d(TAG, "║ RESUMEN parseItemsFromBlocks (ML Kit v2)");
        Log.d(TAG, "║ Filas procesadas: " + rows.size() + " | Footers filtrados: " + footerFilteredCount);
        Log.d(TAG, "║ Items extraídos: " + res.items.size());
        Log.d(TAG, "║ Subtotal calculado: $" + String.format(Locale.US, "%.2f", res.subtotal));
        Log.d(TAG, "║ Precios detectados: " + priceXs.size() + " | Columna X: " + 
               String.format("%.0f", priceColumnX));
        Log.d(TAG, "╚═══════════════════════════════════════════════════════");
        
        return res;
    }

    /** Aplica SOLO correcciones numéricas a los items originales, sin alterar descripciones */
    private void applyNumericCorrectionsToItems(ArrayList<HashMap<String,String>> items, ArrayList<TokenInfo> tokenInfos) {
        if (items == null || tokenInfos == null) return;
        
        // Map de precios y cantidades con sus tokens
        for (TokenInfo tkn : tokenInfos) {
            if (tkn.correctedText == null || tkn.correctedText.isEmpty()) continue;
            
            String corrVal = tkn.correctedText.trim();
            double corrAmount = parseAmountLenient(corrVal);
            if (corrAmount <= 0) continue;
            
            // Buscar el item que mejor coincide con esta posición Y
            // Revisar si es un precio (último campo) o cantidad (primero)
            boolean likelyPrice = corrAmount > 5; // precios típicamente > 5
            
            for (HashMap<String,String> item : items) {
                double itemPrice = parseAmountLenient(item.getOrDefault("precio", "0"));
                double itemQty = parseAmountLenient(item.getOrDefault("cantidad", "1"));
                
                if (likelyPrice && Math.abs(itemPrice - parseAmountLenient(tkn.text)) < 0.01) {
                    // Coincide con el precio del item
                    item.put("precio", String.format(Locale.US, "%.2f", corrAmount));
                    Log.d(TAG, "Corrección de precio aplicada: " + tkn.text + " -> " + corrVal);
                    break;
                } else if (!likelyPrice && Math.abs(itemQty - parseAmountLenient(tkn.text)) < 0.01) {
                    // Coincide con la cantidad del item
                    if (Math.abs(corrAmount - Math.round(corrAmount)) < 0.001) {
                        item.put("cantidad", String.valueOf((int)Math.round(corrAmount)));
                    } else {
                        item.put("cantidad", String.format(Locale.US, "%.2f", corrAmount));
                    }
                    Log.d(TAG, "Corrección de cantidad aplicada: " + tkn.text + " -> " + corrVal);
                    break;
                }
            }
        }
    }

    /** Reconstruye items desde tokenInfos ya corregidos (aplica correctedText si existe) */
    private ParseItemsResult rebuildItemsFromTokenInfos(ArrayList<TokenInfo> tokenInfos) {
        ParseItemsResult res = new ParseItemsResult();
        if (tokenInfos == null || tokenInfos.isEmpty()) return res;
        // construir lista de tokens con texto corregido si existe
        ArrayList<TokenInfo> tokens = new ArrayList<>();
        for (TokenInfo t : tokenInfos) {
            TokenInfo c = new TokenInfo();
            c.text = (t.correctedText != null && !t.correctedText.isEmpty()) ? t.correctedText : t.text;
            c.box = t.box;
            c.centerX = t.centerX;
            c.centerY = t.centerY;
            c.confidence = t.confidence;
            tokens.add(c);
        }
        // Reutilizamos la lógica de parsing (similar a parseItemsFromBlocks)
        // Detectar precio X
        Pattern priceTokenLoose = Pattern.compile("^\\s*[$]?[0-9]+[.,]?[0-9]*[.,]?[0-9]+\\s*$");
        ArrayList<Float> priceXs = new ArrayList<>();
        for (TokenInfo e : tokens) {
            String t = e.text.replaceAll("[^0-9,\\.\\$]", "").trim();
            if (t.isEmpty()) continue;
            if (priceTokenLoose.matcher(t).matches()) priceXs.add(e.centerX);
        }
        float priceColumnX = -1f;
        if (!priceXs.isEmpty()) {
            priceXs.sort(Float::compare);
            priceColumnX = priceXs.get(priceXs.size()/2);
        }
        tokens.sort((a,b)->Float.compare(a.centerY, b.centerY));
        ArrayList<ArrayList<TokenInfo>> rows = new ArrayList<>();
        float yTol = 30f;
        for (TokenInfo e : tokens) {
            if (rows.isEmpty()) {
                rows.add(new ArrayList<>() {{ add(e); }});
            } else {
                ArrayList<TokenInfo> last = rows.get(rows.size()-1);
                float avgY = 0;
                for (TokenInfo ti : last) avgY += ti.centerY;
                avgY /= last.size();
                if (Math.abs(e.centerY - avgY) <= yTol) last.add(e);
                else rows.add(new ArrayList<>() {{ add(e); }});
            }
        }
        Pattern strictPrice = Pattern.compile("^[\\$]?([0-9]+(?:[\\.,][0-9]{3})*(?:[\\.,][0-9]{2}))$");
        Pattern footerPattern = Pattern.compile("(?i).*\\b(sumas|subtotal|total venta|ventas gravadas|ventas no sujetas|vtas\\.? exentas|autorizaci(o|ó)n|resoluci[oó]n|firma|recibido por|firma cajera|duplicado|nrc|nit|registro|serie|factura)\\b.*");
        for (ArrayList<TokenInfo> row : rows) {
            row.sort((a,b)->Float.compare(a.centerX, b.centerX));
            ArrayList<String> tokensText = new ArrayList<>();
            for (TokenInfo ti : row) tokensText.add(ti.text.trim());
            String rowText = String.join(" ", tokensText).trim();
            if (rowText.length() < 2) continue;
            if (footerPattern.matcher(rowText).matches()) continue;
            String priceToken = null;
            int priceIdx = -1;
            if (priceColumnX > 0) {
                float minDist = Float.MAX_VALUE;
                int bestI = -1;
                for (int i = 0; i < row.size(); i++) {
                    float d = Math.abs(row.get(i).centerX - priceColumnX);
                    if (d < minDist) {
                        minDist = d;
                        bestI = i;
                    }
                }
                if (bestI >= 0) {
                    String cand = row.get(bestI).text.replaceAll("[^0-9,\\.\\$]","").trim();
                    Matcher m = strictPrice.matcher(cand);
                    if (m.find()) {
                        priceToken = m.group(1);
                        priceIdx = bestI;
                    }
                }
            }
            if (priceToken == null) {
                for (int i = tokensText.size()-1; i >= 0; i--) {
                    String t = tokensText.get(i).replaceAll("[^0-9,\\.\\$]","").trim();
                    Matcher m = strictPrice.matcher(t);
                    if (m.find()) {
                        priceToken = m.group(1);
                        priceIdx = i;
                        break;
                    }
                }
            }
            if (priceToken == null) continue;
            double price = parseAmountLenient(priceToken);
            if (price <= 0) continue;
            double qty = 1.0;
            if (priceIdx > 0) {
                String prev = tokensText.get(priceIdx-1).replaceAll("[^0-9,\\.]","").trim();
                if (!prev.isEmpty() && prev.matches("^\\d+(?:[\\.,]\\d+)?$")) {
                    qty = parseAmountLenient(prev);
                }
            }
            // Si no se seteo desde prev, chequea first si es num
            if (qty == 1.0) {
                String first = tokensText.get(0).replaceAll("[^0-9,\\.]","").trim();
                if (!first.isEmpty() && first.matches("^\\d+(?:[\\.,]\\d+)?$")) {
                    qty = parseAmountLenient(first);
                }
            }
            StringBuilder descSb = new StringBuilder();
            int descEnd = priceIdx - 1;
            if (descEnd >= 0) {
                String maybeQty = tokensText.get(descEnd).replaceAll("[^0-9,\\.]","").trim();
                if (!maybeQty.isEmpty() && maybeQty.matches("^\\d+(?:[\\.,]\\d+)?$")) {
                    descEnd--;
                }
            }
            for (int i = 0; i <= descEnd; i++) {
                String tok = tokensText.get(i);
                // saltar qty al inicio
                if (i == 0 && tok.matches("^\\d+(?:[\\.,]\\d+)?$")) continue;
                // saltar códigos en la primera posición
                if (i == 0 && isCodeToken(tok)) continue;
                // saltar tokens que claramente son header/labels
                if (isProbableHeaderToken(tok)) continue;
                descSb.append(tok).append(" ");
            }
            String desc = stripLeadingCodes(descSb.toString().trim());
            if (desc.isEmpty()) {
                for (int i = 0; i <= descEnd; i++) {
                    String cand = tokensText.get(i).trim();
                    if (!cand.isEmpty() && !isCodeToken(cand) && !isProbableHeaderToken(cand)) {
                        desc = cand;
                        break;
                    }
                }
            }
            if (desc.isEmpty()) desc = "PRODUCTO";
            HashMap<String,String> item = new HashMap<>();
            item.put("producto", desc);
            if (Math.abs(qty - Math.round(qty)) < 0.001) item.put("cantidad", String.valueOf((int)Math.round(qty)));
            else item.put("cantidad", String.format(Locale.US, "%.2f", qty));
            item.put("precio", String.format(Locale.US, "%.2f", price));
            res.items.add(item);
            res.subtotal += qty * price;
        }
        return res;
    }

    private HashMap<String, Object> parsearTextoFactura(String texto) {
        HashMap<String, Object> datos = new HashMap<>();
        if (texto == null) texto = "";

        Log.d(TAG, "===== PARSEAR FACTURA - EXTRAYENDO METADATOS =====");

        // ============ DETECCIÓN DE EMPRESA (estrategia mejorada) ============
        String empresa = findCompanyNameWithSA(texto);
        Log.d(TAG, "findCompanyNameWithSA retornó: " + empresa);
        
        // Fallback 1: líneas iniciales antes de palabras clave
        if (empresa == null || empresa.trim().isEmpty() || "EMPRESA DESCONOCIDA".equals(empresa)) {
            Log.d(TAG, "Intento fallback 1: búsqueda de bloque inicial...");
            // Buscar todo antes de la primera línea que empiece con palabra clave
            String[] lineas = texto.split("\n");
            for (int i = 0; i < Math.min(5, lineas.length); i++) {
                String lin = lineas[i].trim();
                if (lin.length() > 3 && 
                    !lin.matches("(?i).*(FACTURA|FECHA|NIT|NRC|No\\.|CLIENTE|DIRECCION).*") &&
                    !lin.matches("^[0-9\\-\\.]+$")) {
                    empresa = normalizeCompanyName(lin);
                    if (!empresa.isEmpty()) {
                        Log.d(TAG, "Empresa encontrada en línea " + i + ": " + empresa);
                        break;
                    }
                }
            }
        }
        
        if (empresa == null || empresa.trim().isEmpty()) {
            empresa = "";
        }
        
        datos.put("empresa", empresa);
        Log.d(TAG, "Empresa final: '" + empresa + "'");

        // --- FACTURA: múltiples patrones de búsqueda ---
        String facturaNum = null;
        
        // Patrón 1: "No. 12345" o "NO. 12345"
        Pattern p1 = Pattern.compile("(?i)No\\.\\s+(\\d{4,})");
        Matcher m = p1.matcher(texto);
        if (m.find()) {
            facturaNum = m.group(1);
            Log.d(TAG, "Factura detectada (patrón No.): " + facturaNum);
        }
        
        // Patrón 2: "FACTURA: 12345" o "FACTURA #12345"
        if (facturaNum == null) {
            Pattern p2 = Pattern.compile("(?i)FACTURA[\\s:#+\\-]+(\\d{4,})");
            m = p2.matcher(texto);
            if (m.find()) {
                facturaNum = m.group(1);
                Log.d(TAG, "Factura detectada (patrón FACTURA): " + facturaNum);
            }
        }
        
        // Patrón 3: "N° 12345" (con símbolo de grado)
        if (facturaNum == null) {
            Pattern p3 = Pattern.compile("(?i)N°\\s+(\\d{4,})");
            m = p3.matcher(texto);
            if (m.find()) {
                facturaNum = m.group(1);
                Log.d(TAG, "Factura detectada (patrón N°): " + facturaNum);
            }
        }
        
        if (facturaNum != null) {
            datos.put("factura", facturaNum);
        } else {
            Log.d(TAG, "No se detectó número de factura");
        }

        // --- FECHA: múltiples formatos ---
        String fechaStr = null;
        
        // Patrón 1: "Lunes 15 de Junio del 2024" o similar
        Pattern pf1 = Pattern.compile("(?i)(?:Lunes|Martes|Miércoles|Miercoles|Jueves|Viernes|Sábado|Sabado|Domingo)?\\s*(\\d{1,2})\\s+de\\s+([A-Za-záéíóúñÑ]+)\\s+(?:del|de|de\\s+)(?:año\\s+)?(\\d{4})");
        Matcher mf = pf1.matcher(texto);
        if (mf.find()) {
            String dia = mf.group(1);
            String mesStr = mf.group(2);
            String ano = mf.group(3);
            String mes = convertirMesANumero(mesStr);
            fechaStr = String.format("%02d", Integer.parseInt(dia)) + "/" + mes + "/" + ano;
            Log.d(TAG, "Fecha detectada (patrón 1): " + fechaStr);
        }
        
        // Patrón 2: "15/06/2024" o "15-06-2024"
        if (fechaStr == null) {
            Pattern pf2 = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})");
            mf = pf2.matcher(texto);
            if (mf.find()) {
                fechaStr = String.format("%02d", Integer.parseInt(mf.group(1))) + "/" + 
                           String.format("%02d", Integer.parseInt(mf.group(2))) + "/" + 
                           mf.group(3);
                Log.d(TAG, "Fecha detectada (patrón 2 numérico): " + fechaStr);
            }
        }
        
        // Patrón 3: "FECHA: 15 junio 2024"
        if (fechaStr == null) {
            Pattern pf3 = Pattern.compile("(?i)FECHA[\\s:]+(?:del?\\s+)?(\\d{1,2})\\s+(?:de\\s+)?([A-Za-záéíóúñÑ]+)\\s+(?:del?\\s+)?(\\d{4})");
            mf = pf3.matcher(texto);
            if (mf.find()) {
                String dia = mf.group(1);
                String mesStr = mf.group(2);
                String ano = mf.group(3);
                String mes = convertirMesANumero(mesStr);
                fechaStr = String.format("%02d", Integer.parseInt(dia)) + "/" + mes + "/" + ano;
                Log.d(TAG, "Fecha detectada (patrón FECHA:): " + fechaStr);
            }
        }
        
        if (fechaStr != null) {
            datos.put("fecha", fechaStr);
        } else {
            Log.d(TAG, "No se detectó fecha válida");
        }

        // --- ÍTEMS: usar helper robusto que trabaja línea a línea (precio final, qty heurística) ---
        ArrayList<HashMap<String, String>> items = new ArrayList<>();
        double subtotal = 0;
        ParseItemsResult itemsRes = parseItemsFromText(texto);
        items = itemsRes.items;
        subtotal = itemsRes.subtotal;
        datos.put("items", items);
        datos.put("subtotal", subtotal);

        // --- IMPUESTO: intenta detectar IVA/Impuesto (ignorando Retención) ---
        Pattern impuestoPattern = Pattern.compile("(?i)(IVA|Impuesto)(?!.*(Retencion|retenido))\\s*(\\d+)%?\\s*(\\d+\\.\\d{2})?");
        Matcher matcher = impuestoPattern.matcher(texto);
        if (matcher.find()) {
            datos.put("impuesto_porcentaje", matcher.group(2) != null ? matcher.group(2) : "0");
            datos.put("impuesto_cantidad", matcher.group(3) != null ? matcher.group(3) : "0");
            datos.put("impuesto_aplicado", 1);
        } else {
            // Check exentas/no sujetas para set 0 y adjust subtotal
            Pattern exentaPattern = Pattern.compile("(?i)(VTAS\\. EXENTAS|VTAS NO SUJETAS|V Exentas|V No Sujetas)\\s*(\\d+\\.\\d{2})");
            matcher = exentaPattern.matcher(texto);
            if (matcher.find()) {
                subtotal = Double.parseDouble(matcher.group(2));
                datos.put("subtotal", subtotal);
            }
            datos.put("impuesto_porcentaje", "0");
            datos.put("impuesto_cantidad", "0");
            datos.put("impuesto_aplicado", 0);
        }

        // --- DESCUENTO ---
        Pattern descuentoPattern = Pattern.compile("(?i)(Descuento|Discount)\\s*(\\d+)%?\\s*(\\d+\\.\\d{2})?");
        matcher = descuentoPattern.matcher(texto);
        if (matcher.find()) {
            datos.put("descuento_porcentaje", matcher.group(2));
            datos.put("descuento_cantidad", matcher.group(3) != null ? matcher.group(3) : "0");
        } else {
            datos.put("descuento_porcentaje", "0");
            datos.put("descuento_cantidad", "0");
        }

        // --- TOTAL ---
        Pattern totalPattern = Pattern.compile("(?i)TOTAL VENTA\\s*(\\d+\\.\\d{2})");
        matcher = totalPattern.matcher(texto);
        if (matcher.find()) {
            datos.put("total", matcher.group(1));
        } else {
            double tax = datos.containsKey("impuesto_cantidad") ? Double.parseDouble((String) datos.get("impuesto_cantidad")) : 0;
            double disc = datos.containsKey("descuento_cantidad") ? Double.parseDouble((String) datos.get("descuento_cantidad")) : 0;
            datos.put("total", subtotal + tax - disc);
        }

        // --- GARANTÍA: mantengo lógica existente intacta ---
        Pattern garantiaPattern = Pattern.compile("(?i)(\\d+)\\s*MESES DE GARANTIA");
        matcher = garantiaPattern.matcher(texto);
        if (matcher.find()) {
            int meses = Integer.parseInt(matcher.group(1));
            String fechaFactura = (String) datos.get("fecha");
            if (fechaFactura != null) {
                String start = fechaFactura;  // DD/MM/YYYY
                String end = calcularFechaFinGarantia(start, meses);
                datos.put("garantia_start", start);
                datos.put("garantia_end", end);
            }
        }

        // --- NOTAS EXTRAS ---
        int indexTotal = texto.toLowerCase().indexOf("total venta");
        String notasExtras = (indexTotal != -1) ? texto.substring(indexTotal + 11).trim() : "";
        notasExtras = notasExtras.replaceAll("(?i)(HECHO POR|AUTOR|RECIBI|DUPLICADO).*", "").trim();
        datos.put("notas_extras", notasExtras);

        return datos;
    }

    private ParseItemsResult parseItemsFromText(String texto) {
         ParseItemsResult res = new ParseItemsResult();
         if (texto == null) return res;

         Log.d(TAG, "parseItemsFromText: extrayendo items del texto crudo...");
         
         // Intentar localizar encabezado para empezar después (si existe)
         int start = 0;
         Pattern header = Pattern.compile("(?im)^.*\\b(CANT|CANT\\.|Cantidad|DESCRIPCION|Desc\\.|P\\.?\\s?UNITARIO|PRECIO UNITARIO|CANTIDAD|ITEM|ART\\.?)\\b.*$");
         Matcher h = header.matcher(texto);
         if (h.find()) {
             start = h.end();
             Log.d(TAG, "Encabezado encontrado, iniciando después...");
         }

         String tail = texto.substring(start);

         // Dividir por líneas y limpiar
         String[] allLines = tail.split("\\r?\\n");

         // ============ PATRONES MEJORADOS DE PRECIO ============
         // Patrón precio al final (acepta múltiples formatos)
         Pattern pricePattern = Pattern.compile("([0-9]+(?:[\\.,][0-9]{3})*(?:[\\.,][0-9]{2}))\\s*$");
         // Patrón cantidad al inicio  
         Pattern qtyAtStart = Pattern.compile("^\\s*(\\d+(?:[\\.,]\\d+)?)\\b");

         // 1) Elegir el segmento (block) de líneas donde más precios aparecen (probable tabla)
         int[] bestRange = selectBestItemsSegment(allLines, pricePattern);
         int rangeStart = bestRange[0];
         int rangeEnd = bestRange[1];

         // Si no se encontró segmento claro, usar todo el tail
         if (rangeStart < 0 || rangeEnd < rangeStart) {
             rangeStart = 0;
             rangeEnd = allLines.length - 1;
         }
         
         Log.d(TAG, "Rango de items: " + rangeStart + " a " + rangeEnd + " (" + (rangeEnd - rangeStart + 1) + " líneas)");

         // ============ FILTRO DE FOOTER/HEADER MEJORADO ============
         Pattern footerPattern = Pattern.compile("(?i).*(sumas|venta(s)?\\s*(total)?|ventas gravadas|ventas no sujetas|vtas\\.? exentas|autorizaci(o|ó)n|resoluci[oó]n|firma|recibido por|firma cajera|duplicado|nrc|nit|n°|no\\.|iva retenido|retencion|observaci|nota|aclaraci|tasa|interes|por pagar).*");

        for (int idx = rangeStart; idx <= rangeEnd; idx++) {
            String rawLine = allLines[idx];
            if (rawLine == null) continue;
            String line = rawLine.trim();
            if (line.length() < 3) continue;

            // evitar líneas que claramente son footer/header
            if (footerPattern.matcher(line).matches()) continue;

            Matcher mp = pricePattern.matcher(line);
            if (!mp.find()) continue; // sólo procesar líneas con precio al final

            String priceStr = mp.group(1);
            double price = parseAmountLenient(priceStr);
            if (price <= 0) continue;

            String before = line.substring(0, mp.start()).trim();

            // qty heurística
            double qty = 1.0;
            Matcher mq = qtyAtStart.matcher(before);
            if (mq.find()) {
                qty = parseAmountLenient(mq.group(1));
                before = before.substring(mq.end()).trim();
            } else {
                Pattern lastNumBefore = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*$");
                Matcher mln = lastNumBefore.matcher(before);
                if (mln.find()) {
                    double qv = parseAmountLenient(mln.group(1));
                    if (qv > 0 && qv < 1000 && Math.abs(qv - Math.round(qv)) < 0.001) {
                        qty = qv;
                        before = before.substring(0, mln.start()).trim();
                    }
                }
            }

            // remover tokens de código al inicio (11-3-152, 51800414, etc.)
            String desc = stripLeadingCodes(before);

            // Si quedó vacío, intentar rescatar desde líneas previas dentro del segmento (hasta 2 líneas atrás)
            if (desc.isEmpty()) {
                for (int look = 1; look <= 2 && (idx - look) >= rangeStart; look++) {
                    String cand = allLines[idx - look].trim();
                    if (cand.length() > 3 && !footerPattern.matcher(cand).matches()) {
                        cand = cand.replaceAll("^(\\d+\\s*(x|X|\\*)\\s*)", "");
                        desc = stripLeadingCodes(cand);
                        if (!desc.isEmpty()) break;
                    }
                }
            }

            // Lookahead: la descripción puede continuar en la siguiente línea (si la siguiente no tiene precio)
            if (desc.isEmpty() && (idx + 1) <= rangeEnd) {
                String next = allLines[idx + 1].trim();
                if (next.length() > 3 && !pricePattern.matcher(next).find() && !footerPattern.matcher(next).matches()) {
                    desc = stripLeadingCodes(next);
                }
            }

            // Fallbacks: intentar limpiar tokens comunes
            if (desc.isEmpty()) {
                String alt = rawLine.replace(priceStr, "").trim();
                alt = alt.replaceAll("^\\d+\\s*", "").trim();
                alt = alt.replaceAll("(^\\*+|\\bpcs?\\b|\\bund\\b|\\bunit\\b|\\$)","").trim();
                desc = stripLeadingCodes(alt);
            }

            if (desc.isEmpty()) desc = "PRODUCTO";

            HashMap<String,String> item = new HashMap<>();
            item.put("producto", desc);
            if (Math.abs(qty - Math.round(qty)) < 0.001) {
                item.put("cantidad", String.valueOf((int)Math.round(qty)));
            } else {
                item.put("cantidad", String.format(Locale.US, "%.2f", qty));
            }
            item.put("precio", String.format(Locale.US, "%.2f", price));
            res.items.add(item);
            res.subtotal += qty * price;
        }

        return res;
    }

    /** 
     * Detección mejorada y robusta de nombre de empresa.
     * Estrategias en orden de prioridad:
     * 1. Nombre con S.A. de C.V. (Sociedad Anónima)
     * 2. Nombre con Ltd., S.L., Corp., Inc. 
     * 3. Primer bloque de texto (cabecera)
     * 4. Primera línea no-vacía
     */
    private String findCompanyNameWithSA(String texto) {
        if (texto == null || texto.trim().isEmpty()) return null;
        
        Log.d(TAG, "Buscando nombre de empresa...");
        Log.d(TAG, "Primeras 500 caracteres del texto: " + texto.substring(0, Math.min(500, texto.length())));
        
        // ============ ESTRATEGIA 1: S.A. de C.V. y variantes ============
        Pattern saPattern = Pattern.compile("(?im)([A-Z0-9&\\-\\.,\\s]{3,}?\\bS\\.?\\s*A\\.?(?:\\s+de)?\\s+C\\.?\\s*V\\.?\\b)");
        Matcher m = saPattern.matcher(texto);
        if (m.find()) {
            String raw = m.group(1).trim();
            raw = normalizeCompanyName(raw);
            Log.d(TAG, "Empresa detectada (S.A.): " + raw);
            return raw;
        }
        
        // ============ ESTRATEGIA 2: Otras formas legales ============
        Pattern otherLegalForms = Pattern.compile("(?im)([A-Z][A-Za-z0-9&\\-.,\\s]{2,})(?:\\s+(?:Ltd|Limited|S\\.L\\.|SL|Corp|Corporation|Inc|Incorporated|GmbH|AG|Ltda))\\b");
        Matcher m2 = otherLegalForms.matcher(texto);
        if (m2.find()) {
            String raw = m2.group(1).trim();
            raw = normalizeCompanyName(raw);
            Log.d(TAG, "Empresa detectada (Otra forma legal): " + raw);
            return raw;
        }
        
        // ============ ESTRATEGIA 3: Línea anterior a "S.A." o similar ============
        Pattern saShort = Pattern.compile("(?im)^(.*\\S.*)\\r?\\n.*\\b(?:S\\.?\\s*A\\.?|Ltd|Inc|Corp)\\b", Pattern.MULTILINE);
        Matcher ms = saShort.matcher(texto);
        if (ms.find()) {
            String raw = ms.group(1).trim();
            raw = normalizeCompanyName(raw);
            Log.d(TAG, "Empresa detectada (línea anterior): " + raw);
            return raw;
        }
        
        // ============ ESTRATEGIA 4: Bloque inicial de cabecera ============
        // Busca primer bloque antes de palabras clave (FACTURA, FECHA, DIRECCIÓN, NIT, etc.)
        Pattern headerBlock = Pattern.compile("(?is)^((?:[^\\n]{1,}\\n?){1,4})(?:FACTURA|FECHA|DIRECCION|NIT|NRC|N°|No\\.)", Pattern.MULTILINE);
        Matcher mh = headerBlock.matcher(texto);
        if (mh.find()) {
            String[] lineas = mh.group(1).split("\n");
            for (int i = lineas.length - 1; i >= 0; i--) {
                String linea = lineas[i].trim();
                if (!linea.isEmpty() && linea.length() > 3 && !linea.matches("^[0-9\\-\\.]+$")) {
                    String clean = normalizeCompanyName(linea);
                    if (clean.length() > 2) {
                        Log.d(TAG, "Empresa detectada (cabecera): " + clean);
                        return clean;
                    }
                }
            }
        }
        
        // ============ ESTRATEGIA 5: Primera línea significativa ============
        String[] lineas = texto.split("\\r?\\n");
        for (String linea : lineas) {
            String limpia = linea.trim();
            // Saltar líneas muy cortas, números solo, headers
            if (limpia.length() > 3 && 
                !limpia.matches("^[0-9\\-\\.]+$") &&
                !limpia.matches("(?i).*(FACTURA|FECHA|PAGINA|PAGE|CAJA|VENDEDOR|CLIENTE).*") &&
                !isProbableHeaderToken(limpia)) {
                String clean = normalizeCompanyName(limpia);
                if (clean.length() > 2) {
                    Log.d(TAG, "Empresa detectada (primera línea): " + clean);
                    return clean;
                }
            }
        }
        
        Log.d(TAG, "No se detectó empresa, usando fallback");
        return null;  // Devolver null en lugar de "EMPRESA DESCONOCIDA" para control en parsearTextoFactura
    }
    
    /** Helper: normaliza nombre de empresa eliminando caracteres basura y espacios excesivos */
    private String normalizeCompanyName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s{2,}", " ")  // espacios múltiples a uno
                   .replaceAll("\\s*\\.\\s*", ". ")  // puntos con espacios
                   .replaceAll("\\s*,\\s*", ", ")    // comas con espacios
                   .replaceAll("^[\\-_\\*\\s]+|[\\-_\\*\\s]+$", "")  // trim especial
                   .trim();
    }

    /**
     * Selecciona el mejor segmento de líneas donde hay más coincidencias de precio.
     * Devuelve int[]{startIndex, endIndex}. Si no hay segmento claro, devuelve {-1,-1}.
     */
    private int[] selectBestItemsSegment(String[] lines, Pattern pricePattern) {
        int n = lines.length;
        if (n == 0) return new int[]{-1,-1};

        // contiguos: encuentra runs donde hay >= 1 precio, calcula el que tenga mayor countPrice
        int bestStart = -1, bestEnd = -1, bestCount = 0;
        int i = 0;
        while (i < n) {
            // saltar líneas vacías
            while (i < n && (lines[i] == null || lines[i].trim().isEmpty())) i++;
            if (i >= n) break;
            int j = i;
            int countPrice = 0;
            // construir run hasta que tengamos 3+ vacías consecutivas (aprox final de bloque)
            int emptyStreak = 0;
            while (j < n && emptyStreak < 3) {
                String l = lines[j] == null ? "" : lines[j].trim();
                if (l.isEmpty()) {
                    emptyStreak++;
                } else {
                    emptyStreak = 0;
                    Matcher m = pricePattern.matcher(l);
                    if (m.find()) countPrice++;
                }
                j++;
            }
            // Considerar este bloque si tiene al menos 1 precio y preferir mayor countPrice
            if (countPrice > bestCount) {
                bestCount = countPrice;
                bestStart = i;
                bestEnd = j - 1;
            }
            i = j + 1;
        }

        // Si el mejor bloque tiene menos de 2 precios, podemos ser conservadores: aceptar si >=1 y longitud razonable
        if (bestCount >= 1) return new int[]{bestStart, bestEnd};
        return new int[]{-1,-1};
    }



    /** Helper: token que probablemente es un header/label (ej: "DESCRIPCION", "P. UNITARIO", "GARANTIA", etc.) */
     private boolean isProbableHeaderToken(String tok) {
         if (tok == null || tok.trim().isEmpty()) return true;
         String t = tok.trim().toLowerCase();
         // Headers comunes en facturas (excluir palabras muy cortas que pueden ser parte de descripciones)
         return t.matches("(?i).*(descripcion|descripción|p\\.?\\s?unitario|precio\\s+unitario|cant\\.?|cantidad|garantia|garantía|meses|serie|no\\.?|num|factura|cliente|nit|nrc|subtotal|total|iva|impuesto|retencion|retención|firma|pago|fecha|fecha venta|código|code|producto|item|art\\.?|article|unidad|unitario).*");
     }

    /** Devuelve true si el token parece ser un código/product-code (número largo, guiones, / o mucha proporción de dígitos/letras sin vocales). */
    private boolean isCodeToken(String token) {
        if (token == null || token.trim().isEmpty()) return false;
        token = token.trim();
        
        // Patrón 1: números con guiones/barras (ej: 51-2301, 11/3/152)
        if (token.matches("^[0-9\\-\\/\\.]{2,}$")) return true;
        
        // Patrón 2: códigos alfanuméricos sin vocales (ej: SKU123, PRC4567)
        if (token.matches("^[A-Z0-9]{4,}$") && token.matches(".*\\d.*")) {
            // Contar vocales
            long vowels = token.toLowerCase().chars().filter(c -> "aeiou".indexOf(c) >= 0).count();
            if (vowels <= token.length() / 3) return true;
        }
        
        // Patrón 3: números de 2-4 dígitos solos (ej: "51", "1234")
        // PERO: solo si NO está en contexto de línea (revisaré después)
        if (token.matches("^\\d{2,4}$")) {
            // Ser conservador: solo códigos si son exactamente 3-4 dígitos
            return token.length() >= 3;
        }
        
        // Patrón 4: formato típico de SKU/artículo (ej: ART-001-002)
        if (token.contains("-") && token.matches(".*[A-Z].*\\d.*")) return true;
        
        return false;
    }

    /** Elimina tokens de código al principio de una línea y devuelve la parte más descriptiva. */
    private String stripLeadingCodes(String before) {
        if (before == null) return "";
        String work = before.replaceAll("\\s{2,}", " ").trim();
        if (work.isEmpty()) return "";

        String[] tokens = work.split("\\s+");
        int i = 0;
        
        // Saltar códigos al inicio (números con guiones, formato SKU, etc.)
        while (i < tokens.length && isCodeToken(tokens[i])) {
            Log.d(TAG, "Saltando código: " + tokens[i]);
            i++;
        }
        
        // Saltar números simples (cantidades sin unidad) al inicio
        // Solo si es un número pequeño (< 100 probable cantidad)
        while (i < tokens.length && tokens[i].matches("^[\\*xX]?\\d+([.,]\\d+)?$")) {
            double num = parseAmountLenient(tokens[i]);
            // Números pequeños (<100) son probablemente cantidades
            if (num > 0 && num < 100) {
                Log.d(TAG, "Saltando cantidad probable: " + tokens[i]);
                i++;
            } else {
                break;
            }
        }

        // Construir descripción desde tokens no-código/no-header
        StringBuilder sb = new StringBuilder();
        for (int j = i; j < tokens.length; j++) {
            String tok = tokens[j].trim();
            if (!isProbableHeaderToken(tok) && !tok.isEmpty()) {
                sb.append(tok);
                if (j < tokens.length - 1) sb.append(" ");
            }
        }
        
        String desc = sb.toString().trim();
        
        // Fallback 1: si quedó vacío, tomar primer token no-código y no-header
        if (desc.isEmpty()) {
            for (String tok : tokens) {
                if (!isCodeToken(tok) && !isProbableHeaderToken(tok) && 
                    !tok.isEmpty() && !tok.matches("^\\d+([.,]\\d+)?$")) {
                    desc = tok;
                    break;
                }
            }
        }
        
        // Fallback 2: si sigue vacío, tomar cualquier token que no sea puro número
        if (desc.isEmpty()) {
            for (String tok : tokens) {
                if (!tok.matches("^\\d+([.,]\\d+)?$") && tok.length() > 2) {
                    desc = tok;
                    break;
                }
            }
        }
        
        // Limpiar caracteres especiales y espacios extras
        desc = desc.replaceAll("[\\*\\u2022\\t|¦\\\\`^~]+", " ")  // Eliminar símbolos OCR comunes
                   .replaceAll("\\s{2,}", " ")  // espacios múltiples
                   .replaceAll("([a-z])([0-9])|([0-9])([a-z])", "$1$2 $3$4")  // espacios entre letras y números
                   .replaceAll("^[\\-_]+|[\\-_]+$", "")  // remover guiones al inicio/fin
                   .replaceAll("O([0-9])", "0$1")  // O grande confundida con cero
                   .replaceAll("([0-9])l$", "$10")  // l minúscula confundida con 0 al final
                   .trim();
        
        // Validar: rechazar si es solo números o muy corto
        if (desc.matches("^\\d+([.,]\\d+)?$") || desc.length() < 2) return "";
        
        Log.d(TAG, "Descripción limpiada: " + desc);
        return desc;
    }

    /** Normaliza montos variados a double (acepta 1.234,56 / 1,234.56 / 1234.56 etc). */
    private double parseAmountLenient(String raw) {
        if (raw == null) return 0.0;
        String s = raw.trim().replaceAll("[^0-9,\\.\\-]", "");
        if (s.isEmpty()) return 0.0;
        if (s.contains(",") && s.contains(".")) {
            if (s.matches(".*,[0-9]{2}$")) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            s = s.replace(',', '.');
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Helper: Convertir mes texto a número (ej: Junio -> 06)
    private String convertirMesANumero(String mesStr) {
        if (mesStr == null) return "01";
        
        String mes = mesStr.toLowerCase().trim();
        
        // Versiones con acentos y sin
        if (mes.contains("enero")) return "01";
        if (mes.contains("febrero")) return "02";
        if (mes.contains("marzo")) return "03";
        if (mes.contains("abril")) return "04";
        if (mes.contains("mayo")) return "05";
        if (mes.contains("junio")) return "06";
        if (mes.contains("julio")) return "07";
        if (mes.contains("agosto")) return "08";
        if (mes.contains("septiembre") || mes.contains("setiembre")) return "09";
        if (mes.contains("octubre")) return "10";
        if (mes.contains("noviembre")) return "11";
        if (mes.contains("diciembre")) return "12";
        
        // Versiones cortas
        if (mes.contains("ene")) return "01";
        if (mes.contains("feb")) return "02";
        if (mes.contains("mar")) return "03";
        if (mes.contains("abr")) return "04";
        if (mes.contains("may")) return "05";
        if (mes.contains("jun")) return "06";
        if (mes.contains("jul")) return "07";
        if (mes.contains("ago")) return "08";
        if (mes.contains("sep") || mes.contains("set")) return "09";
        if (mes.contains("oct")) return "10";
        if (mes.contains("nov")) return "11";
        if (mes.contains("dic")) return "12";
        
        return "01";  // Default si no hay match
    }

    // Helper: Calcular fecha fin garantía (DD/MM/YYYY + meses)
    private String calcularFechaFinGarantia(String startStr, int meses) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = sdf.parse(startStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            cal.add(Calendar.MONTH, meses);
            return sdf.format(cal.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Error calculando garantía: " + e.getMessage());
            return "";
        }
    }

    // ==================== TESSERACT HELPERS (copiar desde raw, init, run region) ====================

    /** Asegura que los traineddata estén en files/tessdata y luego inicializa Tess en background */
    private void ensureTessDataAndInit() {
        ocrExecutor.submit(() -> {
            prepareTessDataIfNeededFromRaw();
            initTess("eng+spa"); // <<< ajustar si quieres "spa" solamente >>>
        });
    }

    /** Copia traineddata desde res/raw (busca varios nombres probables) a getFilesDir()/tessdata/ */
    private void prepareTessDataIfNeededFromRaw() {
        String destDirPath = getFilesDir().getAbsolutePath() + "/tessdata/";
        File destDir = new File(destDirPath);
        if (!destDir.exists()) destDir.mkdirs();
        // Nombres exactos que mencionaste en raw (con preferencia a "best")
        String[] rawCandidates = new String[] {
                "eng_best.traineddata", "eng.traineddata",
                "spa_best.traineddata", "spa_old_best.traineddata", "spa.traineddata", "spa_old.traineddata"
        };
        for (String lang : TESS_LANGS) {
            File outFile = new File(destDirPath + lang + ".traineddata");
            if (outFile.exists()) {
                Log.d(TAG, "Traineddata ya existe para " + lang);
                continue;
            }
            boolean copied = false;
            for (String cand : rawCandidates) {
                // Solo candidatos que empiecen con el lang (para no copiar spa a eng)
                if (!cand.startsWith(lang)) continue;
                // Identifier: lower, replace non-alnum con _
                String identifier = cand.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                int resId = getResources().getIdentifier(identifier, "raw", getPackageName());
                if (resId == 0) continue;
                try (InputStream is = getResources().openRawResource(resId);
                     OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int r;
                    while ((r = is.read(buffer)) > 0) os.write(buffer, 0, r);
                    os.flush();
                    copied = true;
                    Log.d(TAG, "Copiado traineddata desde raw: " + cand + " -> " + outFile.getAbsolutePath() + " para lang " + lang);
                    break;
                } catch (IOException e) {
                    Log.w(TAG, "No se pudo copiar raw " + cand + ": " + e.getMessage());
                }
            }
            if (!copied) {
                Log.w(TAG, "No se encontró traineddata para idioma: " + lang + " en res/raw. Asegúrate de añadir el archivo con nombre como 'eng_best.traineddata' en res/raw.");
            }
        }
    }

    /** Inicializa TessBaseAPI (llamar después de copiar los datos) */
    private void initTess(String langsCombined) {
        String datapath = getFilesDir().getAbsolutePath() + "/";
        try {
            if (tess == null) {
                tess = new TessBaseAPI();
                boolean ok = tess.init(datapath, langsCombined);
                Log.d(TAG, "Tess init ok? " + ok + " langs=" + langsCombined + " datapath=" + datapath);
                tess.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando Tesseract: " + e.getMessage());
        }
    }

    private void releaseTess() {
        if (tess != null) {
            try {
                tess.end();
            } catch (Exception ignored) {}
            tess = null;
        }
    }

    // ==================== Resto de tu UI / navegación / fragments (sin cambios) ====================

    private void crearFragmentConImagen(String photoPath) {
        EscanearImagenFragment fragmentConImagen = EscanearImagenFragment.newInstance(photoPath);
        getSupportFragmentManager().beginTransaction()
                .replace(FRAGMENT_CONTAINER_ID, fragmentConImagen, "IMAGEN_TAG")
                .commitNow();
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



    private int obtenerColor(int atributo) {
        android.util.TypedValue valorTipeado = new android.util.TypedValue();
        getTheme().resolveAttribute(atributo, valorTipeado, true);
        return valorTipeado.data;
    }

    private void abrirEditorImagen() {
        Intent intent = new Intent(this, CameraEscaneoActivity.class);
        scannerLauncher.launch(intent);
    }

    public void lanzarEscaner() {
        Intent intent = new Intent(this, CameraEscaneoActivity.class);
        scannerLauncher.launch(intent);
    }

    public void mostrarVistaTexto(View view) {
        botonTexto.setBackgroundResource(R.drawable.fondo_seleccionado4);
        botonTexto.setTextColor(getResources().getColor(android.R.color.white, getTheme()));

        botonImagen.setBackgroundResource(android.R.color.transparent);
        botonImagen.setTextColor(obtenerColor(com.google.android.material.R.attr.colorOnSecondaryFixed));

        // Crear fragment de texto y pasar datos
        if (textoFragment == null) {
            textoFragment = new EscanearTextoFragment();
        }

        // Pasar datos a través de Bundle
        Bundle bundle = new Bundle();
        if (datosExtraidos != null) {
            bundle.putSerializable("datos_extraidos", datosExtraidos);
        }
        if (imagenEscaneadaPath != null) {
            bundle.putString("imagen_path", imagenEscaneadaPath);
        }
        if (textoOcrCrudo != null) {
            bundle.putString("ocr_text", textoOcrCrudo);
        }
        textoFragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(FRAGMENT_CONTAINER_ID, textoFragment, "TEXTO_TAG");
        transaction.commit();
        fragmentTagActual = "TEXTO_TAG";

        // Actualizar visibilidad
        fragmentTextoContainer.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.GONE);
    }
}
