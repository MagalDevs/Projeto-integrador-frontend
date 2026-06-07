package com.example.projeto_integrador;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

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

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private MaterialButton buttonAddDenuncia;
    private MaterialButton buttonMinhasDenuncias;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);

        // obrigatório pro osmdroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return insets;
        });

        // MAPA
        map = findViewById(R.id.map);

        map.setMultiTouchControls(true);

        GeoPoint indaiatuba = new GeoPoint(-23.0882, -47.2234);

        map.getController().setZoom(15.0);
        map.getController().setCenter(indaiatuba);

        // BOTÃO NOVA DENÚNCIA
        buttonAddDenuncia = findViewById(R.id.button);

        buttonAddDenuncia.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    NovaDenunciaActivity.class
            );
            startActivity(intent);
        });

        // BOTÃO MINHAS DENÚNCIAS
        buttonMinhasDenuncias = findViewById(R.id.buttonMinhasDenuncias);

        buttonMinhasDenuncias.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    DenunciasActivity.class
            );
            startActivity(intent);
        });

        // Carregar denúncias do usuário no mapa
        carregarDenunciasNoMapa();
    }

    /**
     * Carrega as denúncias do usuário logado e plota marcadores no mapa.
     */
    private void carregarDenunciasNoMapa() {
        String userId = session.getUserId();
        if (userId.isEmpty()) return;

        ApiClient.getInstance().get(
                ApiConfig.DENUNCIAS_POR_USUARIO + "/" + userId,
                response -> {
                    try {
                        JSONArray array = new JSONArray(response);

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject denuncia = array.getJSONObject(i);

                            double lat = denuncia.getDouble("latitude");
                            double lng = denuncia.getDouble("longitude");
                            String titulo = denuncia.optString("titulo", "Denúncia");
                            String status = denuncia.optString("status", "");

                            GeoPoint ponto = new GeoPoint(lat, lng);

                            Marker marker = new Marker(map);
                            marker.setPosition(ponto);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle(titulo);
                            marker.setSnippet("Status: " + formatarStatus(status));

                            map.getOverlays().add(marker);
                        }

                        map.invalidate();

                    } catch (JSONException e) {
                        // Silencioso — mapa funciona sem marcadores
                    }
                },
                (code, msg) -> {
                    // Silencioso — mapa funciona sem marcadores
                }
        );
    }

    /**
     * Converte o enum do backend para texto legível.
     */
    private String formatarStatus(String status) {
        switch (status) {
            case "ABERTA":      return "Aberta";
            case "EM_ANALISE":  return "Em Análise";
            case "RESOLVIDA":   return "Resolvida";
            default:            return status;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();

        // Recarrega marcadores ao voltar (ex: após criar nova denúncia)
        map.getOverlays().clear();
        carregarDenunciasNoMapa();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}