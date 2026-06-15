package com.example.projeto_integrador;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.projeto_integrador.network.ApiClient;
import com.example.projeto_integrador.network.ApiConfig;
import com.example.projeto_integrador.session.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DenunciasActivity extends AppCompatActivity {

    private static final String CACHE_PREFS = "denuncias_usuario_cache";
    private static final String CACHE_KEY   = "denuncias_json";

    private ImageButton  buttonBack;
    private LinearLayout containerDenuncias;
    private TextView     textLoading;
    private SessionManager session;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_denuncias);

        session = new SessionManager(this);
        iniciarComponentes();
        buttonBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String cache = lerCache();
        if (cache != null) {
            renderizarDenuncias(cache);
            atualizarEmBackground();
        } else {
            textLoading.setVisibility(View.VISIBLE);
            carregarDenuncias();
        }
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void iniciarComponentes() {
        buttonBack         = findViewById(R.id.buttonBack);
        containerDenuncias = findViewById(R.id.containerDenuncias);
        textLoading        = findViewById(R.id.textLoading);
    }

    // ── Rede ─────────────────────────────────────────────────────────────────

    private void carregarDenuncias() {
        String userId = session.getUserId();
        if (userId.isEmpty()) {
            textLoading.setText("Erro: usuário não autenticado.");
            textLoading.setVisibility(View.VISIBLE);
            return;
        }

        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_POR_USUARIO + "/" + userId,
                response -> {
                    salvarCache(response);
                    textLoading.setVisibility(View.GONE);
                    renderizarDenuncias(response);
                },
                (code, msg) -> {
                    if (lerCache() == null) {
                        textLoading.setText("Erro ao carregar denúncias. Verifique sua conexão.");
                        textLoading.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    private void atualizarEmBackground() {
        String userId = session.getUserId();
        if (userId.isEmpty()) return;

        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_POR_USUARIO + "/" + userId,
                response -> {
                    if (!response.equals(lerCache())) {
                        salvarCache(response);
                        renderizarDenuncias(response);
                    }
                },
                (code, msg) -> { /* falha silenciosa */ }
        );
    }

    // ── Renderização ─────────────────────────────────────────────────────────

    private void renderizarDenuncias(String json) {
        containerDenuncias.removeAllViews();
        textLoading.setVisibility(View.GONE);

        try {
            JSONArray array = new JSONArray(json);

            if (array.length() == 0) {
                textLoading.setText("Você ainda não fez nenhuma denúncia.");
                textLoading.setVisibility(View.VISIBLE);
                return;
            }

            for (int i = 0; i < array.length(); i++) {
                adicionarCardDenuncia(array.getJSONObject(i));
            }

        } catch (JSONException e) {
            limparCache();
            textLoading.setText("Erro ao processar dados.");
            textLoading.setVisibility(View.VISIBLE);
        }
    }

    // ── Card ─────────────────────────────────────────────────────────────────

    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    private void adicionarCardDenuncia(JSONObject denuncia) {
        String titulo    = denuncia.optString("titulo",    "Denúncia");
        String descricao = denuncia.optString("descricao", "");
        String status    = denuncia.optString("status",    "ABERTA");
        double latitude  = denuncia.optDouble("latitude",  -23.0882);
        double longitude = denuncia.optDouble("longitude", -47.2234);
        // Trata null explícito do JSON e campo ausente
        String devolutiva = "";
        if (!denuncia.isNull("devolutiva")) {
            devolutiva = denuncia.optString("devolutiva", "").trim();
        }

        // Imagens
        JSONArray imagensJson = denuncia.optJSONArray("imagens");

        View cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_denuncia, containerDenuncias, false);

        // Views básicas
        TextView     textTitulo    = cardView.findViewById(R.id.textTitulo);
        TextView     textDesc      = cardView.findViewById(R.id.textDescricao);
        LinearLayout layoutStatus  = cardView.findViewById(R.id.layoutStatus);
        View         dotStatus     = cardView.findViewById(R.id.dotStatus);
        TextView     textStatus    = cardView.findViewById(R.id.textStatus);

        // Expansão
        LinearLayout buttonExpandir  = cardView.findViewById(R.id.buttonExpandir);
        ImageView    iconExpandir    = cardView.findViewById(R.id.iconExpandir);
        LinearLayout layoutExpandido = cardView.findViewById(R.id.layoutExpandido);

        // Localização
        TextView textEndereco = cardView.findViewById(R.id.textEndereco);
        MapView  mapMini      = cardView.findViewById(R.id.mapMini);

        // Fotos
        LinearLayout layoutFotos = cardView.findViewById(R.id.layoutFotos);

        // Devolutiva
        LinearLayout layoutDevolutiva = cardView.findViewById(R.id.layoutDevolutiva);
        TextView     textDevolutiva   = cardView.findViewById(R.id.textDevolutiva);

        // ── Preencher ────────────────────────────────────────────────────────

        textTitulo.setText(titulo);
        textDesc.setText(descricao);
        configurarBadgeStatus(status, layoutStatus, dotStatus, textStatus);

        // Endereço e mapa
        textEndereco.setText(obterEndereco(latitude, longitude));
        mapMini.setMultiTouchControls(false);
        mapMini.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);
        GeoPoint local = new GeoPoint(latitude, longitude);
        mapMini.getController().setZoom(16.0);
        mapMini.getController().setCenter(local);
        Marker marker = new Marker(mapMini);
        marker.setPosition(local);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapMini.getOverlays().add(marker);

        // Fotos
        carregarFotos(layoutFotos, imagensJson);

        // Devolutiva
        if (!devolutiva.isEmpty()) {
            textDevolutiva.setText(devolutiva);
            layoutDevolutiva.setVisibility(View.VISIBLE);
        } else {
            layoutDevolutiva.setVisibility(View.GONE);
        }

        // ── Expansão ─────────────────────────────────────────────────────────

        final boolean[] expandido = {false};
        buttonExpandir.setOnClickListener(v -> {
            expandido[0] = !expandido[0];
            if (expandido[0]) {
                layoutExpandido.setVisibility(View.VISIBLE);
                iconExpandir.setRotation(180);
                mapMini.invalidate();
            } else {
                layoutExpandido.setVisibility(View.GONE);
                iconExpandir.setRotation(0);
            }
        });

        containerDenuncias.addView(cardView);
    }

    // ── Fotos ────────────────────────────────────────────────────────────────

    private void carregarFotos(LinearLayout layoutFotos, JSONArray imagensJson) {
        layoutFotos.removeAllViews();

        if (imagensJson == null || imagensJson.length() == 0) {
            TextView semFoto = new TextView(this);
            semFoto.setText("Nenhuma foto anexada.");
            semFoto.setTextColor(0xFF9AA5B1);
            semFoto.setTextSize(13);
            layoutFotos.addView(semFoto);
            return;
        }

        int sizePx   = dpToPx(120);
        int marginPx = dpToPx(10);

        for (int i = 0; i < imagensJson.length(); i++) {
            String url = imagensJson.optString(i);
            if (url.isEmpty()) continue;

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMarginEnd(marginPx);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(0xFFD9E2EC);

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(imageView);

            layoutFotos.addView(imageView);
        }
    }

    // ── Badge de status ───────────────────────────────────────────────────────

    private void configurarBadgeStatus(String status, LinearLayout layout,
                                       View dot, TextView text) {
        int bgColor, fgColor;
        String label;

        switch (status) {
            case "EM_ANALISE":
                bgColor = 0xFFFFF3D6; fgColor = 0xFFF5A623; label = "EM ANÁLISE"; break;
            case "RESOLVIDA":
                bgColor = 0xFFD6F5E0; fgColor = 0xFF27AE60; label = "RESOLVIDA";  break;
            case "ABERTA":
            default:
                bgColor = 0xFFD6EAFF; fgColor = 0xFF2B7DE9; label = "ABERTA";     break;
        }

        layout.setBackgroundColor(bgColor);
        dot.setBackgroundColor(fgColor);
        text.setText(label);
        text.setTextColor(fgColor);
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    private void salvarCache(String json) {
        getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .edit().putString(CACHE_KEY, json).apply();
    }

    private String lerCache() {
        return getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .getString(CACHE_KEY, null);
    }

    private void limparCache() {
        getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .edit().remove(CACHE_KEY).apply();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String obterEndereco(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> enderecos = geocoder.getFromLocation(latitude, longitude, 1);
            if (enderecos != null && !enderecos.isEmpty()) {
                return enderecos.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Endereço não encontrado";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}