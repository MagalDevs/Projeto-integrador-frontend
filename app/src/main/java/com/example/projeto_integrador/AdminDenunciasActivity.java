package com.example.projeto_integrador;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.projeto_integrador.network.ApiClient;
import com.example.projeto_integrador.network.ApiConfig;
import com.example.projeto_integrador.session.SessionManager;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminDenunciasActivity extends AppCompatActivity {

    private static final String CACHE_PREFS = "denuncias_cache";
    private static final String CACHE_KEY   = "denuncias_json";
    private LinearLayout containerDenuncias;
    private TextView     textLoading;
    private MaterialButton buttonLogout;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_denuncias);
        iniciarComponentes();
        configurarEventos();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String cache = lerCache();

        if (cache != null) {
            // 1. Mostra cache imediatamente — sem spinner
            renderizarDenuncias(cache, false);
            // 2. Atualiza em background silenciosamente
            atualizarEmBackground();
        } else {
            // Primeira abertura: mostra spinner e carrega normalmente
            textLoading.setVisibility(View.VISIBLE);
            carregarDenuncias(true);
        }
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void iniciarComponentes() {
        buttonLogout = findViewById(R.id.buttonLogout);
        containerDenuncias = findViewById(R.id.containerDenuncias);
        textLoading        = findViewById(R.id.textLoading);
    }

    private void configurarEventos() {
        buttonLogout.setOnClickListener(v -> {
            new SessionManager(this).logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ── Carregamento ─────────────────────────────────────────────────────────

    /**
     * Carregamento normal (primeira vez): mostra spinner, salva cache e renderiza.
     */
    private void carregarDenuncias(boolean mostrarSpinner) {
        if (mostrarSpinner) {
            textLoading.setVisibility(View.VISIBLE);
        }

        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_ADMIN,
                response -> {
                    salvarCache(response);
                    textLoading.setVisibility(View.GONE);
                    renderizarDenuncias(response, true);
                },
                (code, msg) -> {
                    textLoading.setVisibility(View.GONE);
                    if (lerCache() == null) {
                        // Só mostra erro se não há nada para exibir
                        textLoading.setText("Erro ao carregar denúncias. Verifique sua conexão.");
                        textLoading.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    /**
     * Atualização silenciosa em background.
     * Re-renderiza apenas se o JSON mudou desde o cache.
     */
    private void atualizarEmBackground() {
        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_ADMIN,
                response -> {
                    String cacheAtual = lerCache();
                    // Só re-renderiza se houver mudança real nos dados
                    if (!response.equals(cacheAtual)) {
                        salvarCache(response);
                        renderizarDenuncias(response, true);
                    }
                },
                (code, msg) -> { /* falha silenciosa — cache continua visível */ }
        );
    }

    // ── Renderização ─────────────────────────────────────────────────────────

    private void renderizarDenuncias(String json, boolean limparAntes) {
        try {
            JSONArray array = new JSONArray(json);

            if (limparAntes) {
                containerDenuncias.removeAllViews();
            }

            if (array.length() == 0) {
                textLoading.setText("Nenhuma denúncia encontrada.");
                textLoading.setVisibility(View.VISIBLE);
                return;
            }

            textLoading.setVisibility(View.GONE);
            containerDenuncias.removeAllViews(); // garante sem duplicatas

            for (int i = 0; i < array.length(); i++) {
                adicionarCardDenuncia(array.getJSONObject(i));
            }

        } catch (JSONException e) {
            // JSON corrompido — limpa cache e força reload
            limparCache();
            textLoading.setText("Erro ao processar dados.");
            textLoading.setVisibility(View.VISIBLE);
        }
    }

    // ── Cache ────────────────────────────────────────────────────────────────

    private void salvarCache(String json) {
        getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(CACHE_KEY, json)
                .apply();
    }

    private String lerCache() {
        return getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .getString(CACHE_KEY, null);
    }

    private void limparCache() {
        getSharedPreferences(CACHE_PREFS, MODE_PRIVATE)
                .edit()
                .remove(CACHE_KEY)
                .apply();
    }

    // ── Cards ────────────────────────────────────────────────────────────────

    private void adicionarCardDenuncia(JSONObject denuncia) throws JSONException {
        String id        = denuncia.getString("id");
        String titulo    = denuncia.optString("titulo", "Denúncia");
        String descricao = denuncia.optString("descricao", "");
        String status    = denuncia.optString("status", "ABERTA");
        double latitude  = denuncia.optDouble("latitude", -23.0882);
        double longitude = denuncia.optDouble("longitude", -47.2234);
        String createdAt = denuncia.optString("createdAt", "");

        // Imagens
        JSONArray imagensJson = denuncia.optJSONArray("imagens");
        ArrayList<String> imagens = new ArrayList<>();
        if (imagensJson != null) {
            for (int j = 0; j < imagensJson.length(); j++) {
                imagens.add(imagensJson.optString(j));
            }
        }
        final ArrayList<String> imagensFinal = imagens;

        // Usuário
        String cidadaoNome = "Cidadão";
        JSONObject usuario = denuncia.optJSONObject("usuario");
        if (usuario != null) {
            cidadaoNome = usuario.optString("nome", "Cidadão");
        }
        final String cidadao = cidadaoNome;

        String dataFormatada = formatarData(createdAt);

        // CardView
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dpToPx(18);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dpToPx(20));
        card.setCardElevation(dpToPx(5));

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18));

        // Título
        TextView textTitulo = new TextView(this);
        textTitulo.setText(titulo);
        textTitulo.setTextColor(0xFF1A3A5C);
        textTitulo.setTextSize(20);
        textTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        innerLayout.addView(textTitulo);

        // Cidadão + Data
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = dpToPx(8);
        infoRow.setLayoutParams(infoParams);

        TextView iconCidadao = new TextView(this);
        iconCidadao.setText("👤");
        iconCidadao.setTextSize(14);
        infoRow.addView(iconCidadao);

        TextView textCidadaoView = new TextView(this);
        textCidadaoView.setText(cidadao);
        textCidadaoView.setTextColor(0xFF5F6F81);
        textCidadaoView.setTextSize(13);
        textCidadaoView.setPadding(dpToPx(6), 0, 0, 0);
        infoRow.addView(textCidadaoView);

        TextView iconData = new TextView(this);
        iconData.setText("📅");
        iconData.setTextSize(14);
        iconData.setPadding(dpToPx(16), 0, 0, 0);
        infoRow.addView(iconData);

        TextView textDataView = new TextView(this);
        textDataView.setText(dataFormatada);
        textDataView.setTextColor(0xFF5F6F81);
        textDataView.setTextSize(13);
        textDataView.setPadding(dpToPx(6), 0, 0, 0);
        infoRow.addView(textDataView);
        innerLayout.addView(infoRow);

        // Descrição
        TextView textDesc = new TextView(this);
        textDesc.setText(descricao);
        textDesc.setTextColor(0xFF5F6F81);
        textDesc.setTextSize(14);
        textDesc.setMaxLines(2);
        textDesc.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = dpToPx(10);
        textDesc.setLayoutParams(descParams);
        innerLayout.addView(textDesc);

        // Badge status
        LinearLayout statusBadge = criarBadgeStatus(status);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.topMargin = dpToPx(14);
        statusBadge.setLayoutParams(badgeParams);
        innerLayout.addView(statusBadge);

        card.addView(innerLayout);

        // Click
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDetalheDenunciaActivity.class);
            intent.putExtra("denunciaId",  id);
            intent.putExtra("tipo",        titulo);
            intent.putExtra("cidadao",     cidadao);
            intent.putExtra("data",        dataFormatada);
            intent.putExtra("descricao",   descricao);
            intent.putExtra("status",      formatarStatusParaDisplay(status));
            intent.putExtra("latitude",    latitude);
            intent.putExtra("longitude",   longitude);
            intent.putStringArrayListExtra("imagens", imagensFinal);
            startActivity(intent);
        });

        containerDenuncias.addView(card);
    }

    private LinearLayout criarBadgeStatus(String status) {
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        View dot = new View(this);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)));

        TextView text = new TextView(this);
        text.setTextSize(12);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(dpToPx(8), 0, 0, 0);

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

        badge.setBackgroundColor(bgColor);
        dot.setBackgroundColor(fgColor);
        text.setText(label);
        text.setTextColor(fgColor);
        badge.addView(dot);
        badge.addView(text);
        return badge;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String formatarStatusParaDisplay(String status) {
        switch (status) {
            case "EM_ANALISE":
                return "EM ANÁLISE";
            case "RESOLVIDA":
                return "RESOLVIDA";
            default:
                return "ABERTA";
        }
    }

    private String formatarData(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = iso.parse(isoDate.substring(0, Math.min(19, isoDate.length())));
            if (date != null) {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            if (isoDate.length() >= 10) {
                return isoDate.substring(8, 10) + "/" + isoDate.substring(5, 7) + "/" + isoDate.substring(0, 4);
            }
        }
        return isoDate;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}