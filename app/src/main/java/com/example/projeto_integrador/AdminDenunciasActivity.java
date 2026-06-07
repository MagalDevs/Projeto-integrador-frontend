package com.example.projeto_integrador;

import android.content.Intent;
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
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDenunciasActivity extends AppCompatActivity {

    private ImageButton buttonBack;
    private LinearLayout containerDenuncias;
    private TextView textLoading;

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
        carregarDenuncias();
    }

    private void iniciarComponentes() {
        buttonBack         = findViewById(R.id.buttonBack);
        containerDenuncias = findViewById(R.id.containerDenuncias);
        textLoading        = findViewById(R.id.textLoading);
    }

    private void configurarEventos() {
        // BOTÃO VOLTAR
        buttonBack.setOnClickListener(v -> {
            // Logout e volta para login
            SessionManager session = new SessionManager(this);
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void carregarDenuncias() {
        textLoading.setVisibility(View.VISIBLE);
        containerDenuncias.removeAllViews();

        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_ADMIN,
                response -> {
                    textLoading.setVisibility(View.GONE);

                    try {
                        JSONArray array = new JSONArray(response);

                        if (array.length() == 0) {
                            textLoading.setText("Nenhuma denúncia encontrada.");
                            textLoading.setVisibility(View.VISIBLE);
                            return;
                        }

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject denuncia = array.getJSONObject(i);
                            adicionarCardDenuncia(denuncia);
                        }

                    } catch (JSONException e) {
                        textLoading.setText("Erro ao processar dados.");
                        textLoading.setVisibility(View.VISIBLE);
                    }
                },
                (code, msg) -> {
                    textLoading.setText("Erro ao carregar denúncias. Verifique sua conexão.");
                    textLoading.setVisibility(View.VISIBLE);
                }
        );
    }

    /**
     * Cria um card programaticamente mantendo o mesmo visual dos cards estáticos anteriores.
     */
    private void adicionarCardDenuncia(JSONObject denuncia) throws JSONException {
        String id        = denuncia.getString("id");
        String titulo    = denuncia.optString("titulo", "Denúncia");
        String descricao = denuncia.optString("descricao", "");
        String status    = denuncia.optString("status", "ABERTA");
        double latitude  = denuncia.optDouble("latitude", -23.0882);
        double longitude = denuncia.optDouble("longitude", -47.2234);
        String createdAt = denuncia.optString("createdAt", "");

        // Dados do usuário
        String cidadao = "Cidadão";
        JSONObject usuario = denuncia.optJSONObject("usuario");
        if (usuario != null) {
            cidadao = usuario.optString("nome", "Cidadão");
        }

        // Dados da categoria
        String categoriaNome = "";
        JSONObject categoria = denuncia.optJSONObject("categoria");
        if (categoria != null) {
            categoriaNome = categoria.optString("nome", "");
        }

        // Formatar data
        String dataFormatada = formatarData(createdAt);

        // ── Construir card ───────────────────────────────────────────────────

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

        // Container interno
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

        // Cidadão + Data (linha horizontal)
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.topMargin = dpToPx(8);
        infoRow.setLayoutParams(infoParams);

        // Ícone cidadão
        TextView iconCidadao = new TextView(this);
        iconCidadao.setText("👤");
        iconCidadao.setTextSize(14);
        infoRow.addView(iconCidadao);

        // Nome cidadão
        TextView textCidadao = new TextView(this);
        textCidadao.setText(cidadao);
        textCidadao.setTextColor(0xFF5F6F81);
        textCidadao.setTextSize(13);
        textCidadao.setPadding(dpToPx(6), 0, 0, 0);
        infoRow.addView(textCidadao);

        // Ícone data
        TextView iconData = new TextView(this);
        iconData.setText("📅");
        iconData.setTextSize(14);
        iconData.setPadding(dpToPx(16), 0, 0, 0);
        infoRow.addView(iconData);

        // Data
        TextView textData = new TextView(this);
        textData.setText(dataFormatada);
        textData.setTextColor(0xFF5F6F81);
        textData.setTextSize(13);
        textData.setPadding(dpToPx(6), 0, 0, 0);
        infoRow.addView(textData);

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

        // Badge de status
        LinearLayout statusBadge = criarBadgeStatus(status);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        badgeParams.topMargin = dpToPx(14);
        statusBadge.setLayoutParams(badgeParams);
        innerLayout.addView(statusBadge);

        card.addView(innerLayout);

        // ── Click listener ─────────────────────────────────────────────────
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AdminDenunciasActivity.this,
                    AdminDetalheDenunciaActivity.class
            );

            intent.putExtra("denunciaId", id);
            intent.putExtra("tipo", titulo);
            intent.putExtra("cidadao", cidadao);
            intent.putExtra("data", dataFormatada);
            intent.putExtra("descricao", descricao);
            intent.putExtra("status", formatarStatusParaDisplay(status));
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);

            startActivity(intent);
        });

        containerDenuncias.addView(card);
    }

    /**
     * Cria o badge visual de status (dot + texto), mantendo o estilo original.
     */
    private LinearLayout criarBadgeStatus(String status) {
        LinearLayout badge = new LinearLayout(this);
        badge.setOrientation(LinearLayout.HORIZONTAL);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dot.setLayoutParams(dotParams);

        TextView text = new TextView(this);
        text.setTextSize(12);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(dpToPx(8), 0, 0, 0);

        int bgColor, fgColor;
        String label;

        switch (status) {
            case "EM_ANALISE":
                bgColor = 0xFFFFF3D6;
                fgColor = 0xFFF5A623;
                label = "EM ANÁLISE";
                break;
            case "RESOLVIDA":
                bgColor = 0xFFD6F5E0;
                fgColor = 0xFF27AE60;
                label = "RESOLVIDA";
                break;
            case "ABERTA":
            default:
                bgColor = 0xFFD6EAFF;
                fgColor = 0xFF2B7DE9;
                label = "ABERTA";
                break;
        }

        badge.setBackgroundColor(bgColor);
        dot.setBackgroundColor(fgColor);
        text.setText(label);
        text.setTextColor(fgColor);

        badge.addView(dot);
        badge.addView(text);

        return badge;
    }

    /**
     * Converte enum do backend para texto de display.
     */
    private String formatarStatusParaDisplay(String status) {
        switch (status) {
            case "EM_ANALISE": return "EM ANÁLISE";
            case "RESOLVIDA":  return "CONCLUÍDO";
            case "ABERTA":     return "EM ANÁLISE";
            default:           return status;
        }
    }

    /**
     * Formata data ISO 8601 para dd/MM/yyyy.
     */
    private String formatarData(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";

        try {
            // Tentar formatos ISO comuns
            SimpleDateFormat isoFormat = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()
            );
            Date date = isoFormat.parse(isoDate.substring(0, Math.min(19, isoDate.length())));
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat(
                        "dd/MM/yyyy", Locale.getDefault()
                );
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            // fallback: retorna os primeiros 10 chars (yyyy-MM-dd)
            if (isoDate.length() >= 10) {
                return isoDate.substring(8, 10) + "/" + isoDate.substring(5, 7) + "/" + isoDate.substring(0, 4);
            }
        }

        return isoDate;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
