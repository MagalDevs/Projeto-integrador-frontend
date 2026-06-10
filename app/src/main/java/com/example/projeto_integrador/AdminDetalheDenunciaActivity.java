package com.example.projeto_integrador;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.projeto_integrador.network.ApiClient;
import com.example.projeto_integrador.network.ApiConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDetalheDenunciaActivity extends AppCompatActivity {

    private ImageButton buttonBack;

    // Informações Gerais
    private TextView textTipo;
    private TextView textCidadao;
    private TextView textData;
    private TextView textDescricao;
    private LinearLayout layoutStatusBadge;
    private View dotStatus;
    private TextView textStatusBadge;

    // Localização
    private TextView textEndereco;
    private MapView mapMini;

    // Status
    private Spinner spinnerStatus;
    private MaterialButton buttonSalvarStatus;

    // Devolutiva
    private CardView cardDevolutiva;
    private EditText editDevolutiva;
    private MaterialButton buttonEnviarDevolutiva;

    // Barra inferior
    private MaterialButton buttonVoltarLista;
    private MaterialButton buttonSalvarAlteracoes;

    // Dados
    private String denunciaId;
    private double latitude;
    private double longitude;
    private String statusAtual;
    private LinearLayout layoutFotos;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_detalhe_denuncia);

        iniciarComponentes();

        carregarDadosDaIntent();

        configurarMapa();

        configurarEventos();
    }

    private void iniciarComponentes() {

        buttonBack = findViewById(R.id.buttonBack);

        // Informações Gerais
        textTipo = findViewById(R.id.textTipo);
        textCidadao = findViewById(R.id.textCidadao);
        textData = findViewById(R.id.textData);
        textDescricao = findViewById(R.id.textDescricao);
        layoutStatusBadge = findViewById(R.id.layoutStatusBadge);
        dotStatus = findViewById(R.id.dotStatus);
        textStatusBadge = findViewById(R.id.textStatusBadge);

        // Localização
        textEndereco = findViewById(R.id.textEndereco);
        mapMini = findViewById(R.id.mapMini);

        // Status
        spinnerStatus = findViewById(R.id.spinnerStatus);
        buttonSalvarStatus = findViewById(R.id.buttonSalvarStatus);

        // Devolutiva
        cardDevolutiva = findViewById(R.id.cardDevolutiva);
        editDevolutiva = findViewById(R.id.editDevolutiva);
        buttonEnviarDevolutiva = findViewById(R.id.buttonEnviarDevolutiva);

        // Barra inferior
        buttonVoltarLista = findViewById(R.id.buttonVoltarLista);
        buttonSalvarAlteracoes = findViewById(R.id.buttonSalvarAlteracoes);

        //fotos
        layoutFotos = findViewById(R.id.layoutFotos);
    }

    private void carregarDadosDaIntent() {

        // Recebe dados passados pelo AdminDenunciasActivity
        denunciaId = getIntent().getStringExtra("denunciaId");
        String tipo = getIntent().getStringExtra("tipo");
        String cidadao = getIntent().getStringExtra("cidadao");
        String data = getIntent().getStringExtra("data");
        String descricao = getIntent().getStringExtra("descricao");
        statusAtual = getIntent().getStringExtra("status");
        latitude = getIntent().getDoubleExtra("latitude", -23.0882);
        longitude = getIntent().getDoubleExtra("longitude", -47.2234);
        ArrayList<String> imagens = getIntent().getStringArrayListExtra("imagens");
        carregarImagens(imagens);

        // Preenche os campos
        if (tipo != null) textTipo.setText(tipo);
        if (cidadao != null) textCidadao.setText(cidadao);
        if (data != null) textData.setText(data);
        if (descricao != null) textDescricao.setText(descricao);

        // Atualiza badge de status
        if (statusAtual != null) {
            atualizarBadgeStatus(statusAtual);
            selecionarStatusNoSpinner(statusAtual);
        }

        // Geocodificação reversa
        String endereco = obterEndereco(latitude, longitude);
        textEndereco.setText(endereco);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarMapa() {

        mapMini.setMultiTouchControls(false);
        mapMini.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);

        GeoPoint local = new GeoPoint(latitude, longitude);

        mapMini.getController().setZoom(16.0);
        mapMini.getController().setCenter(local);

        Marker marker = new Marker(mapMini);

        marker.setPosition(local);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        mapMini.getOverlays().add(marker);
    }

    private void configurarEventos() {

        // BOTÃO VOLTAR (header)
        buttonBack.setOnClickListener(v -> finish());

        // BOTÃO VOLTAR (barra inferior)
        buttonVoltarLista.setOnClickListener(v -> finish());

        // SPINNER — detecta mudança de status
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String novoStatus = parent.getItemAtPosition(position).toString();
                // Mostra devolutiva só ao marcar RESOLVIDA
                cardDevolutiva.setVisibility(
                        novoStatus.equals("RESOLVIDA") ? View.VISIBLE : View.GONE
                );
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // BOTÃO SALVAR STATUS
        buttonSalvarStatus.setOnClickListener(v -> salvarStatusViaApi());

        // BOTÃO ENVIAR DEVOLUTIVA
        // BOTÃO ENVIAR DEVOLUTIVA
        buttonEnviarDevolutiva.setOnClickListener(v -> enviarDevolutiva());

        // BOTÃO SALVAR TUDO (barra inferior)
        buttonSalvarAlteracoes.setOnClickListener(v -> salvarStatusViaApi());
    }

    /**
     * Envia atualização de status para o backend via PATCH.
     * Mapeia o texto do spinner para o enum do backend.
     */
    private void salvarStatusViaApi() {
        String novoStatusDisplay = spinnerStatus.getSelectedItem().toString();
        String statusEnum = mapearStatusParaEnum(novoStatusDisplay);

        if (denunciaId == null || denunciaId.isEmpty()) {
            Snackbar.make(
                    findViewById(R.id.main),
                    "❌ ID da denúncia não encontrado.",
                    Snackbar.LENGTH_SHORT
            ).show();
            return;
        }

        // Desabilita botões durante a chamada
        buttonSalvarStatus.setEnabled(false);
        buttonSalvarAlteracoes.setEnabled(false);

        String path = ApiConfig.DENUNCIAS + "/" + denunciaId
                + ApiConfig.DENUNCIAS_UPDATE_STATUS + "/" + statusEnum;

        ApiClient.getInstance().patchNoBody(
                path,
                response -> {
                    statusAtual = novoStatusDisplay;
                    atualizarBadgeStatus(novoStatusDisplay);

                    buttonSalvarStatus.setEnabled(true);
                    buttonSalvarAlteracoes.setEnabled(true);

                    Snackbar.make(
                            findViewById(R.id.main),
                            "✅ Status atualizado para: " + novoStatusDisplay,
                            Snackbar.LENGTH_SHORT
                    ).show();
                },
                (code, msg) -> {
                    buttonSalvarStatus.setEnabled(true);
                    buttonSalvarAlteracoes.setEnabled(true);

                    String errorMsg;
                    if (code == -1) {
                        errorMsg = "Sem conexão com o servidor.";
                    } else {
                        errorMsg = "Erro ao atualizar status (código " + code + ").";
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
     * Mapeia o texto de display do app para o enum do backend.
     *
     * Display (App)     → Enum (Backend)
     * "EM ANÁLISE"      → "EM_ANALISE"
     * "EM ANDAMENTO"    → "EM_ANALISE"
     * "CONCLUÍDO"       → "RESOLVIDA"
     */
    private String mapearStatusParaEnum(String displayStatus) {
        switch (displayStatus) {
            case "EM ANÁLISE":
                return "EM_ANALISE";
            case "RESOLVIDA":
                return "RESOLVIDA";
            case "ABERTA":
            default:
                return "ABERTA";
        }
    }

    /**
     * Atualiza visualmente o badge de status com as cores correspondentes.
     */
    private void atualizarBadgeStatus(String status) {
        switch (status) {
            case "ABERTA":
                layoutStatusBadge.setBackgroundColor(0xFFD6EAFF);
                dotStatus.setBackgroundColor(0xFF2B7DE9);
                textStatusBadge.setText("ABERTA");
                textStatusBadge.setTextColor(0xFF2B7DE9);
                break;
            case "EM ANÁLISE":
                layoutStatusBadge.setBackgroundColor(0xFFFFF3D6);
                dotStatus.setBackgroundColor(0xFFF5A623);
                textStatusBadge.setText("EM ANÁLISE");
                textStatusBadge.setTextColor(0xFFF5A623);
                break;
            case "RESOLVIDA":
            case "CONCLUÍDO":
                layoutStatusBadge.setBackgroundColor(0xFFD6F5E0);
                dotStatus.setBackgroundColor(0xFF27AE60);
                textStatusBadge.setText("RESOLVIDA");
                textStatusBadge.setTextColor(0xFF27AE60);
                break;
        }
    }

    /**
     * Seleciona o status correto no Spinner baseado na string recebida.
     */
    private void selecionarStatusNoSpinner(String status) {
        // Converte enum do backend para o texto do spinner
        String displayStatus;
        switch (status) {
            case "EM_ANALISE": displayStatus = "EM ANÁLISE"; break;
            case "RESOLVIDA":  displayStatus = "RESOLVIDA";  break;
            default:           displayStatus = "ABERTA";     break;
        }

        String[] statusArray = getResources().getStringArray(R.array.status_denuncia);
        for (int i = 0; i < statusArray.length; i++) {
            if (statusArray[i].equals(displayStatus)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mapMini != null) {
            mapMini.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mapMini != null) {
            mapMini.onPause();
        }
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

                return endereco.getAddressLine(0);
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        return "Endereço não encontrado";
    }

    private void carregarImagens(ArrayList<String> imagens) {
        layoutFotos.removeAllViews();

        if (imagens == null || imagens.isEmpty()) {
            TextView semFoto = new TextView(this);
            semFoto.setText("Nenhuma foto anexada.");
            semFoto.setTextColor(0xFF9AA5B1);
            semFoto.setTextSize(13);
            layoutFotos.addView(semFoto);
            return;
        }

        int sizePx   = dpToPx(140);
        int marginPx = dpToPx(12);

        for (String url : imagens) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void enviarDevolutiva() {
        String devolutiva = editDevolutiva.getText().toString().trim();

        if (devolutiva.isEmpty()) {
            Snackbar.make(findViewById(R.id.main),
                    "⚠️ Escreva uma devolutiva antes de enviar.",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (devolutiva.length() < 10) {
            Snackbar.make(findViewById(R.id.main),
                    "⚠️ A devolutiva deve ter ao menos 10 caracteres.",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (denunciaId == null || denunciaId.isEmpty()) {
            Snackbar.make(findViewById(R.id.main),
                    "❌ ID da denúncia não encontrado.",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        buttonEnviarDevolutiva.setEnabled(false);
        buttonEnviarDevolutiva.setText("Enviando...");

        try {
            JSONObject body = new JSONObject();
            body.put("devolutiva", devolutiva);

            String path = ApiConfig.DENUNCIAS + ApiConfig.DENUNCIAS_DEVOLUTIVA + "/" + denunciaId;

            ApiClient.getInstance().patch(
                    path,
                    body,
                    response -> {
                        editDevolutiva.setEnabled(false);
                        buttonEnviarDevolutiva.setText("✅ Devolutiva Enviada");

                        Snackbar.make(findViewById(R.id.main),
                                "📨 Devolutiva enviada com sucesso!",
                                Snackbar.LENGTH_LONG).show();
                    },
                    (code, msg) -> {
                        buttonEnviarDevolutiva.setEnabled(true);
                        buttonEnviarDevolutiva.setText("📨 Enviar Devolutiva");

                        String errorMsg;
                        switch (code) {
                            case 400: errorMsg = "Devolutiva inválida. Mínimo 10 caracteres."; break;
                            case 404: errorMsg = "Denúncia não encontrada."; break;
                            case -1:  errorMsg = "Sem conexão com o servidor."; break;
                            default:  errorMsg = "Erro ao enviar (código " + code + ")."; break;
                        }

                        Snackbar.make(findViewById(R.id.main),
                                "❌ " + errorMsg,
                                Snackbar.LENGTH_LONG).show();
                    }
            );

        } catch (JSONException e) {
            buttonEnviarDevolutiva.setEnabled(true);
            buttonEnviarDevolutiva.setText("📨 Enviar Devolutiva");
            Snackbar.make(findViewById(R.id.main),
                    "❌ Erro interno ao montar requisição.",
                    Snackbar.LENGTH_SHORT).show();
        }
    }
}
