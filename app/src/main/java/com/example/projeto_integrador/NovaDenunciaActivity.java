package com.example.projeto_integrador;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Dialog;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import com.example.projeto_integrador.network.ApiClient;
import com.example.projeto_integrador.network.ApiConfig;
import com.example.projeto_integrador.session.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NovaDenunciaActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private EditText editTitulo;
    private Spinner spinnerCategoria;
    private EditText editDescricao;
    private ImageView imagePreview;
    private LinearLayout imageHint;
    private MaterialButton buttonRemoverFoto;
    private TextView localizacao;
    private MapView mapPreview;
    private MaterialCardView mapPreviewCard;
    private Marker previewMarker;
    private MaterialButton buttonEnviar;

    // ── Estado ───────────────────────────────────────────────────────────────
    private boolean temFotoSelecionada = false;
    private double latSelecionada = 0;
    private double lngSelecionada = 0;
    private boolean temLocalizacao = false;
    private Uri fotoUri = null;
    private Bitmap fotoBitmap = null;

    // ── Categorias (id → nome) ───────────────────────────────────────────────
    private final LinkedHashMap<String, String> categoriasMap = new LinkedHashMap<>();
    private final List<String> categoriasNomes = new ArrayList<>();

    // ── Session ──────────────────────────────────────────────────────────────
    private SessionManager session;

    // ── Location ─────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;

    // ── Launchers ────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            fotoUri = result.getData().getData();
                            fotoBitmap = null;
                            mostrarFoto(() -> imagePreview.setImageURI(fotoUri));
                        }
                    });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null
                                && result.getData().getExtras() != null) {
                            Object foto = result.getData().getExtras().get("data");
                            if (foto instanceof Bitmap) {
                                fotoBitmap = (Bitmap) foto;
                                fotoUri = null;
                                mostrarFoto(() -> {
                                    imagePreview.setImageBitmap(fotoBitmap);
                                    imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                });
                            }
                        }
                    });

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nova_denuncia);

        session = new SessionManager(this);

        // ── Bind views ───────────────────────────────────────────────────────
        editTitulo        = findViewById(R.id.tituloDenuncia);
        spinnerCategoria  = findViewById(R.id.tipoDenuncia);
        editDescricao     = findViewById(R.id.descricaoDenuncia);
        localizacao       = findViewById(R.id.localizacao);
        imagePreview      = findViewById(R.id.imagePreview);
        imageHint         = findViewById(R.id.imageHint);
        buttonRemoverFoto = findViewById(R.id.buttonRemoverFoto);
        mapPreview        = findViewById(R.id.mapPreview);
        mapPreviewCard    = findViewById(R.id.mapPreviewCard);
        buttonEnviar      = findViewById(R.id.enviarDenuncia);

        MaterialButton buttonLocalAtual    = findViewById(R.id.buttonLocalAtual);
        MaterialButton buttonSelecionarMapa = findViewById(R.id.buttonSelecionarMapa);
        ScrollView scrollView              = findViewById(R.id.scrollView);

        // ── OSMDroid ─────────────────────────────────────────────────────────
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // ── FusedLocation ─────────────────────────────────────────────────────
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ── Edge-to-edge insets ───────────────────────────────────────────────
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        // ── ActionBar ────────────────────────────────────────────────────────
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nova Denúncia");
        }

        // ── Mini-mapa preview (somente leitura) ───────────────────────────────
        mapPreview.setOnTouchListener((v, event) -> true);

        GeoPoint indaiatuba = new GeoPoint(-23.0882, -47.2234);
        mapPreview.setMultiTouchControls(false);
        mapPreview.getController().setZoom(15.0);
        mapPreview.getController().setCenter(indaiatuba);

        previewMarker = new Marker(mapPreview);
        previewMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        previewMarker.setPosition(indaiatuba);
        mapPreview.getOverlays().add(previewMarker);

        // ── Foto: clique no container abre menu ───────────────────────────────
        findViewById(R.id.imageContainer).setOnClickListener(v -> {
            if (!temFotoSelecionada) {
                abrirMenuImagem();
            }
        });
        imagePreview.setOnClickListener(v -> abrirMenuImagem());

        // ── Botão remover foto ────────────────────────────────────────────────
        buttonRemoverFoto.setOnClickListener(v -> removerFoto());

        // ── Localização ───────────────────────────────────────────────────────
        buttonLocalAtual.setOnClickListener(v -> pegarLocalizacaoAtual());
        buttonSelecionarMapa.setOnClickListener(v -> abrirModalMapa());

        // ── Enviar denúncia ───────────────────────────────────────────────────
        buttonEnviar.setOnClickListener(v -> enviarDenuncia());

        // ── Carregar categorias da API ─────────────────────────────────────────
        carregarCategorias();
    }

    // ─── Categorias ──────────────────────────────────────────────────────────

    private void carregarCategorias() {
        ApiClient.getInstance().get(
                ApiConfig.CATEGORIAS,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);

                        categoriasMap.clear();
                        categoriasNomes.clear();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject cat = array.getJSONObject(i);
                            String id   = cat.getString("id");
                            String nome = cat.getString("nome");
                            categoriasMap.put(id, nome);
                            categoriasNomes.add(nome);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                categoriasNomes
                        );
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );
                        spinnerCategoria.setAdapter(adapter);

                    } catch (JSONException e) {
                        Snackbar.make(
                                findViewById(R.id.main),
                                "Erro ao carregar categorias.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                },
                (code, msg) -> {
                    Snackbar.make(
                            findViewById(R.id.main),
                            "Não foi possível carregar as categorias. Verifique sua conexão.",
                            Snackbar.LENGTH_LONG
                    ).show();
                }
        );
    }

    // ─── Enviar Denúncia ─────────────────────────────────────────────────────

    private void enviarDenuncia() {
        String titulo    = editTitulo.getText().toString().trim();
        String descricao = editDescricao.getText().toString().trim();

        // Validações
        if (TextUtils.isEmpty(titulo)) {
            Snackbar.make(findViewById(R.id.main),
                    "Informe o título da denúncia.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(descricao)) {
            Snackbar.make(findViewById(R.id.main),
                    "Informe a descrição da denúncia.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (!temLocalizacao) {
            Snackbar.make(findViewById(R.id.main),
                    "Selecione a localização da denúncia.", Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (categoriasMap.isEmpty()) {
            Snackbar.make(findViewById(R.id.main),
                    "Categorias ainda carregando. Tente novamente.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Pegar categoriaId pelo índice selecionado
        int selectedIndex = spinnerCategoria.getSelectedItemPosition();
        String categoriaId = new ArrayList<>(categoriasMap.keySet()).get(selectedIndex);

        // Montar campos do multipart
        Map<String, String> textFields = new HashMap<>();
        textFields.put("titulo", titulo);
        textFields.put("descricao", descricao);
        textFields.put("latitude", String.valueOf(latSelecionada));
        textFields.put("longitude", String.valueOf(lngSelecionada));
        textFields.put("usuarioId", session.getUserId());
        textFields.put("categoriaId", categoriaId);

        // Imagem (se houver)
        Map<String, byte[]> fileFields = null;
        Map<String, String> fileNames = null;

        if (temFotoSelecionada) {
            byte[] imageBytes = obterBytesImagem();
            if (imageBytes != null) {
                fileFields = new HashMap<>();
                fileFields.put("imagens", imageBytes);
                fileNames = new HashMap<>();
                fileNames.put("imagens", "foto.jpg");
            }
        }

        // Feedback visual
        buttonEnviar.setEnabled(false);
        buttonEnviar.setText("Enviando...");

        ApiClient.getInstance().postMultipart(
                ApiConfig.DENUNCIAS,
                textFields,
                fileFields,
                fileNames,
                response -> {
                    Snackbar.make(
                            findViewById(R.id.main),
                            "✅ Denúncia enviada com sucesso!",
                            Snackbar.LENGTH_SHORT
                    ).show();

                    // Volta para a tela anterior após breve delay
                    findViewById(R.id.main).postDelayed(this::finish, 1200);
                },
                (code, msg) -> {
                    buttonEnviar.setEnabled(true);
                    buttonEnviar.setText("🚀 Enviar denúncia");

                    String errorMsg;
                    if (code == -1) {
                        errorMsg = "Sem conexão com o servidor.";
                    } else {
                        errorMsg = "Erro ao enviar denúncia (código " + code + ").";
                    }

                    Snackbar.make(
                            findViewById(R.id.main),
                            "❌ " + errorMsg,
                            Snackbar.LENGTH_LONG
                    ).show();
                }
        );
    }

    /**
     * Converte a imagem selecionada (URI ou Bitmap) em array de bytes.
     */
    private byte[] obterBytesImagem() {
        try {
            if (fotoUri != null) {
                InputStream is = getContentResolver().openInputStream(fotoUri);
                if (is != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(data)) != -1) {
                        buffer.write(data, 0, bytesRead);
                    }
                    is.close();
                    return buffer.toByteArray();
                }
            } else if (fotoBitmap != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                fotoBitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Foto ────────────────────────────────────────────────────────────────

    private void mostrarFoto(Runnable aplicarImagem) {
        aplicarImagem.run();

        imagePreview.setPadding(0, 0, 0, 0);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setBackground(null);

        imageHint.setVisibility(View.GONE);
        buttonRemoverFoto.setVisibility(View.VISIBLE);
        temFotoSelecionada = true;
    }

    private void removerFoto() {
        imagePreview.setImageResource(android.R.drawable.ic_menu_camera);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setBackgroundColor(0xFFEEF3F8);
        imagePreview.setPadding(dpToPx(40), dpToPx(40), dpToPx(40), dpToPx(40));

        imageHint.setVisibility(View.VISIBLE);
        buttonRemoverFoto.setVisibility(View.GONE);
        temFotoSelecionada = false;
        fotoUri = null;
        fotoBitmap = null;
    }

    private void abrirMenuImagem() {
        PopupMenu popupMenu = new PopupMenu(this, imagePreview);
        popupMenu.getMenu().add("📷 Tirar foto");
        popupMenu.getMenu().add("🖼 Escolher da galeria");

        popupMenu.setOnMenuItemClickListener(item -> {
            String texto = Objects.requireNonNull(item.getTitle()).toString();

            if (texto.contains("Tirar")) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.CAMERA},
                            200);
                    return true;
                }
                cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
            } else {
                Intent galeriaIntent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galeriaLauncher.launch(galeriaIntent);
            }
            return true;
        });

        popupMenu.show();
    }

    // ─── Localização ─────────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void pegarLocalizacaoAtual() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latSelecionada = location.getLatitude();
                lngSelecionada = location.getLongitude();
                temLocalizacao = true;
                GeoPoint ponto = new GeoPoint(latSelecionada, lngSelecionada);

                String endereco = obterEndereco(latSelecionada, lngSelecionada);

                localizacao.setText("📍 " + endereco);

                mapPreviewCard.setVisibility(View.VISIBLE);
                mapPreview.getController().setCenter(ponto);
                previewMarker.setPosition(ponto);
                mapPreview.invalidate();
            } else {
                localizacao.setText("Não foi possível obter a localização");
            }
        });
    }

    // ─── Modal de mapa ───────────────────────────────────────────────────────

    @SuppressLint("SetTextI18n")
    private void abrirModalMapa() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_map);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        MapView mapDialog      = dialog.findViewById(R.id.mapDialog);
        MaterialButton buttonConfirmar =
                dialog.findViewById(R.id.buttonConfirmarLocalizacao);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapDialog.setMultiTouchControls(true);

        GeoPoint indaiatuba = new GeoPoint(-23.0882, -47.2234);
        mapDialog.getController().setZoom(15.0);
        mapDialog.getController().setCenter(indaiatuba);

        Marker marker = new Marker(mapDialog);
        marker.setPosition(indaiatuba);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapDialog.getOverlays().add(marker);

        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                marker.setPosition(p);

                latSelecionada = p.getLatitude();
                lngSelecionada = p.getLongitude();
                temLocalizacao = true;

                String endereco = obterEndereco(
                        p.getLatitude(),
                        p.getLongitude()
                );

                localizacao.setText("📍 " + endereco);

                mapPreviewCard.setVisibility(View.VISIBLE);
                mapPreview.getController().setCenter(p);
                previewMarker.setPosition(p);
                mapPreview.invalidate();
                mapDialog.invalidate();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapDialog.getOverlays().add(new MapEventsOverlay(receiver));
        buttonConfirmar.setOnClickListener(v -> dialog.dismiss());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private String obterEndereco(double latitude, double longitude) {

        try {

            Geocoder geocoder = new Geocoder(
                    this,
                    Locale.getDefault()
            );

            List<Address> enderecos =
                    geocoder.getFromLocation(latitude, longitude, 1);

            if (enderecos != null && !enderecos.isEmpty()) {

                Address endereco = enderecos.get(0);

                String rua = endereco.getThoroughfare();
                String numero = endereco.getSubThoroughfare();
                String cidade = endereco.getSubAdminArea();
                String estado = endereco.getAdminArea();

                StringBuilder resultado = new StringBuilder();

                if (rua != null) {
                    resultado.append(rua);

                    if (numero != null) {
                        resultado.append(", ").append(numero);
                    }
                }

                if (cidade != null) {

                    if (resultado.length() > 0) {
                        resultado.append(" - ");
                    }

                    resultado.append(cidade);
                }

                if (estado != null) {
                    resultado.append("/").append(estado);
                }

                return resultado.toString();
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        return "Localização não encontrada";
    }
}