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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
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
    private double latitude;
    private double longitude;
    private String statusAtual;

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
    }

    private void carregarDadosDaIntent() {

        // Recebe dados passados pelo AdminDenunciasActivity
        String tipo = getIntent().getStringExtra("tipo");
        String cidadao = getIntent().getStringExtra("cidadao");
        String data = getIntent().getStringExtra("data");
        String descricao = getIntent().getStringExtra("descricao");
        statusAtual = getIntent().getStringExtra("status");
        latitude = getIntent().getDoubleExtra("latitude", -23.0882);
        longitude = getIntent().getDoubleExtra("longitude", -47.2234);

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

                // Mostra/esconde card de devolutiva baseado no status
                if (novoStatus.equals("CONCLUÍDO")) {
                    cardDevolutiva.setVisibility(View.VISIBLE);
                } else {
                    cardDevolutiva.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nada a fazer
            }
        });

        // BOTÃO SALVAR STATUS
        buttonSalvarStatus.setOnClickListener(v -> {

            String novoStatus = spinnerStatus.getSelectedItem().toString();

            statusAtual = novoStatus;

            atualizarBadgeStatus(novoStatus);

            Snackbar.make(
                    findViewById(R.id.main),
                    "✅ Status atualizado para: " + novoStatus,
                    Snackbar.LENGTH_SHORT
            ).show();

            // TODO: Enviar atualização de status para o backend
        });

        // BOTÃO ENVIAR DEVOLUTIVA
        buttonEnviarDevolutiva.setOnClickListener(v -> {

            String devolutiva = editDevolutiva.getText().toString().trim();

            if (devolutiva.isEmpty()) {

                Snackbar.make(
                        findViewById(R.id.main),
                        "⚠️ Escreva uma devolutiva antes de enviar.",
                        Snackbar.LENGTH_SHORT
                ).show();

                return;
            }

            Snackbar.make(
                    findViewById(R.id.main),
                    "📨 Devolutiva enviada com sucesso!",
                    Snackbar.LENGTH_LONG
            ).show();

            // Desabilita o campo e botão após envio
            editDevolutiva.setEnabled(false);
            buttonEnviarDevolutiva.setEnabled(false);
            buttonEnviarDevolutiva.setText("✅ Devolutiva Enviada");

            // TODO: Enviar devolutiva para o backend e notificar o cidadão
        });

        // BOTÃO SALVAR TUDO (barra inferior)
        buttonSalvarAlteracoes.setOnClickListener(v -> {

            String novoStatus = spinnerStatus.getSelectedItem().toString();

            statusAtual = novoStatus;

            atualizarBadgeStatus(novoStatus);

            Snackbar.make(
                    findViewById(R.id.main),
                    "💾 Todas as alterações foram salvas!",
                    Snackbar.LENGTH_SHORT
            ).show();

            // TODO: Salvar todas as alterações no backend
        });
    }

    /**
     * Atualiza visualmente o badge de status com as cores correspondentes.
     */
    private void atualizarBadgeStatus(String status) {

        switch (status) {

            case "EM ANÁLISE":
                layoutStatusBadge.setBackgroundColor(0xFFFFF3D6);
                dotStatus.setBackgroundColor(0xFFF5A623);
                textStatusBadge.setText("EM ANÁLISE");
                textStatusBadge.setTextColor(0xFFF5A623);
                break;

            case "EM ANDAMENTO":
                layoutStatusBadge.setBackgroundColor(0xFFD6EAFF);
                dotStatus.setBackgroundColor(0xFF2B7DE9);
                textStatusBadge.setText("EM ANDAMENTO");
                textStatusBadge.setTextColor(0xFF2B7DE9);
                break;

            case "CONCLUÍDO":
                layoutStatusBadge.setBackgroundColor(0xFFD6F5E0);
                dotStatus.setBackgroundColor(0xFF27AE60);
                textStatusBadge.setText("CONCLUÍDO");
                textStatusBadge.setTextColor(0xFF27AE60);
                break;
        }
    }

    /**
     * Seleciona o status correto no Spinner baseado na string recebida.
     */
    private void selecionarStatusNoSpinner(String status) {

        String[] statusArray = getResources().getStringArray(R.array.status_denuncia);

        for (int i = 0; i < statusArray.length; i++) {

            if (statusArray[i].equals(status)) {
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
}
