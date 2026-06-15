package com.example.projeto_integrador;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

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

    private MaterialButton buttonLogout;

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

        // BOTÃO LOGOUT
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener(v -> realizarLogout());
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
                            marker.setIcon(criarIconePin(status)); // ← linha nova
                            map.getOverlays().add(marker);

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

    private void realizarLogout() {
        // Limpa sessão
        session.logout();

        // Vai para Login sem possibilidade de voltar
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Cria um marcador estilo Google Maps (gota) colorido por status.
     */
    private Drawable criarIconePin(String status) {
        int cor;
        switch (status) {
            case "EM_ANALISE": cor = 0xFFF5A623; break; // amarelo
            case "RESOLVIDA":  cor = 0xFF27AE60; break; // verde
            default:           cor = 0xFF2B7DE9; break; // azul (ABERTA)
        }

        int largura  = dpToPx(32);
        int altura   = dpToPx(42);

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                largura, altura, android.graphics.Bitmap.Config.ARGB_8888
        );
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        android.graphics.Paint paintFill  = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        android.graphics.Paint paintBorda = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        android.graphics.Paint paintPonta = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        android.graphics.Paint paintCirc  = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        paintFill.setColor(cor);
        paintFill.setStyle(android.graphics.Paint.Style.FILL);

        paintBorda.setColor(0xFFFFFFFF);
        paintBorda.setStyle(android.graphics.Paint.Style.STROKE);
        paintBorda.setStrokeWidth(dpToPx(2));

        paintPonta.setColor(cor);
        paintPonta.setStyle(android.graphics.Paint.Style.FILL);

        paintCirc.setColor(0xFFFFFFFF);
        paintCirc.setStyle(android.graphics.Paint.Style.FILL);

        float cx      = largura / 2f;
        float raio    = largura / 2f - dpToPx(2);
        float centroY = raio + dpToPx(2);

        // Sombra suave
        android.graphics.Paint paintSombra = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paintSombra.setColor(0x33000000);
        paintSombra.setStyle(android.graphics.Paint.Style.FILL);
        canvas.drawOval(
                cx - raio * 0.7f, altura - dpToPx(6),
                cx + raio * 0.7f, altura,
                paintSombra
        );

        // Triângulo da ponta (gota)
        android.graphics.Path ponta = new android.graphics.Path();
        ponta.moveTo(cx - dpToPx(6), centroY + raio - dpToPx(2));
        ponta.lineTo(cx + dpToPx(6), centroY + raio - dpToPx(2));
        ponta.lineTo(cx, altura - dpToPx(4));
        ponta.close();
        canvas.drawPath(ponta, paintPonta);

        // Círculo principal (corpo do pin)
        canvas.drawCircle(cx, centroY, raio, paintFill);
        canvas.drawCircle(cx, centroY, raio, paintBorda);

        // Círculo interno branco (reflexo)
        canvas.drawCircle(cx, centroY, raio * 0.38f, paintCirc);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}