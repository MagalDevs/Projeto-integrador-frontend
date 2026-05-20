package com.example.projeto_integrador;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class NovaDenunciaActivity extends AppCompatActivity {

    // ── Views ────────────────────────────────────────────────────────────────
    private ImageView imagePreview;
    private LinearLayout imageHint;          // hint "toque para adicionar"
    private MaterialButton buttonRemoverFoto; // botão ✕ sobreposto na foto
    private TextView localizacao;
    private MapView mapPreview;
    private MaterialCardView mapPreviewCard;
    private Marker previewMarker;

    // ── Estado ───────────────────────────────────────────────────────────────
    private boolean temFotoSelecionada = false;

    // ── Location ─────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;

    // ── Launchers ────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galeriaLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            mostrarFoto(() -> imagePreview.setImageURI(uri));
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
                                Bitmap bitmap = (Bitmap) foto;
                                mostrarFoto(() -> {
                                    imagePreview.setImageBitmap(bitmap);
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

        // ── Bind views ───────────────────────────────────────────────────────
        localizacao       = findViewById(R.id.localizacao);
        imagePreview      = findViewById(R.id.imagePreview);
        imageHint         = findViewById(R.id.imageHint);
        buttonRemoverFoto = findViewById(R.id.buttonRemoverFoto);
        mapPreview        = findViewById(R.id.mapPreview);
        mapPreviewCard    = findViewById(R.id.mapPreviewCard);

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
        // Retornar `true` no OnTouchListener consome o evento antes que ele
        // chegue ao ScrollView pai, impedindo que a tela role ao tocar no mapa.
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
            // Só abre o menu se não houver foto; o botão ✕ cuida da remoção.
            if (!temFotoSelecionada) {
                abrirMenuImagem();
            }
        });

        // Clique direto na imagem também abre o menu (quando há foto, troca)
        imagePreview.setOnClickListener(v -> abrirMenuImagem());

        // ── Botão remover foto ────────────────────────────────────────────────
        buttonRemoverFoto.setOnClickListener(v -> removerFoto());

        // ── Localização ───────────────────────────────────────────────────────
        buttonLocalAtual.setOnClickListener(v -> pegarLocalizacaoAtual());
        buttonSelecionarMapa.setOnClickListener(v -> abrirModalMapa());
    }

    // ─── Foto ────────────────────────────────────────────────────────────────

    /**
     * Aplica a imagem na ImageView e atualiza o estado de UI para
     * "foto selecionada": sem padding, sem hint, botão ✕ visível.
     *
     * @param aplicarImagem lambda que seta a fonte da imagem
     */
    private void mostrarFoto(Runnable aplicarImagem) {
        aplicarImagem.run();

        imagePreview.setPadding(0, 0, 0, 0);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setBackground(null);

        imageHint.setVisibility(View.GONE);
        buttonRemoverFoto.setVisibility(View.VISIBLE);
        temFotoSelecionada = true;
    }

    /**
     * Remove a foto e restaura o estado inicial (ícone de câmera + hint).
     */
    private void removerFoto() {
        imagePreview.setImageResource(android.R.drawable.ic_menu_camera);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setBackgroundColor(0xFFEEF3F8);
        imagePreview.setPadding(dpToPx(40), dpToPx(40), dpToPx(40), dpToPx(40));

        imageHint.setVisibility(View.VISIBLE);
        buttonRemoverFoto.setVisibility(View.GONE);
        temFotoSelecionada = false;
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
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                GeoPoint ponto = new GeoPoint(lat, lng);

                String endereco = obterEndereco(lat, lng);

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